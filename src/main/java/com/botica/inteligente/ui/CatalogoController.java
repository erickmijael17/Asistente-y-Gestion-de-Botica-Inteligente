package com.botica.inteligente.ui;

import atlantafx.base.theme.Styles;
import com.botica.inteligente.security.SesionActual;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.function.Function;
import java.util.function.Supplier;
import javafx.animation.PauseTransition;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Base de las pantallas de catalogo: buscador, filtros, tabla paginada, acciones por fila y
 * formulario en dialogo. Todas las consultas y guardados corren fuera del hilo de JavaFX.
 *
 * @param <R> tipo de fila de la tabla
 * @param <F> instantanea de filtros, capturada en el hilo de JavaFX y usada en la consulta
 */
public abstract class CatalogoController<R, F> implements Refrescable {

    private static final int TAMANO_PAGINA = 15;

    protected final SesionActual sesionActual;
    protected final Notificador notificador;

    @FXML
    protected VBox raiz;

    protected final TableView<R> tabla = new TableView<>();
    protected final TextField buscador = new TextField();
    protected final ComboBox<String> filtroEstado = new ComboBox<>();
    protected final HBox barraFiltros = new HBox(10);
    private final Label mensaje = new Label();
    private final Paginador paginador = new Paginador(this::cargar);
    private final PauseTransition esperaBusqueda = new PauseTransition(Duration.millis(300));
    private int paginaActual;
    private long solicitud;

    protected CatalogoController(SesionActual sesionActual, Notificador notificador) {
        this.sesionActual = sesionActual;
        this.notificador = notificador;
    }

    protected abstract String titulo();

    protected abstract String textoBotonNuevo();

    protected abstract void configurarColumnas();

    protected abstract Sort orden();

    /** Se ejecuta en el hilo de JavaFX. */
    protected abstract F capturarFiltros(String texto, Boolean estado);

    /** Se ejecuta en un hilo de fondo. */
    protected abstract Page<R> consultar(F filtros, Pageable pageable);

    protected abstract boolean estaActivo(R fila);

    /** Se ejecuta en un hilo de fondo. */
    protected abstract void cambiarEstado(R fila, boolean activo);

    /** Abre el formulario de alta (fila nula) o de edicion. */
    protected abstract void abrirFormulario(R fila);

    /** Controles de filtro adicionales; se insertan junto al buscador. */
    protected List<Node> filtrosAdicionales() {
        return List.of();
    }

    @FXML
    public void initialize() {
        boolean owner = sesionActual.esOwner();
        raiz.setAccessibleText(titulo());

        buscador.setPromptText("Buscar…");
        buscador.setPrefWidth(220);
        buscador.textProperty().addListener((obs, anterior, nuevo) -> {
            esperaBusqueda.setOnFinished(evento -> cargar(0));
            esperaBusqueda.playFromStart();
        });
        filtroEstado.getItems().setAll("Todos", "Activos", "Inactivos");
        filtroEstado.setValue("Activos");
        filtroEstado.setMinWidth(Region.USE_PREF_SIZE);
        filtroEstado.valueProperty().addListener((obs, anterior, nuevo) -> cargar(0));

        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        barraFiltros.setAlignment(Pos.CENTER_LEFT);
        barraFiltros.getChildren().addAll(buscador, filtroEstado);
        barraFiltros.getChildren().addAll(filtrosAdicionales());
        barraFiltros.getChildren().add(espacio);
        if (owner) {
            Button nuevo = new Button(textoBotonNuevo());
            nuevo.setMinWidth(Region.USE_PREF_SIZE);
            nuevo.getStyleClass().add(Styles.ACCENT);
            nuevo.setOnAction(evento -> abrirFormulario(null));
            barraFiltros.getChildren().add(nuevo);
        }

        tabla.setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_LAST_COLUMN);
        tabla.setPlaceholder(new Label("Sin resultados"));
        configurarColumnas();
        Tablas.aplicarIndicadores(tabla, "Estado");
        if (owner) {
            tabla.getColumns().add(columnaAcciones());
        }
        VBox.setVgrow(tabla, Priority.ALWAYS);
        mensaje.getStyleClass().add("muted-text");

