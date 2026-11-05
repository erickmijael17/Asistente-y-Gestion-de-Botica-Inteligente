package com.botica.inteligente.ui;

import atlantafx.base.theme.Styles;
import com.botica.inteligente.security.SesionActual;
import com.botica.inteligente.venta.enums.EstadoVenta;
import com.botica.inteligente.venta.service.ComprobantePdfService;
import com.botica.inteligente.venta.service.VentaResumen;
import com.botica.inteligente.venta.service.VentaService;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.function.Function;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.ComboBox;
import javafx.scene.control.DatePicker;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class VentasController implements Refrescable {

    private static final int TAMANO_PAGINA = 15;
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final VentaService ventaService;
    private final ComprobantePdfService pdfService;
    private final SesionActual sesionActual;
    private final Notificador notificador;

    @FXML
    private VBox raiz;

    private final TextField buscador = new TextField();
    private final ComboBox<String> filtroEstado = new ComboBox<>();
    private final DatePicker desde = new DatePicker();
    private final DatePicker hasta = new DatePicker();
    private final TableView<VentaResumen> tabla = new TableView<>();
    private final Paginador paginador = new Paginador(this::cargar);
    private final Label hoy = new Label();
    private final Label mensaje = new Label();
    private final PauseTransition esperaBusqueda = new PauseTransition(Duration.millis(300));
    private int paginaActual;
    private long solicitud;

    public VentasController(
        VentaService ventaService, ComprobantePdfService pdfService, SesionActual sesionActual, Notificador notificador
    ) {
        this.ventaService = ventaService;
        this.pdfService = pdfService;
        this.sesionActual = sesionActual;
        this.notificador = notificador;
    }

    @FXML
    public void initialize() {
        hoy.getStyleClass().add("muted-text");
        mensaje.getStyleClass().add("muted-text");

        buscador.setPromptText("Buscar por comprobante, cliente o documento…");
        buscador.setPrefWidth(300);
        buscador.textProperty().addListener((obs, anterior, nuevo) -> {
            esperaBusqueda.setOnFinished(evento -> cargar(0));
            esperaBusqueda.playFromStart();
        });
        filtroEstado.getItems().setAll("Todas", "Emitidas", "Anuladas");
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

        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPlaceholder(new Label("Sin ventas registradas"));
        agregar(tabla, "Comprobante", 130, VentaResumen::comprobante);
        agregar(tabla, "Fecha", 130, v -> FECHA_HORA.format(v.fecha()));
        agregar(tabla, "Cliente", 180, v -> v.clienteNombre() == null ? "—" : v.clienteNombre());
        agregar(tabla, "Vendedor", 110, VentaResumen::vendedor);
        agregar(tabla, "Total", 90, v -> "S/ " + v.total());
        agregar(tabla, "Estado", 90, v -> v.estado() == EstadoVenta.EMITIDA ? "Emitida" : "Anulada");
        tabla.getColumns().add(columnaAcciones());
        Tablas.aplicarIndicadores(tabla, "Estado");
        VBox.setVgrow(tabla, Priority.ALWAYS);

        raiz.getChildren().setAll(hoy, barra, mensaje, tabla, paginador);
    }

    @Override
    public void refrescar() {
        cargar(paginaActual);
        Fondo.ejecutar(
            () -> ventaService.totalEmitido(LocalDate.now(), LocalDate.now()),
            total -> hoy.setText("Vendido hoy: S/ " + total.setScale(2, java.math.RoundingMode.HALF_UP)),
            error -> hoy.setText("")
        );
    }

    private void cargar(int pagina) {
        paginaActual = Math.max(pagina, 0);
        long actual = ++solicitud;
        String texto = buscador.getText();
        EstadoVenta estado = switch (filtroEstado.getValue() == null ? "Todas" : filtroEstado.getValue()) {
            case "Emitidas" -> EstadoVenta.EMITIDA;
            case "Anuladas" -> EstadoVenta.ANULADA;
            default -> null;
        };
        LocalDate fechaDesde = desde.getValue();
        LocalDate fechaHasta = hasta.getValue();
        PageRequest pageable = PageRequest.of(paginaActual, TAMANO_PAGINA, Sort.by("fecha").descending());
        mensaje.setText("Cargando…");
        Fondo.ejecutar(
            () -> ventaService.listar(texto, estado, fechaDesde, fechaHasta, pageable),
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

    private TableColumn<VentaResumen, VentaResumen> columnaAcciones() {
        boolean owner = sesionActual.esOwner();
        TableColumn<VentaResumen, VentaResumen> columna = new TableColumn<>("Acciones");
        columna.setPrefWidth(250);
        columna.setSortable(false);
        columna.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        columna.setCellFactory(c -> new TableCell<>() {
            private final Button detalle = new Button("Detalle");
            private final Button pdf = new Button("PDF");
            private final Button anular = new Button("Anular");
            private final HBox caja = new HBox(8, detalle, pdf, anular);

            {
                detalle.setOnAction(evento -> verDetalle(getItem()));
                pdf.setOnAction(evento -> imprimir(getItem()));
                anular.setOnAction(evento -> confirmarAnulacion(getItem()));
            }

            @Override
            protected void updateItem(VentaResumen venta, boolean vacio) {
                super.updateItem(venta, vacio);
                if (vacio || venta == null) {
                    setGraphic(null);
                    return;
                }
                anular.setVisible(owner && venta.estado() == EstadoVenta.EMITIDA);
                anular.setManaged(anular.isVisible());
                setGraphic(caja);
            }
        });
        return columna;
    }

    private void imprimir(VentaResumen venta) {
        if (venta != null) {
            VisorPdf.abrir(venta.comprobante(), () -> pdfService.generar(venta.id()), this::avisar);
        }
    }

    private void verDetalle(VentaResumen resumen) {
        if (resumen == null) {
            return;
        }
        Fondo.ejecutar(() -> ventaService.obtener(resumen.id()), this::abrirDetalle, error -> avisar(DialogoFormulario.textoError(error)));
    }

    private void abrirDetalle(VentaResumen venta) {
        TableView<VentaResumen.Detalle> lineas = new TableView<>();
        lineas.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        lineas.setPrefHeight(240);
        agregar(lineas, "Producto", 260, d -> d.codigoProducto() + " · " + d.nombreProducto());
        agregar(lineas, "Cant.", 60, d -> String.valueOf(d.cantidad()));
        agregar(lineas, "Precio", 80, d -> d.precioUnitario().toPlainString());
        agregar(lineas, "Importe", 90, d -> d.importe().toPlainString());
        lineas.getItems().setAll(venta.detalles());

        Label cabecera = new Label(
            venta.tipoComprobante().etiqueta() + " " + venta.comprobante() + " · " + FECHA_HORA.format(venta.fecha())
                + "\nCliente: " + (venta.clienteNombre() == null ? "—" : venta.clienteNombre())
                + " · Vendedor: " + venta.vendedor()
        );
        Label totales = new Label(
            "Base S/ " + venta.baseImponible() + " · IGV S/ " + venta.igv() + " · Total S/ " + venta.total()
        );
        totales.getStyleClass().add(Styles.TEXT_BOLD);
        VBox contenido = new VBox(10, cabecera, lineas, totales);
        contenido.setPrefWidth(620);
        Dialog<Void> dialogo = new Dialog<>();
        dialogo.initOwner(ventana());
        dialogo.setTitle("Venta " + venta.comprobante());
        dialogo.setHeaderText("Venta " + venta.comprobante() + (venta.estado() == EstadoVenta.ANULADA ? " (ANULADA)" : ""));
        dialogo.getDialogPane().setContent(contenido);
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialogo.show();
    }

    private void confirmarAnulacion(VentaResumen venta) {
        if (venta == null) {
            return;
        }
        Alert alerta = new Alert(
            Alert.AlertType.CONFIRMATION,
            "Se devolverá el stock de la venta " + venta.comprobante() + ". ¿Desea anularla?",
            ButtonType.YES, ButtonType.NO
        );
        alerta.initOwner(ventana());
        alerta.setHeaderText("Anular venta");
        alerta.showAndWait().filter(boton -> boton == ButtonType.YES).ifPresent(boton -> Fondo.ejecutar(
            () -> ventaService.anular(venta.id()),
            anulada -> {
                avisar("Venta " + anulada.comprobante() + " anulada.");
                refrescar();
            },
            error -> avisar(DialogoFormulario.textoError(error))
        ));
    }

    private <T> void agregar(TableView<T> destino, String titulo, double ancho, Function<T, String> valor) {
        TableColumn<T, String> columna = new TableColumn<>(titulo);
        columna.setPrefWidth(ancho);
        columna.setCellValueFactory(celda -> new ReadOnlyStringWrapper(valor.apply(celda.getValue())));
        destino.getColumns().add(columna);
    }

    private void avisar(String texto) {
        notificador.mostrar(ventana(), texto);
    }

    private Window ventana() {
        return raiz.getScene() == null ? null : raiz.getScene().getWindow();
    }
}
