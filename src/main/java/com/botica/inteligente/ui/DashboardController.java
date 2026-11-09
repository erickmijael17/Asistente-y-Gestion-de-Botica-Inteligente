package com.botica.inteligente.ui;

import atlantafx.base.theme.Styles;
import com.botica.inteligente.dashboard.service.Alertas;
import com.botica.inteligente.dashboard.service.DashboardService;
import com.botica.inteligente.dashboard.service.Periodo;
import com.botica.inteligente.dashboard.service.ProductoVendido;
import com.botica.inteligente.dashboard.service.ResumenVentas;
import com.botica.inteligente.inventario.service.InventarioFila;
import com.botica.inteligente.inventario.service.LoteResumen;
import com.botica.inteligente.security.SesionActual;
import com.botica.inteligente.shared.i18n.Mensajes;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.function.Function;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.util.StringConverter;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Pantalla de inicio: indicadores de ventas (solo propietario) y alertas de inventario. */
@Component
@Scope("prototype")
public class DashboardController implements Refrescable {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    private final DashboardService dashboardService;
    private final SesionActual sesionActual;

    @FXML
    private VBox raiz;

    private final ComboBox<Periodo> periodo = new ComboBox<>();
    private final Label ventas = new Label("—");
    private final Label variacion = new Label();
    private final Label cantidad = new Label("—");
    private final Label ticket = new Label("—");
    private final Label utilidad = new Label("—");
    private final CategoryAxis ejeX = new CategoryAxis();
    private final NumberAxis ejeY = new NumberAxis();
    private final BarChart<String, Number> grafico = new BarChart<>(ejeX, ejeY);
    private final TableView<ProductoVendido> masVendidos = new TableView<>();
    private final Label alertaStock = new Label("—");
    private final Label alertaVencimiento = new Label("—");
    private final TableView<InventarioFila> tablaStock = new TableView<>();
    private final TableView<LoteResumen> tablaVencimientos = new TableView<>();
    private final Label mensaje = new Label();

    public DashboardController(DashboardService dashboardService, SesionActual sesionActual) {
        this.dashboardService = dashboardService;
        this.sesionActual = sesionActual;
    }

    @FXML
    public void initialize() {
        mensaje.getStyleClass().add("muted-text");
        VBox contenido = new VBox(16);
        if (sesionActual.esOwner()) {
            contenido.getChildren().add(construirIndicadores());
        }
        contenido.getChildren().addAll(mensaje, construirAlertas());
        ScrollPane marco = new ScrollPane(contenido);
        marco.setFitToWidth(true);
        marco.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        marco.getStyleClass().add("marco-transparente");
        VBox.setVgrow(marco, Priority.ALWAYS);
        raiz.getChildren().setAll(marco);
    }

