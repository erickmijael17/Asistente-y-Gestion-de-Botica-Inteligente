package com.botica.inteligente.ui;

import atlantafx.base.controls.Notification;
import javafx.animation.PauseTransition;
import javafx.application.Platform;
import javafx.stage.Popup;
import javafx.stage.Window;
import javafx.util.Duration;
import org.springframework.stereotype.Component;

@Component
public class Notificador {

    public void mostrar(Window ventana, String mensaje) {
        if (ventana == null) {
            return;
        }

        Runnable mostrarNotificacion = () -> mostrarEnHiloJavaFx(ventana, mensaje);
        if (Platform.isFxApplicationThread()) {
            mostrarNotificacion.run();
        } else {
            Platform.runLater(mostrarNotificacion);
        }
    }

    private void mostrarEnHiloJavaFx(Window ventana, String mensaje) {
        if (!ventana.isShowing()) {
            return;
        }

        Notification notificacion = new Notification(mensaje);
        Popup popup = new Popup();
        popup.setAutoHide(true);
        popup.getContent().add(notificacion);
        popup.show(
            ventana,
            ventana.getX() + ventana.getWidth() - notificacion.getPrefWidth() - 24,
            ventana.getY() + 56
        );

        PauseTransition cierreAutomatico = new PauseTransition(Duration.seconds(3));
        cierreAutomatico.setOnFinished(event -> popup.hide());
        cierreAutomatico.play();
    }
}
