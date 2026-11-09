package com.botica.inteligente.ui;

import com.botica.inteligente.shared.exception.CampoInvalidoException;
import com.botica.inteligente.shared.exception.NegocioException;
import com.botica.inteligente.shared.i18n.Mensajes;
import java.util.concurrent.Callable;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.geometry.Insets;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.ScrollPane;
import javafx.stage.Window;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Dialogo con un formulario. El preparador lee los campos en el hilo de JavaFX y devuelve la
 * accion que se ejecuta en segundo plano; si falla, el error se muestra en el formulario y el
 * dialogo sigue abierto.
 */
public final class DialogoFormulario {

    private static final Logger log = LoggerFactory.getLogger(DialogoFormulario.class);

    private DialogoFormulario() {
    }

    /**
     * @param alGuardar recibe el texto devuelto por la accion (puede ser null) cuando se guardo bien
     */
    public static void mostrar(
        Window propietario, String titulo, Formulario formulario,
        Supplier<Callable<String>> preparador, Consumer<String> alGuardar
    ) {
        Dialog<Void> dialogo = new Dialog<>();
        dialogo.setTitle(titulo);
        dialogo.setHeaderText(titulo);
        dialogo.initOwner(propietario);
        ScrollPane desplazamiento = new ScrollPane(formulario.nodo());
        desplazamiento.setFitToWidth(true);
        desplazamiento.setPrefViewportWidth(760);
        desplazamiento.setPrefViewportHeight(520);
        desplazamiento.setPadding(new Insets(4));
        dialogo.getDialogPane().setContent(desplazamiento);
        ButtonType guardar = new ButtonType(Mensajes.get("dialogo.guardar"), ButtonBar.ButtonData.OK_DONE);
        dialogo.getDialogPane().getButtonTypes().addAll(guardar, ButtonType.CANCEL);
        Button botonGuardar = (Button) dialogo.getDialogPane().lookupButton(guardar);
        botonGuardar.addEventFilter(ActionEvent.ACTION, evento -> {
            evento.consume();
            ejecutar(dialogo, botonGuardar, formulario, preparador, alGuardar);
        });
        dialogo.show();
    }

    private static void ejecutar(
        Dialog<Void> dialogo, Button botonGuardar, Formulario formulario,
        Supplier<Callable<String>> preparador, Consumer<String> alGuardar
    ) {
        formulario.limpiarErrores();
        Callable<String> accion;
        try {
            accion = preparador.get();
        } catch (CampoInvalidoException error) {
            formulario.mostrarError(error.getCampo(), error.getMessage());
            return;
        }
        botonGuardar.setDisable(true);
        Task<String> tarea = new Task<>() {
            @Override
            protected String call() throws Exception {
                return accion.call();
            }
        };
        tarea.setOnSucceeded(evento -> {
            dialogo.setResult(null);
            dialogo.close();
            alGuardar.accept(tarea.getValue());
        });
        tarea.setOnFailed(evento -> {
            botonGuardar.setDisable(false);
            Throwable error = tarea.getException();
            if (error instanceof CampoInvalidoException campo) {
                formulario.mostrarError(campo.getCampo(), campo.getMessage());
            } else {
                formulario.mostrarError(null, textoError(error));
            }
        });
        Thread hilo = new Thread(tarea, "botica-formulario-guardar");
        hilo.setDaemon(true);
        hilo.start();
    }

    static String textoError(Throwable error) {
        if (error instanceof NegocioException) {
            return error.getMessage();
        }
        log.error("Error inesperado al guardar", error);
        return Mensajes.get("usuarios.errorGenerico");
    }
}
