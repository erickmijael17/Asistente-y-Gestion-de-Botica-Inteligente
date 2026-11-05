package com.botica.inteligente.ui;

import com.botica.inteligente.usuario.enums.Rol;
import com.botica.inteligente.usuario.service.UsuarioResumen;
import com.botica.inteligente.usuario.service.UsuarioService;
import java.util.List;
import javafx.collections.FXCollections;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.beans.property.ReadOnlyObjectWrapper;
import javafx.beans.property.ReadOnlyStringWrapper;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class UsuariosController implements Refrescable {

    private final UsuarioService usuarioService;
    private boolean procesando;

    @FXML
    private TableView<UsuarioResumen> usuariosTabla;

    @FXML
    private TableColumn<UsuarioResumen, String> usernameColumn;

    @FXML
    private TableColumn<UsuarioResumen, String> rolColumn;

    @FXML
    private TableColumn<UsuarioResumen, String> estadoColumn;

    @FXML
    private TableColumn<UsuarioResumen, UsuarioResumen> accionColumn;

    @FXML
    private TextField usernameField;

    @FXML
    private PasswordField passwordField;

    @FXML
    private ComboBox<Rol> rolCombo;

    @FXML
    private Button crearUsuarioButton;

    @FXML
    private Button actualizarButton;

    @FXML
    private Label mensajeLabel;

    public UsuariosController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @FXML
    private void initialize() {
        usernameColumn.setCellValueFactory(
            celda -> new ReadOnlyStringWrapper(celda.getValue().username())
        );
        rolColumn.setCellValueFactory(
            celda -> new ReadOnlyStringWrapper(celda.getValue().rol().name())
        );
        estadoColumn.setCellValueFactory(
            celda -> new ReadOnlyStringWrapper(celda.getValue().estado() ? "Activo" : "Inactivo")
        );
        accionColumn.setCellValueFactory(celda -> new ReadOnlyObjectWrapper<>(celda.getValue()));
        accionColumn.setCellFactory(columna -> new TableCell<>() {
            private final Button accionButton = new Button();

            {
                accionButton.setOnAction(event -> {
                    UsuarioResumen usuario = getItem();
                    if (usuario != null) {
                        cambiarEstado(usuario);
                    }
                });
            }

            @Override
            protected void updateItem(UsuarioResumen usuario, boolean empty) {
                super.updateItem(usuario, empty);
                if (empty || usuario == null) {
                    setGraphic(null);
                } else {
                    accionButton.setText(usuario.estado() ? "Dar de baja" : "Reactivar");
                    accionButton.setDisable(procesando);
                    setGraphic(accionButton);
                }
            }
        });
        rolCombo.setItems(FXCollections.observableArrayList(Rol.values()));
        rolCombo.setValue(Rol.SELLER);
        refrescar();
    }

    @Override
    public void refrescar() {
        if (procesando) {
            return;
        }
        procesando = true;
        actualizarEstadoControles();
        mensajeLabel.setText("Cargando usuarios…");

        Task<List<UsuarioResumen>> tarea = new Task<>() {
            @Override
            protected List<UsuarioResumen> call() {
                return usuarioService.listarUsuarios();
            }
        };
        tarea.setOnSucceeded(event -> {
            usuariosTabla.getItems().setAll(tarea.getValue());
            mensajeLabel.setText("");
            procesando = false;
            actualizarEstadoControles();
            usuariosTabla.refresh();
        });
        tarea.setOnFailed(event -> finalizarConError(tarea.getException().getMessage()));
        iniciarTarea(tarea);
    }

    @FXML
    private void crearUsuario() {
        if (procesando) {
            return;
        }
        procesando = true;
        actualizarEstadoControles();
        mensajeLabel.setText("Guardando usuario…");
        String username = usernameField.getText();
        String password = passwordField.getText();
        Rol rol = rolCombo.getValue();

        Task<UsuarioResumen> tarea = new Task<>() {
            @Override
            protected UsuarioResumen call() {
                return usuarioService.crearUsuario(username, password, rol);
            }
        };
        tarea.setOnSucceeded(event -> {
            usernameField.clear();
            passwordField.clear();
            mensajeLabel.setText("Usuario creado.");
            procesando = false;
            actualizarEstadoControles();
            refrescar();
        });
        tarea.setOnFailed(event -> finalizarConError(tarea.getException().getMessage()));
        iniciarTarea(tarea);
    }

    private void cambiarEstado(UsuarioResumen usuario) {
        if (procesando) {
            return;
        }
        procesando = true;
        actualizarEstadoControles();
        mensajeLabel.setText("Actualizando usuario…");
        Task<UsuarioResumen> tarea = new Task<>() {
            @Override
            protected UsuarioResumen call() {
                return usuarioService.cambiarEstado(usuario.id(), !usuario.estado());
            }
        };
        tarea.setOnSucceeded(event -> {
            mensajeLabel.setText("Estado actualizado.");
            procesando = false;
            actualizarEstadoControles();
            refrescar();
        });
        tarea.setOnFailed(event -> finalizarConError(tarea.getException().getMessage()));
        iniciarTarea(tarea);
    }

    private void finalizarConError(String mensaje) {
        mensajeLabel.setText(mensaje == null || mensaje.isBlank()
            ? "No se pudo completar la operación."
            : mensaje);
        procesando = false;
        actualizarEstadoControles();
    }

    private void actualizarEstadoControles() {
        if (crearUsuarioButton != null) {
            crearUsuarioButton.setDisable(procesando);
        }
        if (actualizarButton != null) {
            actualizarButton.setDisable(procesando);
        }
        if (usuariosTabla != null) {
            usuariosTabla.setDisable(procesando);
            usuariosTabla.refresh();
        }
    }

    private void iniciarTarea(Task<?> tarea) {
        Thread hilo = new Thread(tarea, "botica-usuarios");
        hilo.setDaemon(true);
        hilo.start();
    }
}
