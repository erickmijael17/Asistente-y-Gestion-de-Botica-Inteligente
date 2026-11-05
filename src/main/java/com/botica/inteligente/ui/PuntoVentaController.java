package com.botica.inteligente.ui;

import atlantafx.base.theme.Styles;
import com.botica.inteligente.inventario.service.InventarioService;
import com.botica.inteligente.producto.service.ProductoFiltro;
import com.botica.inteligente.producto.service.ProductoResumen;
import com.botica.inteligente.producto.service.ProductoService;
import com.botica.inteligente.shared.util.CalculadoraIgv;
import com.botica.inteligente.venta.enums.TipoComprobante;
import com.botica.inteligente.venta.service.ComprobantePdfService;
import com.botica.inteligente.venta.service.LineaVenta;
import com.botica.inteligente.venta.service.NuevaVenta;
import com.botica.inteligente.venta.service.VentaResumen;
import com.botica.inteligente.venta.service.VentaService;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import javafx.beans.property.IntegerProperty;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.beans.property.SimpleIntegerProperty;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.TextFieldTableCell;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.converter.IntegerStringConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

/** Punto de venta: busqueda o escaneo de productos, carrito, totales con IGV y cobro. */
@Component
@Scope("prototype")
public class PuntoVentaController implements Refrescable {

    private final VentaService ventaService;
    private final ProductoService productoService;
    private final InventarioService inventarioService;
    private final ComprobantePdfService pdfService;
    private final Notificador notificador;
    private final BigDecimal tasaIgv;

    @FXML
    private VBox raiz;

    private final ObservableList<Linea> carrito = FXCollections.observableArrayList();
    private final TableView<Linea> tabla = new TableView<>(carrito);
    private final TextField escaner = new TextField();
    private final Autocompletar<ProductoResumen> buscador;
    private final ComboBox<TipoComprobante> tipo = new ComboBox<>();
    private final TextField clienteNombre = new TextField();
    private final TextField clienteDocumento = new TextField();
    private final CheckBox aplicaIgv = new CheckBox("Aplica IGV");
    private final CheckBox incluyeIgv = new CheckBox("Los precios incluyen IGV");
    private final TextField observacion = new TextField();
    private final Label base = new Label();
    private final Label igv = new Label();
    private final Label total = new Label();
    private final Label error = new Label();
    private final Button cobrar = new Button("Cobrar");

    /** Linea del carrito: producto, cantidad editable y stock vendible al momento de agregarlo. */
    private static final class Linea {
        private final ProductoResumen producto;
        private final IntegerProperty cantidad = new SimpleIntegerProperty(1);
        private final long stock;

        Linea(ProductoResumen producto, long stock) {
            this.producto = producto;
            this.stock = stock;
        }

        BigDecimal precio() {
            return producto.datos().precioVenta();
        }

        BigDecimal importe() {
            return CalculadoraIgv.importe(cantidad.get(), precio());
        }
    }

    public PuntoVentaController(
        VentaService ventaService,
        ProductoService productoService,
        InventarioService inventarioService,
        ComprobantePdfService pdfService,
        Notificador notificador,
        @Value("${botica.igv.tasa:0.18}") BigDecimal tasaIgv
    ) {
        this.ventaService = ventaService;
        this.productoService = productoService;
        this.inventarioService = inventarioService;
        this.pdfService = pdfService;
        this.notificador = notificador;
        this.tasaIgv = tasaIgv;
        this.buscador = new Autocompletar<>(
            texto -> productoService.listar(
                new ProductoFiltro(texto, true, null, null, null),
                PageRequest.of(0, 10, Sort.by("nombreComercial"))
            ).getContent(),
            p -> p.datos().codigoInterno() + " · " + p.datos().nombreComercial()
        );
    }

    @FXML
    public void initialize() {
        escaner.setPromptText("Escanee el código de barras y presione Enter");
        escaner.setOnAction(evento -> escanear());
        buscador.setPromptText("…o busque un producto por nombre o código");
        buscador.setPrefWidth(380);
        buscador.valueProperty().addListener((obs, anterior, producto) -> {
            if (producto != null) {
                agregarProducto(producto);
                buscador.limpiar();
                escaner.requestFocus();
            }
        });
        HBox entrada = new HBox(10, escaner, buscador);
        HBox.setHgrow(escaner, Priority.ALWAYS);
        entrada.setAlignment(Pos.CENTER_LEFT);

        configurarTabla();
        VBox izquierda = new VBox(12, entrada, tabla);
        VBox.setVgrow(tabla, Priority.ALWAYS);
        HBox.setHgrow(izquierda, Priority.ALWAYS);

        raiz.getChildren().setAll(new HBox(18, izquierda, construirPanelCobro()));
        VBox.setVgrow(raiz.getChildren().get(0), Priority.ALWAYS);
        carrito.addListener((javafx.collections.ListChangeListener<Linea>) cambio -> recalcular());
        aplicaIgv.selectedProperty().addListener((obs, a, n) -> recalcular());
        incluyeIgv.selectedProperty().addListener((obs, a, n) -> recalcular());
        recalcular();
    }