    private Label subtitulo(String texto) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("subtitulo");
        return etiqueta;
    }

    @Override
    public void refrescar() {
        if (sesionActual.esOwner()) {
            cargarVentas();
        }
        Fondo.ejecutar(dashboardService::alertas, this::mostrarAlertas, error -> mensaje.setText(DialogoFormulario.textoError(error)));
    }

    // ---------- indicadores de ventas ----------

    private VBox construirIndicadores() {
        periodo.getItems().setAll(Arrays.asList(Periodo.values()));
        periodo.setValue(Periodo.ULTIMOS_7);
        periodo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Periodo p) {
                return p == null ? "" : p.etiqueta();
            }

            @Override
            public Periodo fromString(String texto) {
                return null;
            }
        });
        periodo.valueProperty().addListener((obs, anterior, nuevo) -> cargarVentas());
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox barra = new HBox(10, new Label(Mensajes.get("dashboard.periodo")), periodo, espacio);
        barra.setAlignment(Pos.CENTER_LEFT);

        variacion.getStyleClass().add("muted-text");
        HBox tarjetas = new HBox(
            14,
            tarjeta(Mensajes.get("dashboard.kpi.ventasPeriodo"), ventas, variacion, "kpi-verde"),
            tarjeta(Mensajes.get("dashboard.kpi.numVentas"), cantidad, null, "kpi-azul"),
            tarjeta(Mensajes.get("dashboard.kpi.ticketPromedio"), ticket, null, "kpi-violeta"),
            tarjeta(Mensajes.get("dashboard.kpi.utilidadBruta"), utilidad, null, "kpi-ambar")
        );

        grafico.setLegendVisible(false);
        grafico.setAnimated(true);
        grafico.setCategoryGap(6);
        grafico.setPrefHeight(240);
        grafico.setMinHeight(200);
        ejeY.setLabel("S/");
        agregar(masVendidos, Mensajes.get("producto.columna.producto"), 280, ProductoVendido::nombre);
        agregar(masVendidos, Mensajes.get("dashboard.columna.unidades"), 90, p -> String.valueOf(p.unidades()));
        agregar(masVendidos, Mensajes.get("compra.columna.importe"), 110, p -> "S/ " + p.importe().setScale(2, RoundingMode.HALF_UP));
        masVendidos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        masVendidos.setPlaceholder(new Label(Mensajes.get("dashboard.sinVentasPeriodo")));
        masVendidos.setPrefHeight(240);
        masVendidos.setMinHeight(160);
        ejeX.setTickLabelRotation(-30);
        VBox listado = new VBox(6, subtitulo(Mensajes.get("dashboard.productosMasVendidos")), masVendidos);
        listado.setPrefWidth(520);
        HBox medio = new HBox(14, new VBox(6, subtitulo(Mensajes.get("dashboard.ventasPorPeriodo")), grafico), listado);
        HBox.setHgrow(medio.getChildren().get(0), Priority.ALWAYS);
        VBox.setVgrow(grafico, Priority.ALWAYS);

        Animaciones.escalonada(tarjetas.getChildren());
        return new VBox(14, barra, tarjetas, medio);
    }

    private VBox tarjeta(String titulo, Label valor, Label detalle, String color) {
        Label etiqueta = new Label(titulo);
        etiqueta.getStyleClass().add("muted-text");
        valor.getStyleClass().add(Styles.TITLE_2);
        VBox tarjeta = new VBox(4, etiqueta, valor);
        if (detalle != null) {
            tarjeta.getChildren().add(detalle);
        }
        tarjeta.getStyleClass().addAll("kpi-card", color);
        tarjeta.setPadding(new Insets(14, 18, 14, 18));
        Animaciones.elevarAlPasar(tarjeta);
        HBox.setHgrow(tarjeta, Priority.ALWAYS);
        tarjeta.setPrefWidth(0);
        tarjeta.setMaxWidth(Double.MAX_VALUE);
        return tarjeta;
    }

    private void cargarVentas() {
        Periodo elegido = periodo.getValue() == null ? Periodo.ULTIMOS_7 : periodo.getValue();
        Fondo.ejecutar(
            () -> dashboardService.resumenVentas(elegido),
            this::mostrarVentas,
            error -> mensaje.setText(DialogoFormulario.textoError(error))
        );
    }

    private void mostrarVentas(ResumenVentas r) {
        mensaje.setText("");
        ventas.setText(dinero(r.total()));
        cantidad.setText(String.valueOf(r.cantidadVentas()));
        ticket.setText(dinero(r.ticketPromedio()));
        utilidad.setText(dinero(r.utilidadBruta()));
        if (r.variacionPorcentual() == null) {
            variacion.setText(Mensajes.get("dashboard.sinPeriodoPrevio"));
        } else {
            String flecha = r.variacionPorcentual().signum() >= 0 ? "▲ " : "▼ ";
            variacion.setText(flecha + Mensajes.get("dashboard.variacion", r.variacionPorcentual().abs().toString()));
        }
        XYChart.Series<String, Number> serie = new XYChart.Series<>();
        r.serie().forEach(punto -> serie.getData().add(new XYChart.Data<>(punto.etiqueta(), punto.total())));
        grafico.getData().setAll(java.util.List.of(serie));
        masVendidos.getItems().setAll(r.masVendidos());
    }

    // ---------- alertas ----------

    private VBox construirAlertas() {
        alertaStock.getStyleClass().add(Styles.TEXT_BOLD);
        alertaVencimiento.getStyleClass().add(Styles.TEXT_BOLD);

        tablaStock.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaStock.setPlaceholder(new Label(Mensajes.get("dashboard.sinAlertasStock")));
        agregar(tablaStock, Mensajes.get("producto.columna.producto"), 240, InventarioFila::nombreComercial);
        agregar(tablaStock, Mensajes.get("inventario.columna.stock"), 60, f -> String.valueOf(f.stock()));
        agregar(tablaStock, Mensajes.get("inventario.columna.minimo"), 70, f -> String.valueOf(f.stockMinimo()));
        agregar(tablaStock, Mensajes.get("catalogo.columna.estado"), 90, f -> f.estadoStock() == InventarioFila.EstadoStock.SIN_STOCK
            ? Mensajes.get("inventario.filtro.sinStock")
            : Mensajes.get("inventario.filtro.stockBajo"));

        tablaVencimientos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaVencimientos.setPlaceholder(new Label(Mensajes.get("inventario.sinLotesPorVencer")));
        agregar(tablaVencimientos, Mensajes.get("producto.columna.producto"), 220, LoteResumen::producto);
        agregar(tablaVencimientos, Mensajes.get("inventario.columna.lote"), 100, LoteResumen::codigoLote);
        agregar(tablaVencimientos, Mensajes.get("compra.columna.vence"), 90, l -> FECHA.format(l.fechaVencimiento()));
        agregar(tablaVencimientos, Mensajes.get("compra.columna.cant"), 60, l -> String.valueOf(l.cantidadActual()));
        agregar(tablaVencimientos, Mensajes.get("catalogo.columna.estado"), 80,
            l -> l.vencido() ? Mensajes.get("inventario.estado.vencido") : Mensajes.get("inventario.estado.porVencer"));
        Tablas.aplicarIndicadores(tablaStock, Mensajes.get("catalogo.columna.estado"));
        Tablas.aplicarIndicadores(tablaVencimientos, Mensajes.get("catalogo.columna.estado"));
        tablaStock.setPrefHeight(200);
        tablaVencimientos.setPrefHeight(200);

        VBox stock = new VBox(6, alertaStock, tablaStock);
        VBox vencimientos = new VBox(6, alertaVencimiento, tablaVencimientos);
        HBox.setHgrow(stock, Priority.ALWAYS);
        HBox.setHgrow(vencimientos, Priority.ALWAYS);
        VBox seccion = new VBox(8, subtitulo(Mensajes.get("dashboard.alertasInventario")), new HBox(14, stock, vencimientos));
        VBox.setVgrow(seccion, Priority.SOMETIMES);
        return seccion;
    }

    private void mostrarAlertas(Alertas alertas) {
        alertaStock.setText(Mensajes.get(
            "dashboard.alertaStock",
            String.valueOf(alertas.productosConStockBajo()), String.valueOf(alertas.productosSinStock())
        ));
        alertaVencimiento.setText(Mensajes.get(
            "dashboard.alertaVencimiento",
            String.valueOf(alertas.lotesPorVencer()), String.valueOf(alertas.lotesVencidos())
        ));
        tablaStock.getItems().setAll(alertas.stock());
        tablaVencimientos.getItems().setAll(alertas.vencimientos());
    }

    // ---------- utilidades ----------

    private <T> void agregar(TableView<T> tabla, String titulo, double ancho, Function<T, String> valor) {
        TableColumn<T, String> columna = new TableColumn<>(titulo);
        columna.setPrefWidth(ancho);
        columna.setCellValueFactory(celda -> new ReadOnlyStringWrapper(valor.apply(celda.getValue())));
        tabla.getColumns().add(columna);
    }

    private static String dinero(BigDecimal valor) {
        return "S/ " + valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }
}