        raiz.getChildren().setAll(barraFiltros, mensaje, tabla, paginador);
    }

    @Override
    public void refrescar() {
        cargar(paginaActual);
    }

    protected TableColumn<R, String> columna(String titulo, double ancho, Function<R, String> valor) {
        TableColumn<R, String> columna = new TableColumn<>(titulo);
        columna.setPrefWidth(ancho);
        columna.setCellValueFactory(celda -> new ReadOnlyStringWrapper(valor.apply(celda.getValue())));
        tabla.getColumns().add(columna);
        return columna;
    }

    protected Window ventana() {
        return raiz.getScene() == null ? null : raiz.getScene().getWindow();
    }

    protected void avisar(String texto) {
        notificador.mostrar(ventana(), texto);
    }

    protected void recargar() {
        cargar(paginaActual);
    }

    /** Vuelve a consultar desde la primera pagina (por ejemplo al cambiar un filtro). */
    protected void reiniciar() {
        cargar(0);
    }

    private TableColumn<R, R> columnaAcciones() {
        TableColumn<R, R> columna = new TableColumn<>("Acciones");
        columna.setPrefWidth(210);
        columna.setSortable(false);
        columna.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue()));
        columna.setCellFactory(c -> new TableCell<>() {
            private final Button editar = new Button("Editar");
            private final Button alternar = new Button();
            private final HBox caja = new HBox(8, editar, alternar);

            {
                editar.setOnAction(evento -> abrirFormulario(getItem()));
                alternar.setOnAction(evento -> alternarEstado(getItem()));
            }

            @Override
            protected void updateItem(R fila, boolean vacio) {
                super.updateItem(fila, vacio);
                if (vacio || fila == null) {
                    setGraphic(null);
                    return;
                }
                alternar.setText(estaActivo(fila) ? "Desactivar" : "Activar");
                setGraphic(caja);
            }
        });
        return columna;
    }

    private void alternarEstado(R fila) {
        if (fila == null) {
            return;
        }
        boolean nuevoEstado = !estaActivo(fila);
        Task<Void> tarea = new Task<>() {
            @Override
            protected Void call() {
                cambiarEstado(fila, nuevoEstado);
                return null;
            }
        };
        tarea.setOnSucceeded(evento -> {
            avisar(nuevoEstado ? "Registro activado." : "Registro desactivado.");
            recargar();
        });
        tarea.setOnFailed(evento -> avisar(textoError(tarea.getException())));
        iniciar(tarea, "botica-catalogo-estado");
    }

    private void cargar(int pagina) {
        paginaActual = Math.max(pagina, 0);
        long actual = ++solicitud;
        Boolean estado = switch (filtroEstado.getValue() == null ? "Todos" : filtroEstado.getValue()) {
            case "Activos" -> Boolean.TRUE;
            case "Inactivos" -> Boolean.FALSE;
            default -> null;
        };
        F filtros = capturarFiltros(buscador.getText(), estado);
        Pageable pageable = PageRequest.of(paginaActual, TAMANO_PAGINA, orden());
        mensaje.setText("Cargando…");

        Task<Page<R>> tarea = new Task<>() {
            @Override
            protected Page<R> call() {
                return consultar(filtros, pageable);
            }
        };
        tarea.setOnSucceeded(evento -> {
            if (actual != solicitud) {
                return;
            }
            Page<R> resultado = tarea.getValue();
            if (resultado.isEmpty() && resultado.getTotalPages() > 0 && paginaActual > 0) {
                cargar(resultado.getTotalPages() - 1);
                return;
            }
            tabla.getItems().setAll(resultado.getContent());
            paginador.actualizar(resultado);
            mensaje.setText("");
        });
        tarea.setOnFailed(evento -> {
            if (actual == solicitud) {
                mensaje.setText(textoError(tarea.getException()));
                paginador.vaciar();
            }
        });
        iniciar(tarea, "botica-catalogo-carga");
    }

    /**
     * Muestra el formulario en un dialogo. El preparador se ejecuta en el hilo de JavaFX (lee los
     * campos) y devuelve la accion que se ejecuta en segundo plano; su resultado, si no es nulo,
     * se muestra como aviso.
     */
    protected void mostrarDialogo(String titulo, Formulario formulario, Supplier<Callable<String>> preparador) {
        DialogoFormulario.mostrar(ventana(), titulo, formulario, preparador, aviso -> {
            avisar(aviso == null ? "Guardado correctamente." : aviso);
            recargar();
        });
    }

    private String textoError(Throwable error) {
        return DialogoFormulario.textoError(error);
    }

    private void iniciar(Task<?> tarea, String nombre) {
        Thread hilo = new Thread(tarea, nombre);
        hilo.setDaemon(true);
        hilo.start();
    }
}
