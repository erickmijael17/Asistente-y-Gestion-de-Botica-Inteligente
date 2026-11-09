package com.botica.inteligente.ui;

import com.botica.inteligente.inventario.service.InventarioFila;
import com.botica.inteligente.inventario.service.InventarioService;
import com.botica.inteligente.inventario.service.InventarioService.FiltroStock;
import com.botica.inteligente.inventario.service.KardexFila;
import com.botica.inteligente.inventario.service.LoteResumen;
import com.botica.inteligente.inventario.service.ResumenInventario;
import com.botica.inteligente.security.SesionActual;
import com.botica.inteligente.shared.i18n.Mensajes;
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

        Tab existencias = new Tab(Mensajes.get("inventario.tab.existencias"), construirExistencias());
        Tab vencimientos = new Tab(Mensajes.get("inventario.tab.vencimientos"), construirVencimientos());
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
        buscador.setPromptText(Mensajes.get("inventario.buscar.prompt"));
        buscador.setPrefWidth(320);
        buscador.textProperty().addListener((obs, anterior, nuevo) -> {
            esperaBusqueda.setOnFinished(evento -> cargarExistencias(0));
            esperaBusqueda.playFromStart();
        });
        filtroStock.getItems().setAll(
            Mensajes.get("catalogo.filtro.todos"),
            Mensajes.get("inventario.filtro.disponible"),
            Mensajes.get("inventario.filtro.stockBajo"),
            Mensajes.get("inventario.filtro.sinStock")
        );
        filtroStock.setValue(Mensajes.get("catalogo.filtro.todos"));
        filtroStock.valueProperty().addListener((obs, anterior, nuevo) -> cargarExistencias(0));
        HBox barra = new HBox(10, buscador, filtroStock);
        barra.setAlignment(Pos.CENTER_LEFT);

        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPlaceholder(new Label(Mensajes.get("catalogo.sinResultados")));
        agregar(tabla, Mensajes.get("producto.columna.codigo"), 90, InventarioFila::codigoInterno);
        agregar(tabla, Mensajes.get("producto.columna.producto"), 240, InventarioFila::nombreComercial);
        agregar(tabla, Mensajes.get("inventario.columna.stock"), 70, f -> String.valueOf(f.stock()));
        agregar(tabla, Mensajes.get("inventario.columna.minimo"), 70, f -> String.valueOf(f.stockMinimo()));
        agregar(tabla, Mensajes.get("catalogo.columna.estado"), 100, f -> switch (f.estadoStock()) {
            case DISPONIBLE -> Mensajes.get("inventario.filtro.disponible");
            case BAJO -> Mensajes.get("inventario.filtro.stockBajo");
            case SIN_STOCK -> Mensajes.get("inventario.filtro.sinStock");
        });
        agregar(tabla, Mensajes.get("inventario.columna.proxVencimiento"), 130,
            f -> f.proximoVencimiento() == null ? "" : FECHA.format(f.proximoVencimiento()));
        agregar(tabla, Mensajes.get("inventario.columna.costoProm"), 95,
            f -> f.costoPromedio() == null ? "" : "S/ " + f.costoPromedio().setScale(2, java.math.RoundingMode.HALF_UP));
        TableColumn<InventarioFila, InventarioFila> acciones = new TableColumn<>(Mensajes.get("inventario.columna.detalle"));
        acciones.setPrefWidth(130);
        acciones.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
        acciones.setCellFactory(c -> new TableCell<>() {
            private final Button boton = new Button(Mensajes.get("inventario.boton.lotesKardex"));

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
        Tablas.aplicarIndicadores(tabla, Mensajes.get("catalogo.columna.estado"));
        VBox.setVgrow(tabla, Priority.ALWAYS);
        VBox caja = new VBox(12, barra, tabla, paginador);
        caja.setPadding(new Insets(12, 0, 0, 0));
        return caja;
    }

    private void cargarResumen() {
        Fondo.ejecutar(inventarioService::resumen, this::mostrarResumen, error -> mensaje.setText(texto(error)));
    }

    private void mostrarResumen(ResumenInventario r) {
        resumenLabel.setText(Mensajes.get(
            "inventario.resumen",
            String.valueOf(r.productos()), String.valueOf(r.unidades()), r.valorCosto().toString(),
            r.valorVenta().toString(), String.valueOf(r.conStockBajo()), String.valueOf(r.sinStock())
        ));
    }

    private void cargarExistencias(int pagina) {
        paginaActual = Math.max(pagina, 0);
        long actual = ++solicitud;
        String texto = buscador.getText();
        String valorFiltro = filtroStock.getValue() == null ? Mensajes.get("catalogo.filtro.todos") : filtroStock.getValue();
        FiltroStock filtro;
        if (valorFiltro.equals(Mensajes.get("inventario.filtro.disponible"))) {
            filtro = FiltroStock.DISPONIBLE;
        } else if (valorFiltro.equals(Mensajes.get("inventario.filtro.stockBajo"))) {
            filtro = FiltroStock.BAJO;
        } else if (valorFiltro.equals(Mensajes.get("inventario.filtro.sinStock"))) {
            filtro = FiltroStock.SIN_STOCK;
        } else {
            filtro = FiltroStock.TODOS;
        }
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
        agregar(lotes, Mensajes.get("inventario.columna.lote"), 140, LoteResumen::codigoLote);
        agregar(lotes, Mensajes.get("inventario.columna.vencimiento"), 110,
            l -> l.fechaVencimiento() == null ? "—" : FECHA.format(l.fechaVencimiento()));
        agregar(lotes, Mensajes.get("inventario.columna.cantidad"), 80, l -> String.valueOf(l.cantidadActual()));
        agregar(lotes, Mensajes.get("catalogo.columna.estado"), 100,
            l -> l.vencido() ? Mensajes.get("inventario.estado.vencido") : Mensajes.get("inventario.estado.vigente"));
        Tablas.aplicarIndicadores(lotes, Mensajes.get("catalogo.columna.estado"));
        lotes.getItems().setAll(detalle.lotes());

        TableView<KardexFila> kardex = new TableView<>();
        kardex.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        kardex.setPrefHeight(260);
        agregar(kardex, Mensajes.get("inventario.columna.fecha"), 130, k -> FECHA_HORA.format(k.fecha()));
        agregar(kardex, Mensajes.get("inventario.columna.movimiento"), 150, k -> k.tipo().etiqueta());
        agregar(kardex, Mensajes.get("inventario.columna.lote"), 110, k -> k.lote() == null ? "" : k.lote());
        agregar(kardex, Mensajes.get("inventario.columna.entrada"), 70, k -> k.entrada() == 0 ? "" : String.valueOf(k.entrada()));
        agregar(kardex, Mensajes.get("inventario.columna.salida"), 70, k -> k.salida() == 0 ? "" : String.valueOf(k.salida()));
        agregar(kardex, Mensajes.get("inventario.columna.saldo"), 70, k -> String.valueOf(k.saldo()));
        agregar(kardex, Mensajes.get("inventario.columna.documento"), 110, k -> k.documento() == null ? "" : k.documento());
        kardex.getItems().setAll(detalle.kardex().getContent());

        VBox contenido = new VBox(
            10, new Label(Mensajes.get("inventario.seccion.lotes")), lotes,
            new Label(Mensajes.get("inventario.kardexReciente")), kardex
        );
        contenido.setPrefWidth(760);
        Dialog<Void> dialogo = new Dialog<>();
        dialogo.initOwner(ventana());
        dialogo.setTitle(Mensajes.get("inventario.dialogo.detalle"));
        dialogo.setHeaderText(fila.nombreComercial() + " — " + Mensajes.get("inventario.columna.stock") + " " + fila.stock());
        dialogo.getDialogPane().setContent(contenido);
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialogo.show();
    }

    // ---------- vencimientos ----------

    private VBox construirVencimientos() {
        filtroDias.getItems().setAll(
            Mensajes.get("inventario.dias.30"), Mensajes.get("inventario.dias.60"), Mensajes.get("inventario.dias.90")
        );
        filtroDias.setValue(filtroDias.getItems().get(0));
        filtroDias.valueProperty().addListener((obs, anterior, nuevo) -> cargarVencimientos());
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        HBox barra = new HBox(10, filtroDias, espacio);
        barra.setAlignment(Pos.CENTER_LEFT);

        tablaVencimientos.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tablaVencimientos.setPlaceholder(new Label(Mensajes.get("inventario.sinLotesPorVencer")));
        agregar(tablaVencimientos, Mensajes.get("producto.columna.producto"), 240, LoteResumen::producto);
        agregar(tablaVencimientos, Mensajes.get("inventario.columna.lote"), 130, LoteResumen::codigoLote);
        agregar(tablaVencimientos, Mensajes.get("inventario.columna.vencimiento"), 110, l -> FECHA.format(l.fechaVencimiento()));
        agregar(tablaVencimientos, Mensajes.get("inventario.columna.dias"), 70, l -> String.valueOf(l.diasParaVencer()));
        agregar(tablaVencimientos, Mensajes.get("inventario.columna.cantidad"), 80, l -> String.valueOf(l.cantidadActual()));
        agregar(tablaVencimientos, Mensajes.get("catalogo.columna.estado"), 100,
            l -> l.vencido() ? Mensajes.get("inventario.estado.vencido") : Mensajes.get("inventario.estado.porVencer"));
        if (sesionActual.esOwner()) {
            TableColumn<LoteResumen, LoteResumen> baja = new TableColumn<>(Mensajes.get("usuarios.columna.accion"));
            baja.setPrefWidth(120);
            baja.setCellValueFactory(c -> new ReadOnlyObjectWrapper<>(c.getValue()));
            baja.setCellFactory(c -> new TableCell<>() {
                private final Button boton = new Button(Mensajes.get("inventario.boton.darBaja"));

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
        Tablas.aplicarIndicadores(tablaVencimientos, Mensajes.get("catalogo.columna.estado"));
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
        TextField cantidad = formulario.texto("cantidad", Mensajes.get("inventario.form.cantidadBajaLabel"), "", false);
        cantidad.setText(String.valueOf(lote.cantidadActual()));
        TextField motivo = formulario.texto(
            "motivo", Mensajes.get("inventario.form.motivoLabel"),
            lote.vencido() ? Mensajes.get("inventario.estado.vencido") : Mensajes.get("inventario.motivo.merma"), false
        );
        motivo.setText(lote.vencido() ? Mensajes.get("inventario.estado.vencido") : "");
        DialogoFormulario.mostrar(
            ventana(), Mensajes.get("inventario.dialogo.darBaja", lote.codigoLote()), formulario,
            () -> {
                int unidades = entero(cantidad.getText());
                String razon = motivo.getText();
                return () -> {
                    inventarioService.registrarMerma(lote.id(), unidades, razon);
                    return Mensajes.get("inventario.bajaRegistrada");
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
                "cantidad", Mensajes.get("inventario.error.numeroEntero")
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
