package com.botica.inteligente.ui;

import com.botica.inteligente.inventario.service.InventarioFila;
import com.botica.inteligente.inventario.service.InventarioService;
import com.botica.inteligente.inventario.service.InventarioService.FiltroStock;
import com.botica.inteligente.inventario.service.KardexFila;
import com.botica.inteligente.inventario.service.LoteResumen;
import com.botica.inteligente.inventario.service.ResumenInventario;
import com.botica.inteligente.security.SesionActual;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.function.Function;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.Tab;
import javafx.scene.control.TabPane;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import javafx.util.Duration;
import org.springframework.context.annotation.Scope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class InventarioController implements Refrescable {

    private static final int TAMANO_PAGINA = 15;
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final InventarioService inventarioService;
    private final SesionActual sesionActual;
    private final Notificador notificador;

    @FXML
    private VBox raiz;

    private final Label resumenLabel = new Label();
    private final TextField buscador = new TextField();
    private final ComboBox<String> filtroStock = new ComboBox<>();
    private final TableView<InventarioFila> tabla = new TableView<>();
    private final Paginador paginador = new Paginador(this::cargarExistencias);
    private final ComboBox<String> filtroDias = new ComboBox<>();
    private final TableView<LoteResumen> tablaVencimientos = new TableView<>();
    private final Label mensaje = new Label();
    private final PauseTransition esperaBusqueda = new PauseTransition(Duration.millis(300));
    private int paginaActual;
    private long solicitud;

    public InventarioController(
        InventarioService inventarioService, SesionActual sesionActual, Notificador notificador
    ) {
        this.inventarioService = inventarioService;
        this.sesionActual = sesionActual;
        this.notificador = notificador;
    }

    @FXML
    public void initialize() {
        resumenLabel.getStyleClass().add("muted-text");
        mensaje.getStyleClass().add("muted-text");

        Tab existencias = new Tab("Existencias", construirExistencias());
        Tab vencimientos = new Tab("Vencimientos", construirVencimientos());
        existencias.setClosable(false);
        vencimientos.setClosable(false);
        TabPane pestanas = new TabPane(existencias, vencimientos);
        VBox.setVgrow(pestanas, Priority.ALWAYS);
        raiz.getChildren().setAll(resumenLabel, mensaje, pestanas);
    }

    @Override
    public void refrescar() {
        cargarResumen();
        cargarExistencias(paginaActual);
        cargarVencimientos();
    }

    // ---------- existencias ----------

    private VBox construirExistencias() {
        buscador.setPromptText("Buscar por nombre, código o código de barras…");
        buscador.setPrefWidth(320);
        buscador.textProperty().addListener((obs, anterior, nuevo) -> {
            esperaBusqueda.setOnFinished(evento -> cargarExistencias(0));
            esperaBusqueda.playFromStart();
        });
        filtroStock.getItems().setAll("Todos", "Disponible", "Stock bajo", "Sin stock");
        filtroStock.setValue("Todos");
        filtroStock.valueProperty().addListener((obs, anterior, nuevo) -> cargarExistencias(0));
        HBox barra = new HBox(10, buscador, filtroStock);
        barra.setAlignment(Pos.CENTER_LEFT);

        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPlaceholder(new Label("Sin resultados"));
        agregar(tabla, "Código", 90, InventarioFila::codigoInterno);
        agregar(tabla, "Producto", 240, InventarioFila::nombreComercial);
        agregar(tabla, "Stock", 70, f -> String.valueOf(f.stock()));
        agregar(tabla, "Mínimo", 70, f -> String.valueOf(f.stockMinimo()));
        agregar(tabla, "Estado", 100, f -> switch (f.estadoStock()) {
            case DISPONIBLE -> "Disponible";
            case BAJO -> "Stock bajo";
            case SIN_STOCK -> "Sin stock";
        });
        agregar(tabla, "Próx. vencimiento", 130, f -> f.proximoVencimiento() == null ? "" : FECHA.format(f.proximoVencimiento()));
        agregar(tabla, "Costo prom.", 95, f -> f.costoPromedio() == null ? "" : "S/ " + f.costoPromedio().setScale(2, java.math.RoundingMode.HALF_UP));
        TableColumn<InventarioFila, InventarioFila> acciones = new TableColumn<>("Detalle");
        acciones.setPrefWidth(130);
        acciones.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        acciones.setCellFactory(c -> new TableCell<>() {
            private final Button boton = new Button("Lotes y kardex");

            {
                boton.setOnAction(evento -> mostrarDetalle(getItem()));
            }

            @Override
            protected void updateItem(InventarioFila fila, boolean vacio) {
                super.updateItem(fila, vacio);
                setGraphic(vacio || fila == null ? null : boton);
            }
        });
        tabla.getColumns().add(acciones);
        Tablas.aplicarIndicadores(tabla, "Estado");
        VBox.setVgrow(tabla, Priority.ALWAYS);
        VBox caja = new VBox(12, barra, tabla, paginador);
        caja.setPadding(new Insets(12, 0, 0, 0));
        return caja;
    }

    private void cargarResumen() {
        Fondo.ejecutar(inventarioService::resumen, this::mostrarResumen, error -> mensaje.setText(texto(error)));
    }

    private void mostrarResumen(ResumenInventario r) {
        resumenLabel.setText(
            r.productos() + " productos · " + r.unidades() + " unidades · valor a costo S/ " + r.valorCosto()
                + " · valor a venta S/ " + r.valorVenta() + " · " + r.conStockBajo() + " con stock bajo · "
                + r.sinStock() + " sin stock"
        );
    }

    private void cargarExistencias(int pagina) {
        paginaActual = Math.max(pagina, 0);
        long actual = ++solicitud;
        String texto = buscador.getText();
        FiltroStock filtro = switch (filtroStock.getValue() == null ? "Todos" : filtroStock.getValue()) {
            case "Disponible" -> FiltroStock.DISPONIBLE;
            case "Stock bajo" -> FiltroStock.BAJO;
            case "Sin stock" -> FiltroStock.SIN_STOCK;
            default -> FiltroStock.TODOS;
        };
        PageRequest pageable = PageRequest.of(paginaActual, TAMANO_PAGINA);
        Fondo.ejecutar(
            () -> inventarioService.listar(texto, filtro, pageable),
            resultado -> {
                if (actual != solicitud) {
                    return;
                }
                if (resultado.isEmpty() && resultado.getTotalPages() > 0 && paginaActual > 0) {
                    cargarExistencias(resultado.getTotalPages() - 1);
                    return;
                }
                tabla.getItems().setAll(resultado.getContent());
                paginador.actualizar(resultado);
                mensaje.setText("");
            },
            error -> mensaje.setText(texto(error))
        );
    }

    private void mostrarDetalle(InventarioFila fila) {
        if (fila == null) {
            return;
        }
        Fondo.ejecutar(
            () -> new Detalle(
                inventarioService.lotesDeProducto(fila.productoId()),
                inventarioService.kardex(fila.productoId(), 0, 100)
            ),
            detalle -> abrirDetalle(fila, detalle),
            error -> notificador.mostrar(ventana(), texto(error))
        );
    }

    private record Detalle(List<LoteResumen> lotes, Page<KardexFila> kardex) {
    }

    private void abrirDetalle(InventarioFila fila, Detalle detalle) {
        TableView<LoteResumen> lotes = new TableView<>();
        lotes.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        lotes.setPrefHeight(160);
        agregar(lotes, "Lote", 140, LoteResumen::codigoLote);
        agregar(lotes, "Vencimiento", 110, l -> l.fechaVencimiento() == null ? "—" : FECHA.format(l.fechaVencimiento()));
        agregar(lotes, "Cantidad", 80, l -> String.valueOf(l.cantidadActual()));
        agregar(lotes, "Estado", 100, l -> l.vencido() ? "Vencido" : "Vigente");
        Tablas.aplicarIndicadores(lotes, "Estado");
        lotes.getItems().setAll(detalle.lotes());

        TableView<KardexFila> kardex = new TableView<>();
        kardex.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        kardex.setPrefHeight(260);
        agregar(kardex, "Fecha", 130, k -> FECHA_HORA.format(k.fecha()));
        agregar(kardex, "Movimiento", 150, k -> k.tipo().etiqueta());
        agregar(kardex, "Lote", 110, k -> k.lote() == null ? "" : k.lote());
        agregar(kardex, "Entrada", 70, k -> k.entrada() == 0 ? "" : String.valueOf(k.entrada()));
        agregar(kardex, "Salida", 70, k -> k.salida() == 0 ? "" : String.valueOf(k.salida()));
        agregar(kardex, "Saldo", 70, k -> String.valueOf(k.saldo()));
        agregar(kardex, "Documento", 110, k -> k.documento() == null ? "" : k.documento());
        kardex.getItems().setAll(detalle.kardex().getContent());

        VBox contenido = new VBox(10, new Label("Lotes"), lotes, new Label("Kardex (más reciente primero)"), kardex);
        contenido.setPrefWidth(760);
        Dialog<Void> dialogo = new Dialog<>();
        dialogo.initOwner(ventana());
        dialogo.setTitle("Detalle de inventario");
        dialogo.setHeaderText(fila.nombreComercial() + " — stock " + fila.stock());
        dialogo.getDialogPane().setContent(contenido);
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialogo.show();
    }

    // ---------- vencimientos ----------

    private VBox construirVencimientos() {
        filtroDias.getItems().setAll("Vencidos y próximos 30 días", "Vencidos y próximos 60 días", "Vencidos y próximos 90 días");
        filtroDias.setValue(filtroDias.getItems().get(0));
        filtroDias.valueProperty().addListener((obs, anterior, nuevo) -> cargarVencimientos());
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox barra = new HBox(10, filtroDias, espacio);
        barra.setAlignment(Pos.CENTER_LEFT);

        tablaVencimientos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaVencimientos.setPlaceholder(new Label("Sin lotes por vencer"));
        agregar(tablaVencimientos, "Producto", 240, LoteResumen::producto);
        agregar(tablaVencimientos, "Lote", 130, LoteResumen::codigoLote);
        agregar(tablaVencimientos, "Vencimiento", 110, l -> FECHA.format(l.fechaVencimiento()));
        agregar(tablaVencimientos, "Días", 70, l -> String.valueOf(l.diasParaVencer()));
        agregar(tablaVencimientos, "Cantidad", 80, l -> String.valueOf(l.cantidadActual()));
        agregar(tablaVencimientos, "Estado", 100, l -> l.vencido() ? "Vencido" : "Por vencer");
        if (sesionActual.esOwner()) {
            TableColumn<LoteResumen, LoteResumen> baja = new TableColumn<>("Acción");
            baja.setPrefWidth(120);
            baja.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
            baja.setCellFactory(c -> new TableCell<>() {
                private final Button boton = new Button("Dar de baja");

                {
                    boton.setOnAction(evento -> darDeBaja(getItem()));
                }

                @Override
                protected void updateItem(LoteResumen lote, boolean vacio) {
                    super.updateItem(lote, vacio);
                    setGraphic(vacio || lote == null ? null : boton);
                }
            });
            tablaVencimientos.getColumns().add(baja);
        }
        Tablas.aplicarIndicadores(tablaVencimientos, "Estado");
        VBox.setVgrow(tablaVencimientos, Priority.ALWAYS);
        VBox caja = new VBox(12, barra, tablaVencimientos);
        caja.setPadding(new Insets(12, 0, 0, 0));
        return caja;
    }

    private void cargarVencimientos() {
        String seleccion = filtroDias.getValue() == null ? "" : filtroDias.getValue();
        int dias = seleccion.contains("90") ? 90 : seleccion.contains("60") ? 60 : 30;
        Fondo.ejecutar(
            () -> inventarioService.alertasVencimiento(dias),
            lotes -> tablaVencimientos.getItems().setAll(lotes),
            error -> mensaje.setText(texto(error))
        );
    }

    private void darDeBaja(LoteResumen lote) {
        if (lote == null) {
            return;
        }
        Formulario formulario = new Formulario(1);
        TextField cantidad = formulario.texto("cantidad", "Cantidad a dar de baja *", "", false);
        cantidad.setText(String.valueOf(lote.cantidadActual()));
        TextField motivo = formulario.texto("motivo", "Motivo *", lote.vencido() ? "Vencido" : "Merma", false);
        motivo.setText(lote.vencido() ? "Vencido" : "");
        DialogoFormulario.mostrar(
            ventana(), "Dar de baja lote " + lote.codigoLote(), formulario,
            () -> {
                int unidades = entero(cantidad.getText());
                String razon = motivo.getText();
                return () -> {
                    inventarioService.registrarMerma(lote.id(), unidades, razon);
                    return "Baja registrada.";
                };
            },
            aviso -> {
                notificador.mostrar(ventana(), aviso);
                refrescar();
            }
        );
    }

    // ---------- utilidades ----------

    private static int entero(String texto) {
        try {
            return Integer.parseInt(texto == null ? "" : texto.trim());
        } catch (NumberFormatException error) {
            throw new com.botica.inteligente.shared.exception.CampoInvalidoException(
                "cantidad", "Ingrese un número entero válido."
            );
        }
    }

    private <T> void agregar(TableView<T> tablaDestino, String titulo, double ancho, Function<T, String> valor) {
        TableColumn<T, String> columna = new TableColumn<>(titulo);
        columna.setPrefWidth(ancho);
        columna.setCellValueFactory(celda -> new ReadOnlyStringWrapper(valor.apply(celda.getValue())));
        tablaDestino.getColumns().add(columna);
    }

    private Window ventana() {
        return raiz.getScene() == null ? null : raiz.getScene().getWindow();
    }

    private String texto(Throwable error) {
        return DialogoFormulario.textoError(error);
    }
}
