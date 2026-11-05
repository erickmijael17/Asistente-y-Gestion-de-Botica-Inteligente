package com.botica.inteligente.ui;

import com.botica.inteligente.usuario.service.UsuarioService;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class SetupInicialController {

    private final UsuarioService usuarioService;
    private java.util.function.Consumer<String> alCompletar = username -> { };

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private PasswordField confirmacionField;

    @FXML
    private Button crearOwnerButton;

    @FXML
    private Label errorLabel;

    public SetupInicialController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @FXML
    private void initialize() {
        crearOwnerButton.setDefaultButton(true);
    }

    public void setAlCompletar(java.util.function.Consumer<String> alCompletar) {
        this.alCompletar = alCompletar == null ? username -> { } : alCompletar;
    }

    @FXML
    private void crearOwner() {
        crearOwnerButton.setDisable(true);
        errorLabel.setText("");
        String username = usernameField.getText();
        String password = passwordField.getText();
        String confirmacion = confirmacionField.getText();

        Task<Void> tarea = new Task<>() {
            @Override
            protected Void call() {
                usuarioService.crearOwnerInicial(username, password, confirmacion);
                return null;
            }
        };
        tarea.setOnSucceeded(event -> {
            passwordField.clear();
            confirmacionField.clear();
            alCompletar.accept(username == null ? "" : username.trim());
        });
        tarea.setOnFailed(event -> {
            passwordField.clear();
            confirmacionField.clear();
            errorLabel.setText(tarea.getException().getMessage());
            crearOwnerButton.setDisable(false);
        });
        iniciarTarea(tarea);
    }

    private void iniciarTarea(Task<?> tarea) {
        Thread hilo = new Thread(tarea, "botica-setup-owner");
        hilo.setDaemon(true);
        hilo.start();
    }
}
