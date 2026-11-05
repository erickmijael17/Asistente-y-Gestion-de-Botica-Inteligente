package com.botica.inteligente.ui;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import com.botica.inteligente.security.SesionActual;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import javafx.application.Platform;
import javafx.application.Application;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.stage.Window;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Scope("prototype")
public class MainController {

    private final ApplicationContext applicationContext;
    private final Notificador notificador;
    private final SesionActual sesionActual;
    private final Map<String, VistaEnCache> vistasEnCache = new HashMap<>();
    private boolean temaOscuro;
    private Runnable accionCerrarSesion = () -> { };

    @FXML
    private StackPane areaContenido;

    @FXML
    private Label tituloVista;

    @FXML
    private Label usuarioActual;

    @FXML
    private Button usuariosButton;

    public MainController(
        ApplicationContext applicationContext,
        Notificador notificador,
        SesionActual sesionActual
    ) {
        this.applicationContext = applicationContext;
        this.notificador = notificador;
        this.sesionActual = sesionActual;
    }

    @FXML
    private void initialize() {
        sesionActual.usuarioAutenticado().ifPresentOrElse(
            usuario -> usuarioActual.setText(usuario.nombreVisible() + " · " + usuario.rol()),
            () -> usuarioActual.setText("Sesión no disponible")
        );
        usuariosButton.setVisible(sesionActual.esOwner());
        usuariosButton.setManaged(sesionActual.esOwner());
        mostrarVista("Dashboard", "/view/dashboard-view.fxml");
    }

    public void setAlCerrarSesion(Runnable accionCerrarSesion) {
        this.accionCerrarSesion = accionCerrarSesion == null ? () -> { } : accionCerrarSesion;
    }

    @FXML
    private void mostrarDashboard() {
        mostrarVista("Dashboard", "/view/dashboard-view.fxml");
    }

    @FXML
    private void mostrarProductos() {
        mostrarVista("Productos", "/view/productos-view.fxml");
    }

    @FXML
    private void mostrarCategorias() {
        mostrarVista("Categorias", "/view/categorias-view.fxml");
    }

    @FXML
    private void mostrarLaboratorios() {
        mostrarVista("Laboratorios", "/view/laboratorios-view.fxml");
    }

    @FXML
    private void mostrarInventario() {
        mostrarVista("Inventario", "/view/inventario-view.fxml");
    }

    @FXML
    private void mostrarCompras() {
        mostrarVista("Compras", "/view/compras-view.fxml");
    }

    @FXML
    private void mostrarVentas() {
        mostrarVista("Ventas", "/view/ventas-view.fxml");
    }

    @FXML
    private void mostrarUsuarios() {
        if (!sesionActual.esOwner()) {
            notificador.mostrar(
                areaContenido.getScene() == null ? null : areaContenido.getScene().getWindow(),
                "No tiene permisos para gestionar usuarios"
            );
            return;
        }
        mostrarVista("Usuarios", "/view/usuarios-view.fxml");
    }

    @FXML
    private void cerrarSesion() {
        sesionActual.cerrarSesion();
        Platform.runLater(accionCerrarSesion);
    }

    @FXML
    private void alternarTema() {
        temaOscuro = !temaOscuro;
        String hojaDeEstilo = temaOscuro
            ? new PrimerDark().getUserAgentStylesheet()
            : new PrimerLight().getUserAgentStylesheet();
        Application.setUserAgentStylesheet(hojaDeEstilo);

        Window ventana = areaContenido.getScene() == null
            ? null
            : areaContenido.getScene().getWindow();
        notificador.mostrar(ventana, temaOscuro ? "Tema oscuro activado" : "Tema claro activado");
    }

    private void mostrarVista(String titulo, String ruta) {
        try {
            VistaEnCache vista = vistasEnCache.get(ruta);
            if (vista == null) {
                URL recurso = MainController.class.getResource(ruta);
                if (recurso == null) {
                    throw new IOException("No se encontro la vista " + ruta);
                }

                FXMLLoader loader = new FXMLLoader(recurso);
                loader.setControllerFactory(applicationContext::getBean);
                Parent raiz = loader.load();
                vista = new VistaEnCache(raiz, loader.getController());
                vistasEnCache.put(ruta, vista);
            }

            areaContenido.getChildren().setAll(vista.raiz());
            tituloVista.setText(titulo);
            if (vista.controlador() instanceof Refrescable refrescable) {
                refrescable.refrescar();
            }
        } catch (IOException exception) {
            log.error("No fue posible cargar la vista {}", ruta, exception);
            notificador.mostrar(
                areaContenido.getScene() == null ? null : areaContenido.getScene().getWindow(),
                "No se pudo abrir la vista solicitada"
            );
        }
    }

    private record VistaEnCache(Parent raiz, Object controlador) {
    }
}
