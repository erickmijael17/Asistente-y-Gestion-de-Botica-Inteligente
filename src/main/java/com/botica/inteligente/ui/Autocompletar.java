package com.botica.inteligente.ui;

import java.util.List;
import java.util.function.Function;
import javafx.animation.PauseTransition;
import javafx.concurrent.Task;
import javafx.scene.control.ComboBox;
import javafx.util.Duration;
import javafx.util.StringConverter;

/**
 * Selector con busqueda mientras se escribe. Las consultas se ejecutan en un hilo aparte;
 * si el texto escrito no corresponde a una opcion elegida, el valor queda en null.
 */
public class Autocompletar<T> extends ComboBox<T> {

    private final Function<String, List<T>> buscador;
    private final Function<T, String> aTexto;
    private final PauseTransition espera = new PauseTransition(Duration.millis(250));
    private boolean actualizando;
    private long solicitud;

    public Autocompletar(Function<String, List<T>> buscador, Function<T, String> aTexto) {
        this.buscador = buscador;
        this.aTexto = aTexto;
        setEditable(true);
        setConverter(new StringConverter<>() {
            @Override
            public String toString(T objeto) {
                return objeto == null ? "" : aTexto.apply(objeto);
            }

            @Override
            public T fromString(String texto) {
                if (texto == null) {
                    return null;
                }
                return getItems().stream()
                    .filter(item -> aTexto.apply(item).equalsIgnoreCase(texto.trim()))
                    .findFirst()
                    .orElse(null);
            }
        });
        getEditor().textProperty().addListener((observable, anterior, nuevo) -> {
            if (actualizando) {
                return;
            }
            T seleccionado = getValue();
            if (seleccionado != null && aTexto.apply(seleccionado).equals(nuevo)) {
                return;
            }
            espera.setOnFinished(evento -> buscar(nuevo));
            espera.playFromStart();
        });
        setOnShowing(evento -> {
            if (getItems().isEmpty()) {
                buscar("");
            }
        });
    }

    /** Fija una opcion de forma programatica (por ejemplo al editar un registro). */
    public void seleccionar(T opcion) {
        actualizando = true;
        getItems().setAll(List.of(opcion));
        setValue(opcion);
        actualizando = false;
    }

    public void limpiar() {
        actualizando = true;
        setValue(null);
        getEditor().clear();
        actualizando = false;
    }

    private void buscar(String texto) {
        long actual = ++solicitud;
        Task<List<T>> tarea = new Task<>() {
            @Override
            protected List<T> call() {
                return buscador.apply(texto == null ? "" : texto);
            }
        };
        tarea.setOnSucceeded(evento -> {
            if (actual != solicitud) {
                return;
            }
            actualizando = true;
            String escrito = getEditor().getText();
            getItems().setAll(tarea.getValue());
            getEditor().setText(escrito);
            getEditor().positionCaret(escrito == null ? 0 : escrito.length());
            actualizando = false;
            if (!tarea.getValue().isEmpty() && getEditor().isFocused() && !isShowing()) {
                show();
            }
        });
        Thread hilo = new Thread(tarea, "botica-autocompletar");
        hilo.setDaemon(true);
        hilo.start();
    }

    public String textoDe(T opcion) {
        return aTexto.apply(opcion);
    }
}
