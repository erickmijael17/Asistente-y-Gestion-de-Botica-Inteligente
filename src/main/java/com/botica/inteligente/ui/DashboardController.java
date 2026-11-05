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
        Label encabezado = new Label("Inicio");
        encabezado.getStyleClass().add("section-title");
        mensaje.getStyleClass().add("muted-text");
        raiz.getChildren().add(encabezado);
        if (sesionActual.esOwner()) {
            raiz.getChildren().addAll(construirIndicadores());
        }
        raiz.getChildren().addAll(mensaje, construirAlertas());
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
        HBox barra = new HBox(10, new Label("Período:"), periodo, espacio);
        barra.setAlignment(Pos.CENTER_LEFT);

        variacion.getStyleClass().add("muted-text");
        HBox tarjetas = new HBox(
            14,
            tarjeta("Ventas del período", ventas, variacion),
            tarjeta("N.º de ventas", cantidad, null),
            tarjeta("Ticket promedio", ticket, null),
            tarjeta("Utilidad bruta", utilidad, null)
        );

        grafico.setLegendVisible(false);
        grafico.setAnimated(false);
        grafico.setCategoryGap(6);
        grafico.setPrefHeight(240);
        grafico.setMinHeight(200);
        ejeY.setLabel("S/");
        agregar(masVendidos, "Producto", 280, ProductoVendido::nombre);
        agregar(masVendidos, "Unidades", 90, p -> String.valueOf(p.unidades()));
        agregar(masVendidos, "Importe", 110, p -> "S/ " + p.importe().setScale(2, RoundingMode.HALF_UP));
        masVendidos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        masVendidos.setPlaceholder(new Label("Sin ventas en el período"));
        masVendidos.setPrefHeight(240);
        masVendidos.setMinHeight(160);
        VBox listado = new VBox(6, new Label("Productos más vendidos"), masVendidos);
        listado.setPrefWidth(520);
        HBox medio = new HBox(14, new VBox(6, new Label("Ventas por período"), grafico), listado);
        HBox.setHgrow(medio.getChildren().get(0), Priority.ALWAYS);
        VBox.setVgrow(grafico, Priority.ALWAYS);

        return new VBox(14, barra, tarjetas, medio);
    }

    private VBox tarjeta(String titulo, Label valor, Label detalle) {
        Label etiqueta = new Label(titulo);
        etiqueta.getStyleClass().add("muted-text");
        valor.getStyleClass().add(Styles.TITLE_2);
        VBox tarjeta = new VBox(4, etiqueta, valor);
        if (detalle != null) {
            tarjeta.getChildren().add(detalle);
        }
        tarjeta.getStyleClass().add("kpi-card");
        tarjeta.setPadding(new Insets(14, 18, 14, 18));
        HBox.setHgrow(tarjeta, Priority.ALWAYS);
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
            variacion.setText("Sin datos del período anterior");
        } else {
            String flecha = r.variacionPorcentual().signum() >= 0 ? "▲ " : "▼ ";
            variacion.setText(flecha + r.variacionPorcentual().abs() + " % vs período anterior");
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
        tablaStock.setPlaceholder(new Label("Sin alertas de stock"));
        agregar(tablaStock, "Producto", 240, InventarioFila::nombreComercial);
        agregar(tablaStock, "Stock", 60, f -> String.valueOf(f.stock()));
        agregar(tablaStock, "Mínimo", 70, f -> String.valueOf(f.stockMinimo()));
        agregar(tablaStock, "Estado", 90, f -> f.estadoStock() == InventarioFila.EstadoStock.SIN_STOCK ? "Sin stock" : "Stock bajo");

        tablaVencimientos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaVencimientos.setPlaceholder(new Label("Sin lotes por vencer"));
        agregar(tablaVencimientos, "Producto", 220, LoteResumen::producto);
        agregar(tablaVencimientos, "Lote", 100, LoteResumen::codigoLote);
        agregar(tablaVencimientos, "Vence", 90, l -> FECHA.format(l.fechaVencimiento()));
        agregar(tablaVencimientos, "Cant.", 60, l -> String.valueOf(l.cantidadActual()));
        agregar(tablaVencimientos, "Estado", 80, l -> l.vencido() ? "Vencido" : "Por vencer");
        tablaStock.setPrefHeight(200);
        tablaVencimientos.setPrefHeight(200);

        VBox stock = new VBox(6, alertaStock, tablaStock);
        VBox vencimientos = new VBox(6, alertaVencimiento, tablaVencimientos);
        HBox.setHgrow(stock, Priority.ALWAYS);
        HBox.setHgrow(vencimientos, Priority.ALWAYS);
        VBox seccion = new VBox(8, new Label("Alertas de inventario"), new HBox(14, stock, vencimientos));
        VBox.setVgrow(seccion, Priority.SOMETIMES);
        return seccion;
    }

    private void mostrarAlertas(Alertas alertas) {
        alertaStock.setText(alertas.productosConStockBajo() + " con stock bajo · " + alertas.productosSinStock() + " sin stock");
        alertaVencimiento.setText(
            alertas.lotesPorVencer() + " lotes por vencer en 30 días · " + alertas.lotesVencidos() + " vencidos con stock"
        );
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
