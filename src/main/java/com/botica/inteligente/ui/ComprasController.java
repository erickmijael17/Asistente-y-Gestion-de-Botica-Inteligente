package com.botica.inteligente.ui;

import atlantafx.base.theme.Styles;
import com.botica.inteligente.categoria.service.CategoriaService;
import com.botica.inteligente.compra.enums.EstadoCompra;
import com.botica.inteligente.compra.enums.TipoDocumentoCompra;
import com.botica.inteligente.compra.service.CompraResumen;
import com.botica.inteligente.compra.service.CompraService;
import com.botica.inteligente.compra.service.LineaCompra;
import com.botica.inteligente.compra.service.NuevaCompra;
import com.botica.inteligente.laboratorio.service.LaboratorioService;
import com.botica.inteligente.producto.service.ProductoFiltro;
import com.botica.inteligente.producto.service.ProductoResumen;
import com.botica.inteligente.producto.service.ProductoService;
import com.botica.inteligente.security.SesionActual;
import com.botica.inteligente.shared.exception.CampoInvalidoException;
import com.botica.inteligente.shared.util.CalculadoraIgv;
import com.botica.inteligente.shared.util.Ean13;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Duration;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class ComprasController implements Refrescable {

    private static final int TAMANO_PAGINA = 15;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final CompraService compraService;
    private final ProductoService productoService;
    private final CategoriaService categoriaService;
    private final LaboratorioService laboratorioService;
    private final SesionActual sesionActual;
    private final Notificador notificador;
    private final BigDecimal tasaIgv;

    @FXML
    private VBox raiz;

    private final TextField buscador = new TextField();
    private final ComboBox<String> filtroEstado = new ComboBox<>();
    private final DatePicker desde = new DatePicker();
    private final DatePicker hasta = new DatePicker();
    private final TableView<CompraResumen> tabla = new TableView<>();
    private final Paginador paginador = new Paginador(this::cargar);
    private final Label mensaje = new Label();
    private final PauseTransition esperaBusqueda = new PauseTransition(Duration.millis(300));
    private int paginaActual;
    private long solicitud;

    public ComprasController(
        CompraService compraService,
        ProductoService productoService,
        CategoriaService categoriaService,
        LaboratorioService laboratorioService,
        SesionActual sesionActual,
        Notificador notificador,
        @Value("${botica.igv.tasa:0.18}") BigDecimal tasaIgv
    ) {
        this.compraService = compraService;
        this.productoService = productoService;
        this.categoriaService = categoriaService;
        this.laboratorioService = laboratorioService;
        this.sesionActual = sesionActual;
        this.notificador = notificador;
        this.tasaIgv = tasaIgv;
    }

    @FXML
    public void initialize() {
        Label encabezado = new Label("Compras");
        encabezado.getStyleClass().add("section-title");
        mensaje.getStyleClass().add("muted-text");

        buscador.setPromptText("Buscar por código, documento o proveedor…");
        buscador.setPrefWidth(300);
        buscador.textProperty().addListener((obs, anterior, nuevo) -> {
            esperaBusqueda.setOnFinished(evento -> cargar(0));
            esperaBusqueda.playFromStart();
        });
        filtroEstado.getItems().setAll("Todas", "Registradas", "Anuladas");
        filtroEstado.setValue("Todas");
        filtroEstado.valueProperty().addListener((obs, anterior, nuevo) -> cargar(0));
        desde.setPromptText("Desde");
        hasta.setPromptText("Hasta");
        desde.valueProperty().addListener((obs, anterior, nuevo) -> cargar(0));
        hasta.valueProperty().addListener((obs, anterior, nuevo) -> cargar(0));
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox barra = new HBox(10, buscador, filtroEstado, desde, hasta, espacio);
        barra.setAlignment(Pos.CENTER_LEFT);
        if (sesionActual.esOwner()) {
            Button nueva = new Button("Nueva compra");
            nueva.getStyleClass().add(Styles.ACCENT);
            nueva.setOnAction(evento -> abrirNuevaCompra());
            barra.getChildren().add(nueva);
        }

        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPlaceholder(new Label("Sin compras registradas"));
        agregar(tabla, "Código", 100, CompraResumen::codigo);
        agregar(tabla, "Fecha", 130, c -> FECHA_HORA.format(c.fecha()));
        agregar(tabla, "Proveedor", 220, c -> c.proveedor() + " (" + c.ruc() + ")");
        agregar(tabla, "Documento", 150, c -> c.tipoDocumento().name() + " " + c.numeroDocumento());
        agregar(tabla, "Total", 90, c -> "S/ " + c.total());
        agregar(tabla, "Estado", 90, c -> c.estado() == EstadoCompra.REGISTRADA ? "Registrada" : "Anulada");
        tabla.getColumns().add(columnaAcciones());
        VBox.setVgrow(tabla, Priority.ALWAYS);

        raiz.getChildren().setAll(encabezado, barra, mensaje, tabla, paginador);
    }

    @Override
    public void refrescar() {
        cargar(paginaActual);
    }

    private void cargar(int pagina) {
        paginaActual = Math.max(pagina, 0);
        long actual = ++solicitud;
        String texto = buscador.getText();
        EstadoCompra estado = switch (filtroEstado.getValue() == null ? "Todas" : filtroEstado.getValue()) {
            case "Registradas" -> EstadoCompra.REGISTRADA;
            case "Anuladas" -> EstadoCompra.ANULADA;
            default -> null;
        };
        LocalDate fechaDesde = desde.getValue();
        LocalDate fechaHasta = hasta.getValue();
        PageRequest pageable = PageRequest.of(paginaActual, TAMANO_PAGINA, Sort.by("fecha").descending());
        mensaje.setText("Cargando…");
        Fondo.ejecutar(
            () -> compraService.listar(texto, estado, fechaDesde, fechaHasta, pageable),
            resultado -> {
                if (actual != solicitud) {
                    return;
                }
                if (resultado.isEmpty() && resultado.getTotalPages() > 0 && paginaActual > 0) {
                    cargar(resultado.getTotalPages() - 1);
                    return;
                }
                tabla.getItems().setAll(resultado.getContent());
                paginador.actualizar(resultado);
                mensaje.setText("");
            },
            error -> {
                if (actual == solicitud) {
                    mensaje.setText(DialogoFormulario.textoError(error));
                }
            }
        );
    }

    private TableColumn<CompraResumen, CompraResumen> columnaAcciones() {
        boolean owner = sesionActual.esOwner();
        TableColumn<CompraResumen, CompraResumen> columna = new TableColumn<>("Acciones");
        columna.setPrefWidth(180);
        columna.setSortable(false);
        columna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        columna.setCellFactory(c -> new TableCell<>() {
            private final Button detalle = new Button("Detalle");
            private final Button anular = new Button("Anular");
            private final HBox caja = new HBox(8, detalle, anular);

            {
                detalle.setOnAction(evento -> verDetalle(getItem()));
                anular.setOnAction(evento -> confirmarAnulacion(getItem()));
            }

            @Override
            protected void updateItem(CompraResumen compra, boolean vacio) {
                super.updateItem(compra, vacio);
                if (vacio || compra == null) {
                    setGraphic(null);
                    return;
                }
                anular.setVisible(owner && compra.estado() == EstadoCompra.REGISTRADA);
                anular.setManaged(anular.isVisible());
                setGraphic(caja);
            }
        });
        return columna;
    }

    // ---------- detalle y anulacion ----------

    private void verDetalle(CompraResumen resumen) {
        if (resumen == null) {
            return;
        }
        Fondo.ejecutar(
            () -> compraService.obtener(resumen.id()),
            this::abrirDetalle,
            error -> notificador.mostrar(ventana(), DialogoFormulario.textoError(error))
        );
    }

    private void abrirDetalle(CompraResumen compra) {
        TableView<CompraResumen.Detalle> lineas = new TableView<>();
        lineas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        lineas.setPrefHeight(240);
        agregar(lineas, "Producto", 220, d -> d.codigoProducto() + " · " + d.nombreProducto());
        agregar(lineas, "Cant.", 60, d -> String.valueOf(d.cantidad()));
        agregar(lineas, "Costo", 80, d -> d.costoUnitario().stripTrailingZeros().toPlainString());
        agregar(lineas, "Importe", 90, d -> d.importe().toPlainString());
        agregar(lineas, "Lote", 120, CompraResumen.Detalle::codigoLote);
        agregar(lineas, "Vence", 100, d -> d.fechaVencimiento() == null ? "—" : FECHA.format(d.fechaVencimiento()));
        lineas.getItems().setAll(compra.detalles());

        Label cabecera = new Label(
            compra.proveedor() + " · RUC " + compra.ruc() + "\n"
                + compra.tipoDocumento() + " " + compra.numeroDocumento() + " (" + FECHA.format(compra.fechaDocumento()) + ")"
        );
        Label totales = new Label(
            "Base S/ " + compra.baseImponible() + " · IGV S/ " + compra.igv() + " · Total S/ " + compra.total()
        );
        totales.getStyleClass().add(Styles.TEXT_BOLD);
        VBox contenido = new VBox(10, cabecera, lineas, totales);
        contenido.setPrefWidth(720);
        Dialog<Void> dialogo = new Dialog<>();
        dialogo.initOwner(ventana());
        dialogo.setTitle("Compra " + compra.codigo());
        dialogo.setHeaderText("Compra " + compra.codigo() + (compra.estado() == EstadoCompra.ANULADA ? " (ANULADA)" : ""));
        dialogo.getDialogPane().setContent(contenido);
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialogo.show();
    }

    private void confirmarAnulacion(CompraResumen compra) {
        if (compra == null) {
            return;
        }
        Alert alerta = new Alert(
            Alert.AlertType.CONFIRMATION,
            "Se revertirá el stock de la compra " + compra.codigo() + ". ¿Desea anularla?",
            ButtonType.YES, ButtonType.NO
        );
        alerta.initOwner(ventana());
        alerta.setHeaderText("Anular compra");
        alerta.showAndWait().filter(boton -> boton == ButtonType.YES).ifPresent(boton -> Fondo.ejecutar(
            () -> compraService.anular(compra.id()),
            anulada -> {
                notificador.mostrar(ventana(), "Compra " + anulada.codigo() + " anulada.");
                cargar(paginaActual);
            },
            error -> notificador.mostrar(ventana(), DialogoFormulario.textoError(error))
        ));
    }

    // ---------- nueva compra ----------

    private final class FilaLinea extends HBox {
        final Autocompletar<ProductoResumen> producto = new Autocompletar<>(
            texto -> productoService.listar(
                new ProductoFiltro(texto, true, null, null, null),
                PageRequest.of(0, 10, Sort.by("nombreComercial"))
            ).getContent(),
            p -> p.datos().codigoInterno() + " · " + p.datos().nombreComercial()
        );
        final TextField cantidad = new TextField();
        final TextField costo = new TextField();
        final TextField lote = new TextField();
        final DatePicker vencimiento = new DatePicker();

        FilaLinea(Runnable alCambiar, Runnable alQuitar) {
            super(6);
            setAlignment(Pos.CENTER_LEFT);
            producto.setPromptText("Producto");
            producto.setPrefWidth(250);
            cantidad.setPromptText("Cant.");
            cantidad.setPrefWidth(60);
            costo.setPromptText("Costo");
            costo.setPrefWidth(80);
            lote.setPromptText("Lote (opc.)");
            lote.setPrefWidth(100);
            vencimiento.setPromptText("Vence");
            vencimiento.setPrefWidth(120);
            Button quitar = new Button("✕");
            quitar.setOnAction(evento -> alQuitar.run());
            cantidad.textProperty().addListener((obs, a, n) -> alCambiar.run());
            costo.textProperty().addListener((obs, a, n) -> alCambiar.run());
            getChildren().addAll(producto, cantidad, costo, lote, vencimiento, quitar);
        }
    }

    private void abrirNuevaCompra() {
        Formulario f = new Formulario(2);
        f.titulo("Proveedor y documento");
        TextField ruc = f.texto("ruc", "RUC del proveedor *", "11 dígitos", false);
        TextField razonSocial = f.texto("razonSocial", "Razón social *", "", false);
        ComboBox<TipoDocumentoCompra> tipo =
            f.combo("tipoDocumento", "Tipo de documento *", Arrays.asList(TipoDocumentoCompra.values()), false);
        tipo.setValue(TipoDocumentoCompra.FACTURA);
        TextField numero = f.texto("numeroDocumento", "Número de documento *", "F001-00001234", false);
        DatePicker fechaDocumento = new DatePicker(LocalDate.now());
        f.agregar("fechaDocumento", "Fecha del documento *", fechaDocumento, false);
        CheckBox aplicaIgv = f.check("aplicaIgv", "Aplica IGV");
        aplicaIgv.setSelected(true);
        CheckBox incluyeIgv = f.check("preciosIncluyenIgv", "Los costos ya incluyen IGV");
        TextArea observacion = f.area("observacion", "Observación", 2);

        f.titulo("Productos recibidos");
        VBox filas = new VBox(6);
        Label totales = new Label();
        totales.getStyleClass().add(Styles.TEXT_BOLD);
        List<FilaLinea> lineas = new ArrayList<>();
        Runnable recalcular = () -> totales.setText(textoTotales(lineas, aplicaIgv.isSelected(), incluyeIgv.isSelected()));
        Runnable[] agregarLinea = new Runnable[1];
        agregarLinea[0] = () -> {
            FilaLinea[] referencia = new FilaLinea[1];
            referencia[0] = new FilaLinea(recalcular, () -> {
                lineas.remove(referencia[0]);
                filas.getChildren().remove(referencia[0]);
                recalcular.run();
            });
            lineas.add(referencia[0]);
            filas.getChildren().add(referencia[0]);
        };
        agregarLinea[0].run();
        aplicaIgv.selectedProperty().addListener((obs, a, n) -> recalcular.run());
        incluyeIgv.selectedProperty().addListener((obs, a, n) -> recalcular.run());
        Button agregar = new Button("+ Agregar producto");
        agregar.setOnAction(evento -> agregarLinea[0].run());

        Consumer<ProductoResumen> agregarProducto = producto -> {
            FilaLinea existente = lineas.stream()
                .filter(l -> l.producto.getValue() != null && l.producto.getValue().id().equals(producto.id()))
                .findFirst().orElse(null);
            if (existente != null) {
                int actual;
                try {
                    Integer escrito = EntradaNumerica.entero("lineas", existente.cantidad.getText());
                    actual = escrito == null ? 0 : escrito;
                } catch (CampoInvalidoException cantidadInvalida) {
                    actual = 0;
                }
                existente.cantidad.setText(String.valueOf(actual + 1));
                return;
            }
            FilaLinea destino = lineas.stream()
                .filter(l -> l.producto.getValue() == null && l.cantidad.getText().isBlank() && l.costo.getText().isBlank())
                .findFirst().orElse(null);
            if (destino == null) {
                agregarLinea[0].run();
                destino = lineas.get(lineas.size() - 1);
            }
            destino.producto.seleccionar(producto);
            destino.cantidad.setText("1");
            destino.costo.setText(producto.datos().precioCompra().toPlainString());
            recalcular.run();
        };

        TextField escaner = new TextField();
        escaner.setPromptText("Escanee el código de barras para agregar un producto y presione Enter");
        escaner.setOnAction(evento -> {
            String codigo = escaner.getText() == null ? "" : escaner.getText().trim();
            escaner.clear();
            if (!codigo.isEmpty()) {
                procesarCodigo(codigo, agregarProducto);
            }
        });
        VBox bloque = new VBox(8, escaner, filas, new HBox(12, agregar, totales));
        bloque.setPadding(new Insets(0, 0, 4, 0));
        f.agregarNodo(bloque);
        f.agregar("lineas", "", new Label(), true);
        recalcular.run();

        DialogoFormulario.mostrar(ventana(), "Nueva compra", f, () -> {
            List<LineaCompra> lineasCompra = new ArrayList<>();
            for (int i = 0; i < lineas.size(); i++) {
                lineasCompra.add(leerLinea(i + 1, lineas.get(i)));
            }
            NuevaCompra compra = new NuevaCompra(
                ruc.getText(), razonSocial.getText(), tipo.getValue(), numero.getText(), fechaDocumento.getValue(),
                aplicaIgv.isSelected(), incluyeIgv.isSelected(), observacion.getText(), lineasCompra
            );
            return () -> "Compra " + compraService.registrar(compra).codigo() + " registrada.";
        }, aviso -> {
            notificador.mostrar(ventana(), aviso);
            cargar(0);
        });
    }

    /** Un codigo escaneado agrega el producto existente o abre el alta rapida si el codigo es nuevo. */
    private void procesarCodigo(String codigo, Consumer<ProductoResumen> agregarProducto) {
        Fondo.ejecutar(
            () -> productoService.buscarPorCodigoBarras(codigo),
            encontrado -> {
                if (encontrado.isPresent()) {
                    if (encontrado.get().estado()) {
                        agregarProducto.accept(encontrado.get());
                    } else {
                        notificador.mostrar(ventana(), "El producto " + encontrado.get().datos().nombreComercial() + " está inactivo.");
                    }
                } else if (!Ean13.esValido(codigo)) {
                    notificador.mostrar(ventana(), "Código no registrado y no es un EAN-13 válido: " + codigo);
                } else {
                    AltaRapidaProducto.abrir(
                        ventana(), codigo, productoService, categoriaService, laboratorioService, agregarProducto,
                        aviso -> notificador.mostrar(ventana(), aviso)
                    );
                }
            },
            error -> notificador.mostrar(ventana(), DialogoFormulario.textoError(error))
        );
    }

    private LineaCompra leerLinea(int item, FilaLinea fila) {
        ProductoResumen producto = fila.producto.getValue();
        if (producto == null) {
            throw new CampoInvalidoException("lineas", "Línea " + item + ": seleccione un producto de la lista.");
        }
        int cantidad;
        try {
            cantidad = Integer.parseInt(fila.cantidad.getText().trim());
        } catch (NumberFormatException error) {
            throw new CampoInvalidoException("lineas", "Línea " + item + ": ingrese una cantidad entera.");
        }
        BigDecimal costo = decimal(fila.costo.getText());
        if (costo == null) {
            throw new CampoInvalidoException("lineas", "Línea " + item + ": ingrese un costo válido.");
        }
        return new LineaCompra(producto.id(), cantidad, costo, fila.lote.getText(), fila.vencimiento.getValue());
    }

    private String textoTotales(List<FilaLinea> lineas, boolean aplicaIgv, boolean incluyeIgv) {
        List<BigDecimal> importes = new ArrayList<>();
        for (FilaLinea fila : lineas) {
            BigDecimal costo = decimal(fila.costo.getText());
            try {
                int cantidad = Integer.parseInt(fila.cantidad.getText().trim());
                if (costo != null && cantidad > 0) {
                    importes.add(CalculadoraIgv.importe(cantidad, costo));
                }
            } catch (NumberFormatException ignorada) {
                // linea incompleta: no suma al total mostrado
            }
        }
        CalculadoraIgv.Totales t = CalculadoraIgv.calcular(importes, aplicaIgv, incluyeIgv, tasaIgv);
        return "Base S/ " + t.baseImponible() + " · IGV S/ " + t.igv() + " · Total S/ " + t.total();
    }

    private static BigDecimal decimal(String texto) {
        String limpio = texto == null ? "" : texto.trim().replace(',', '.');
        if (limpio.isEmpty()) {
            return null;
        }
        try {
            BigDecimal valor = new BigDecimal(limpio);
            return valor.signum() > 0 ? valor : null;
        } catch (NumberFormatException error) {
            return null;
        }
    }

    // ---------- utilidades ----------

    private <T> void agregar(TableView<T> destino, String titulo, double ancho, Function<T, String> valor) {
        TableColumn<T, String> columna = new TableColumn<>(titulo);
        columna.setPrefWidth(ancho);
        columna.setCellValueFactory(celda -> new ReadOnlyStringWrapper(valor.apply(celda.getValue())));
        destino.getColumns().add(columna);
    }

    private Window ventana() {
        return raiz.getScene() == null ? null : raiz.getScene().getWindow();
    }
}
