package com.botica.inteligente.ui;

import com.botica.inteligente.shared.i18n.Mensajes;
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
import javafx.scene.control.ListCell;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.util.StringConverter;
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
            celda -> new ReadOnlyStringWrapper(rolTexto(celda.getValue().rol()))
        );
        estadoColumn.setCellValueFactory(
            celda -> new ReadOnlyStringWrapper(celda.getValue().estado()
                ? Mensajes.get("usuarios.estado.activo")
                : Mensajes.get("usuarios.estado.inactivo"))
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
                    accionButton.setText(usuario.estado()
                        ? Mensajes.get("usuarios.accion.darBaja")
                        : Mensajes.get("usuarios.accion.reactivar"));
                    accionButton.setDisable(procesando);
                    setGraphic(accionButton);
                }
            }
        });
        rolCombo.setItems(FXCollections.observableArrayList(Rol.values()));
        rolCombo.setConverter(new StringConverter<>() {
            @Override
            public String toString(Rol rol) {
                return rol == null ? "" : rolTexto(rol);
            }

            @Override
            public Rol fromString(String texto) {
                return rolCombo.getValue();
            }
        });
        rolCombo.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(Rol rol, boolean empty) {
                super.updateItem(rol, empty);
                setText(empty || rol == null ? null : rolTexto(rol));
            }
        });
        rolCombo.setValue(Rol.SELLER);
        refrescar();
    }

    private String rolTexto(Rol rol) {
        return Mensajes.get("rol." + rol.name().toLowerCase());
    }

    @Override
    public void refrescar() {
        if (procesando) {
            return;
        }
        procesando = true;
        actualizarEstadoControles();
        mensajeLabel.setText(Mensajes.get("usuarios.cargando"));

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
        mensajeLabel.setText(Mensajes.get("usuarios.guardando"));
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
            mensajeLabel.setText(Mensajes.get("usuarios.creado"));
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
        mensajeLabel.setText(Mensajes.get("usuarios.actualizando"));
        Task<UsuarioResumen> tarea = new Task<>() {
            @Override
            protected UsuarioResumen call() {
                return usuarioService.cambiarEstado(usuario.id(), !usuario.estado());
            }
        };
        tarea.setOnSucceeded(event -> {
            mensajeLabel.setText(Mensajes.get("usuarios.estadoActualizado"));
            procesando = false;
            actualizarEstadoControles();
            refrescar();
        });
        tarea.setOnFailed(event -> finalizarConError(tarea.getException().getMessage()));
        iniciarTarea(tarea);
    }

    private void finalizarConError(String mensaje) {
        mensajeLabel.setText(mensaje == null || mensaje.isBlank()
            ? Mensajes.get("usuarios.errorGenerico")
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