    @Override
    public void refrescar() {
        escaner.requestFocus();
    }

    // ---------- carrito ----------

    private void configurarTabla() {
        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPlaceholder(new Label("El carrito está vacío"));
        tabla.setEditable(true);
        texto("Producto", 260, l -> l.producto.datos().codigoInterno() + " · " + l.producto.datos().nombreComercial());
        texto("Stock", 60, l -> String.valueOf(l.stock));
        texto("Precio", 80, l -> dinero(l.precio()));

        TableColumn<Linea, Integer> cantidad = new TableColumn<>("Cant.");
        cantidad.setPrefWidth(70);
        cantidad.setEditable(true);
        cantidad.setCellValueFactory(c -> c.getValue().cantidad.asObject());
        cantidad.setCellFactory(c -> new TextFieldTableCell<>(new IntegerStringConverter() {
            @Override
            public Integer fromString(String valor) {
                try {
                    return super.fromString(valor);
                } catch (NumberFormatException error) {
                    return null;
                }
            }
        }));
        cantidad.setOnEditCommit(evento -> {
            Linea linea = evento.getRowValue();
            Integer nueva = evento.getNewValue();
            if (nueva == null || nueva < 1) {
                avisar("La cantidad debe ser al menos 1.");
            } else if (nueva > linea.stock) {
                avisar("Solo hay " + linea.stock + " unidades disponibles.");
            } else {
                linea.cantidad.set(nueva);
            }
            tabla.refresh();
            recalcular();
        });
        tabla.getColumns().add(cantidad);
        texto("Importe", 90, l -> dinero(l.importe()));

        TableColumn<Linea, Linea> quitar = new TableColumn<>("");
        quitar.setPrefWidth(50);
        quitar.setSortable(false);
        quitar.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        quitar.setCellFactory(c -> new TableCell<>() {
            private final Button boton = new Button("✕");

            {
                boton.setOnAction(evento -> carrito.remove(getItem()));
            }

            @Override
            protected void updateItem(Linea linea, boolean vacio) {
                super.updateItem(linea, vacio);
                setGraphic(vacio || linea == null ? null : boton);
            }
        });
        tabla.getColumns().add(quitar);
    }

    private void texto(String titulo, double ancho, Function<Linea, String> valor) {
        TableColumn<Linea, String> columna = new TableColumn<>(titulo);
        columna.setPrefWidth(ancho);
        columna.setCellValueFactory(c -> (ObservableValue<String>) new ReadOnlyStringWrapper(valor.apply(c.getValue())));
        tabla.getColumns().add(columna);
    }

    private void escanear() {
        String codigo = escaner.getText() == null ? "" : escaner.getText().trim();
        escaner.clear();
        if (codigo.isEmpty()) {
            return;
        }
        Fondo.ejecutar(() -> {
            var porBarras = productoService.buscarPorCodigoBarras(codigo);
            if (porBarras.isPresent()) {
                return porBarras;
            }
            var coincidencias = productoService.listar(
                new ProductoFiltro(codigo, true, null, null, null), PageRequest.of(0, 2)
            ).getContent();
            return coincidencias.size() == 1 ? java.util.Optional.of(coincidencias.get(0)) : java.util.Optional.<ProductoResumen>empty();
        }, resultado -> {
            if (resultado.isEmpty()) {
                avisar("Producto no encontrado: " + codigo);
            } else if (!resultado.get().estado()) {
                avisar("El producto " + resultado.get().datos().nombreComercial() + " está inactivo.");
            } else {
                agregarProducto(resultado.get());
            }
            escaner.requestFocus();
        }, falla -> avisar(DialogoFormulario.textoError(falla)));
    }

    private void agregarProducto(ProductoResumen producto) {
        Linea existente = carrito.stream().filter(l -> l.producto.id().equals(producto.id())).findFirst().orElse(null);
        if (existente != null) {
            if (existente.cantidad.get() + 1 > existente.stock) {
                avisar("Solo hay " + existente.stock + " unidades disponibles de " + producto.datos().nombreComercial() + ".");
            } else {
                existente.cantidad.set(existente.cantidad.get() + 1);
                tabla.refresh();
                recalcular();
            }
            return;
        }
        Fondo.ejecutar(
            () -> inventarioService.stockDisponible(producto.id()),
            stock -> {
                if (stock < 1) {
                    avisar("Sin stock disponible de " + producto.datos().nombreComercial() + ".");
                } else {
                    carrito.add(new Linea(producto, stock));
                }
            },
            falla -> avisar(DialogoFormulario.textoError(falla))
        );
    }

