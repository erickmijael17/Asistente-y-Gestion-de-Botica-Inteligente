package com.botica.inteligente.ui;

import atlantafx.base.theme.PrimerDark;
import atlantafx.base.theme.PrimerLight;
import com.botica.inteligente.security.SesionActual;
import com.botica.inteligente.shared.i18n.Mensajes;
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
import javafx.scene.layout.VBox;
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

    @FXML
    private VBox barraLateral;

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
        String hoja = Application.getUserAgentStylesheet();
        temaOscuro = hoja != null && hoja.toLowerCase().contains("dark");
        areaContenido.sceneProperty().addListener((obs, anterior, escena) -> {
            if (escena != null) {
                aplicarClaseDeTema(escena.getRoot());
            }
        });
        sesionActual.usuarioAutenticado().ifPresentOrElse(
            usuario -> usuarioActual.setText(usuario.nombreVisible() + " · " + usuario.rol()),
            () -> usuarioActual.setText(Mensajes.get("sesion.noDisponible"))
        );
        usuariosButton.setVisible(sesionActual.esOwner());
        usuariosButton.setManaged(sesionActual.esOwner());
        mostrarVista(Mensajes.get("menu.inicio"), "/view/dashboard-view.fxml");
    }

    public void setAlCerrarSesion(Runnable accionCerrarSesion) {
        this.accionCerrarSesion = accionCerrarSesion == null ? () -> { } : accionCerrarSesion;
    }

    @FXML
    private void mostrarDashboard() {
        mostrarVista(Mensajes.get("menu.inicio"), "/view/dashboard-view.fxml");
    }

    @FXML
    private void mostrarProductos() {
        mostrarVista(Mensajes.get("menu.productos"), "/view/productos-view.fxml");
    }

    @FXML
    private void mostrarCategorias() {
        mostrarVista(Mensajes.get("menu.categorias"), "/view/categorias-view.fxml");
    }

    @FXML
    private void mostrarLaboratorios() {
        mostrarVista(Mensajes.get("menu.laboratorios"), "/view/laboratorios-view.fxml");
    }

    @FXML
    private void mostrarInventario() {
        mostrarVista(Mensajes.get("menu.inventario"), "/view/inventario-view.fxml");
    }

    @FXML
    private void mostrarCompras() {
        mostrarVista(Mensajes.get("menu.compras"), "/view/compras-view.fxml");
    }

    @FXML
    private void mostrarPuntoVenta() {
        mostrarVista(Mensajes.get("puntoVenta.titulo"), "/view/punto-venta-view.fxml");
    }

    @FXML
    private void mostrarVentas() {
        mostrarVista(Mensajes.get("menu.ventas"), "/view/ventas-view.fxml");
    }

    @FXML
    private void mostrarUsuarios() {
        if (!sesionActual.esOwner()) {
            notificador.mostrar(
                areaContenido.getScene() == null ? null : areaContenido.getScene().getWindow(),
                Mensajes.get("usuarios.sinPermiso")
            );
            return;
        }
        mostrarVista(Mensajes.get("menu.usuarios"), "/view/usuarios-view.fxml");
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
        if (areaContenido.getScene() != null) {
            aplicarClaseDeTema(areaContenido.getScene().getRoot());
        }

        Window ventana = areaContenido.getScene() == null
            ? null
            : areaContenido.getScene().getWindow();
        notificador.mostrar(ventana, temaOscuro
            ? Mensajes.get("tema.oscuro.activado")
            : Mensajes.get("tema.claro.activado"));
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
                loader.setResources(Mensajes.bundleActual());
                loader.setControllerFactory(applicationContext::getBean);
                Parent raiz = loader.load();
                vista = new VistaEnCache(raiz, loader.getController());
                vistasEnCache.put(ruta, vista);
            }

            areaContenido.getChildren().setAll(vista.raiz());
            tituloVista.setText(titulo);
            marcarActivo(ruta);
            Animaciones.entrada(vista.raiz());
            if (vista.controlador() instanceof Refrescable refrescable) {
                refrescable.refrescar();
            }
        } catch (IOException exception) {
            log.error("No fue posible cargar la vista {}", ruta, exception);
            notificador.mostrar(
                areaContenido.getScene() == null ? null : areaContenido.getScene().getWindow(),
                Mensajes.get("main.error.vista")
            );
        }
    }

    /** Resalta en el menu la seccion que se esta mostrando. */
    private void marcarActivo(String ruta) {
        for (javafx.scene.Node nodo : barraLateral.getChildren()) {
            if (nodo instanceof Button boton && boton.getUserData() != null) {
                boton.getStyleClass().remove("activo");
                if (ruta.equals(boton.getUserData())) {
                    boton.getStyleClass().add("activo");
                }
            }
        }
    }

    private void aplicarClaseDeTema(Parent raiz) {
        raiz.getStyleClass().remove("oscuro");
        if (temaOscuro) {
            raiz.getStyleClass().add("oscuro");
        }
    }

    private record VistaEnCache(Parent raiz, Object controlador) {
    }
}
