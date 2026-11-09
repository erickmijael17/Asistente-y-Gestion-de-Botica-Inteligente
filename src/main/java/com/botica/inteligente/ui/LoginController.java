package com.botica.inteligente.ui;

import com.botica.inteligente.shared.i18n.Idioma;
import com.botica.inteligente.shared.i18n.Mensajes;
import com.botica.inteligente.shared.i18n.PreferenciasApp;
import com.botica.inteligente.usuario.service.AuthService;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class LoginController {

    private final AuthService authService;
    private Runnable alAutenticar = () -> { };
    private Runnable alCambiarIdioma = () -> { };

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private Button ingresarButton;

    @FXML
    private Label errorLabel;

    @FXML
    private ComboBox<Idioma> idiomaCombo;

    public LoginController(AuthService authService) {
        this.authService = authService;
    }

    @FXML
    private void initialize() {
        ingresarButton.setDefaultButton(true);
        usernameField.requestFocus();

        idiomaCombo.setItems(FXCollections.observableArrayList(Idioma.values()));
        idiomaCombo.setValue(Mensajes.idiomaActual());
        idiomaCombo.setOnAction(event -> {
            Idioma seleccionado = idiomaCombo.getValue();
            if (seleccionado != null && seleccionado != Mensajes.idiomaActual()) {
                PreferenciasApp.guardarIdioma(seleccionado);
                Mensajes.establecerIdioma(seleccionado);
                alCambiarIdioma.run();
            }
        });
    }

    public void setAlAutenticar(Runnable alAutenticar) {
        this.alAutenticar = alAutenticar == null ? () -> { } : alAutenticar;
    }

    public void setAlCambiarIdioma(Runnable alCambiarIdioma) {
        this.alCambiarIdioma = alCambiarIdioma == null ? () -> { } : alCambiarIdioma;
    }

    public String usernameActual() {
        return usernameField.getText();
    }

    public void prellenar(String username, String mensaje) {
        usernameField.setText(username == null ? "" : username);
        errorLabel.setText(mensaje == null ? "" : mensaje);
        if (mensaje != null && !mensaje.isBlank()) {
            errorLabel.getStyleClass().add("auth-success");
        }
    }

    @FXML
    private void ingresar() {
        ingresarButton.setDisable(true);
        errorLabel.getStyleClass().remove("auth-success");
        errorLabel.setText("");

        String username = usernameField.getText();
        String password = passwordField.getText();
        Task<Void> tarea = new Task<>() {
            @Override
            protected Void call() {
                authService.autenticar(username, password);
                return null;
            }
        };
        tarea.setOnSucceeded(event -> {
            passwordField.clear();
            alAutenticar.run();
        });
        tarea.setOnFailed(event -> {
            passwordField.clear();
            errorLabel.setText(tarea.getException().getMessage());
            ingresarButton.setDisable(false);
        });
        iniciarTarea(tarea);
    }

    private void iniciarTarea(Task<?> tarea) {
        Thread hilo = new Thread(tarea, "botica-login");
        hilo.setDaemon(true);
        hilo.start();
    }
}
