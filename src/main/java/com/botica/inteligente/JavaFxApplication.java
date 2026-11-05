package com.botica.inteligente;

import atlantafx.base.theme.PrimerLight;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Pos;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressIndicator;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;
import com.botica.inteligente.ui.LoginController;
import com.botica.inteligente.ui.MainController;
import com.botica.inteligente.ui.SetupInicialController;
import com.botica.inteligente.usuario.service.UsuarioService;

@Slf4j
public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        context = new SpringApplicationBuilder(BoticaApplication.class)
            .web(WebApplicationType.NONE)
            .headless(false)
            .run(getParameters().getRaw().toArray(String[]::new));
        log.info("Contexto de Botica Inteligente iniciado");
    }

    @Override
    public void start(Stage stage) throws IOException {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());
        stage.setTitle("Botica Inteligente");
        stage.setMinWidth(380);
        stage.setMinHeight(480);
        stage.setScene(new Scene(vistaCargando(), 440, 300));
        stage.show();

        Task<Boolean> comprobarOwner = new Task<>() {
            @Override
            protected Boolean call() {
                return context.getBean(UsuarioService.class).existeOwner();
            }
        };
        comprobarOwner.setOnSucceeded(event -> {
            if (comprobarOwner.getValue()) {
                mostrarLogin(stage, "", "");
            } else {
                mostrarSetupInicial(stage);
            }
        });
        comprobarOwner.setOnFailed(event -> {
            log.error("No fue posible comprobar la configuración inicial de usuarios", comprobarOwner.getException());
            Alert alerta = new Alert(Alert.AlertType.ERROR, "No se pudo iniciar la aplicación.");
            alerta.initOwner(stage);
            alerta.showAndWait();
            stage.close();
        });
        iniciarTarea(comprobarOwner, "botica-comprobar-owner");
    }

    private Parent vistaCargando() {
        VBox contenido = new VBox(16, new ProgressIndicator(), new Label("Preparando la aplicación…"));
        contenido.setAlignment(Pos.CENTER);
        return contenido;
    }

    private void mostrarSetupInicial(Stage stage) {
        cargarVista(stage, "/view/setup-inicial-view.fxml", controller -> {
            SetupInicialController setupController = (SetupInicialController) controller;
            setupController.setAlCompletar(username -> mostrarLogin(
                stage,
                username,
                "Configuración completada. Inicia sesión."
            ));
        }, 500, 610, 380, 500);
    }

    private void mostrarLogin(Stage stage, String usernameInicial, String mensajeInicial) {
        cargarVista(stage, "/view/login-view.fxml", controller -> {
            LoginController loginController = (LoginController) controller;
            loginController.setAlAutenticar(() -> mostrarMain(stage));
            loginController.prellenar(usernameInicial, mensajeInicial);
        }, 480, 540, 380, 480);
    }

    private void mostrarMain(Stage stage) {
        cargarVista(stage, "/view/main-view.fxml", controller -> {
            MainController mainController = (MainController) controller;
            mainController.setAlCerrarSesion(() -> mostrarLogin(stage, "", ""));
        }, 1180, 760, 960, 640);
    }

    private void cargarVista(
        Stage stage,
        String ruta,
        java.util.function.Consumer<Object> configurarController,
        double ancho,
        double alto,
        double anchoMinimo,
        double altoMinimo
    ) {
        try {
            FXMLLoader loader = new FXMLLoader(JavaFxApplication.class.getResource(ruta));
            loader.setControllerFactory(context::getBean);
            Parent raiz = loader.load();
            configurarController.accept(loader.getController());

            Scene scene = new Scene(raiz, ancho, alto);
            com.botica.inteligente.ui.Animaciones.entrada(raiz.lookup(".auth-card"));
            scene.getStylesheets().add(
                JavaFxApplication.class.getResource("/css/app.css").toExternalForm()
            );
            stage.setMinWidth(anchoMinimo);
            stage.setMinHeight(altoMinimo);
            stage.setWidth(ancho);
            stage.setHeight(alto);
            stage.setScene(scene);
            stage.centerOnScreen();
        } catch (IOException exception) {
            log.error("No fue posible cargar la vista {}", ruta, exception);
            Alert alerta = new Alert(Alert.AlertType.ERROR, "No se pudo abrir la pantalla solicitada.");
            alerta.initOwner(stage);
            alerta.showAndWait();
        }
    }

    private void iniciarTarea(Task<?> task, String nombreHilo) {
        Thread hilo = new Thread(task, nombreHilo);
        hilo.setDaemon(true);
        hilo.start();
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
            log.info("Contexto de Botica Inteligente cerrado");
        }
    }
}