    // ---------- panel de cobro ----------

    private VBox construirPanelCobro() {
        tipo.getItems().setAll(Arrays.asList(TipoComprobante.values()));
        tipo.setValue(TipoComprobante.BOLETA);
        tipo.setConverter(new javafx.util.StringConverter<>() {
            @Override
            public String toString(TipoComprobante t) {
                return t == null ? "" : t.etiqueta();
            }

            @Override
            public TipoComprobante fromString(String texto) {
                return null;
            }
        });
        tipo.setMaxWidth(Double.MAX_VALUE);
        clienteNombre.setPromptText("Cliente (opcional)");
        clienteDocumento.setPromptText("DNI o RUC (opcional)");
        observacion.setPromptText("Observación (opcional)");
        aplicaIgv.setSelected(true);
        incluyeIgv.setSelected(true);
        for (Label etiqueta : List.of(base, igv)) {
            etiqueta.getStyleClass().add("muted-text");
        }
        total.getStyleClass().addAll(Styles.TITLE_2);
        error.getStyleClass().add(Styles.DANGER);
        error.setWrapText(true);
        cobrar.getStyleClass().add(Styles.ACCENT);
        cobrar.setMaxWidth(Double.MAX_VALUE);
        cobrar.setOnAction(evento -> cobrar());

        VBox panel = new VBox(
            10, new Label("Comprobante"), tipo, clienteNombre, clienteDocumento, aplicaIgv, incluyeIgv,
            observacion, new Label("Resumen"), base, igv, total, error, cobrar
        );
        panel.setPrefWidth(300);
        panel.setMinWidth(300);
        panel.setPadding(new Insets(0, 0, 0, 4));
        return panel;
    }

    private void recalcular() {
        CalculadoraIgv.Totales t = CalculadoraIgv.calcular(
            carrito.stream().map(Linea::importe).toList(), aplicaIgv.isSelected(), incluyeIgv.isSelected(), tasaIgv
        );
        base.setText("Base imponible: " + dinero(t.baseImponible()));
        igv.setText("IGV: " + dinero(t.igv()));
        total.setText("Total: " + dinero(t.total()));
        cobrar.setDisable(carrito.isEmpty());
    }

    private void cobrar() {
        error.setText("");
        NuevaVenta venta = new NuevaVenta(
            tipo.getValue(), clienteNombre.getText(), clienteDocumento.getText(), aplicaIgv.isSelected(),
            incluyeIgv.isSelected(), observacion.getText(),
            carrito.stream().map(l -> new LineaVenta(l.producto.id(), l.cantidad.get(), null)).toList()
        );
        cobrar.setDisable(true);
        Fondo.ejecutar(() -> ventaService.registrar(venta), this::ventaRegistrada, falla -> {
            recalcular();
            error.setText(DialogoFormulario.textoError(falla));
        });
    }

    private void ventaRegistrada(VentaResumen venta) {
        carrito.clear();
        clienteNombre.clear();
        clienteDocumento.clear();
        observacion.clear();
        Alert dialogo = new Alert(Alert.AlertType.INFORMATION);
        dialogo.initOwner(ventana());
        dialogo.setTitle("Venta registrada");
        dialogo.setHeaderText("Venta " + venta.comprobante() + " registrada");
        dialogo.setContentText("Total cobrado: " + dinero(venta.total()));
        ButtonType imprimir = new ButtonType("Imprimir PDF", ButtonBar.ButtonData.LEFT);
        dialogo.getButtonTypes().setAll(imprimir, new ButtonType("Nueva venta", ButtonBar.ButtonData.OK_DONE));
        dialogo.showAndWait().filter(boton -> boton == imprimir).ifPresent(boton ->
            VisorPdf.abrir(venta.comprobante(), () -> pdfService.generar(venta.id()), this::avisar)
        );
        escaner.requestFocus();
    }

    // ---------- utilidades ----------

    private void avisar(String mensaje) {
        notificador.mostrar(ventana(), mensaje);
    }

    private Window ventana() {
        return raiz.getScene() == null ? null : raiz.getScene().getWindow();
    }

    private static String dinero(BigDecimal valor) {
        return "S/ " + valor.setScale(2, java.math.RoundingMode.HALF_UP).toPlainString();
    }
}
