package com.botica.inteligente.ui;

import atlantafx.base.theme.Styles;
import com.botica.inteligente.asistente.service.AsistenteService;
import com.botica.inteligente.asistente.service.RespuestaAsistente;
import com.botica.inteligente.asistente.service.Sugerencia;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Chat de consulta de medicamentos: responde con productos del catalogo y su disponibilidad. */
@Component
@Scope("prototype")
public class AsistenteController implements Refrescable {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<String> EJEMPLOS = List.of(
        "Pastillas para la fiebre", "Algo para la alergia", "Dolor de garganta", "Alternativas a paracetamol"
    );

    private final AsistenteService asistenteService;

    @FXML
    private VBox raiz;

    private final VBox conversacion = new VBox(10);
    private final ScrollPane desplazamiento = new ScrollPane(conversacion);
    private final TextField entrada = new TextField();
    private final Button enviar = new Button("Preguntar");
    private boolean saludado;

    public AsistenteController(AsistenteService asistenteService) {
        this.asistenteService = asistenteService;
    }

    @FXML
    public void initialize() {
        Label aviso = new Label(RespuestaAsistente.AVISO_ORIENTATIVO);
        aviso.getStyleClass().add("muted-text");
        aviso.setWrapText(true);

        FlowPane ejemplos = new FlowPane(8, 8);
        for (String ejemplo : EJEMPLOS) {
            Button boton = new Button(ejemplo);
            boton.setOnAction(evento -> preguntar(ejemplo));
            ejemplos.getChildren().add(boton);
        }

        conversacion.setPadding(new Insets(8));
        desplazamiento.setFitToWidth(true);
        desplazamiento.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        VBox.setVgrow(desplazamiento, Priority.ALWAYS);

        entrada.setPromptText("Escriba un síntoma, un medicamento o un principio activo…");
        entrada.setOnAction(evento -> preguntar(entrada.getText()));
        HBox.setHgrow(entrada, Priority.ALWAYS);
        enviar.getStyleClass().add(Styles.ACCENT);
        enviar.setOnAction(evento -> preguntar(entrada.getText()));
        HBox barra = new HBox(10, entrada, enviar);
        barra.setAlignment(Pos.CENTER_LEFT);

        raiz.getChildren().setAll(aviso, ejemplos, desplazamiento, barra);
    }

    @Override
    public void refrescar() {
        if (!saludado) {
            saludado = true;
            agregarBurbuja(
                "Hola, puedo ayudarle a buscar medicamentos del inventario por síntoma, nombre o principio activo, "
                    + "y a sugerir alternativas con su stock y precio. ¿Qué necesita el cliente?",
                false
            );
        }
        entrada.requestFocus();
    }

    private void preguntar(String texto) {
        String pregunta = texto == null ? "" : texto.trim();
        if (pregunta.isEmpty()) {
            return;
        }
        entrada.clear();
        agregarBurbuja(pregunta, true);
        Label buscando = burbuja("Buscando en el catálogo…", false);
        conversacion.getChildren().add(fila(buscando, false));
        enviar.setDisable(true);
        Fondo.ejecutar(
            () -> asistenteService.consultar(pregunta),
            respuesta -> {
                conversacion.getChildren().remove(conversacion.getChildren().size() - 1);
                mostrarRespuesta(respuesta);
                enviar.setDisable(false);
            },
            error -> {
                conversacion.getChildren().remove(conversacion.getChildren().size() - 1);
                agregarBurbuja(DialogoFormulario.textoError(error), false);
                enviar.setDisable(false);
            }
        );
    }

    private void mostrarRespuesta(RespuestaAsistente respuesta) {
        agregarBurbuja(respuesta.texto(), false);
        if (!respuesta.sugerencias().isEmpty()) {
            FlowPane tarjetas = new FlowPane(12, 12);
            for (Sugerencia sugerencia : respuesta.sugerencias()) {
                tarjetas.getChildren().add(tarjeta(sugerencia));
            }
            conversacion.getChildren().add(tarjetas);
            Animaciones.escalonada(tarjetas.getChildren());
        }
        desplazarAlFinal();
    }

    // ---------- presentacion ----------

    private Node tarjeta(Sugerencia s) {
        Label nombre = new Label(s.nombre());
        nombre.getStyleClass().add(Styles.TEXT_BOLD);
        nombre.setWrapText(true);
        VBox tarjeta = new VBox(4, nombre);
        if (s.nombreGenerico() != null) {
            tarjeta.getChildren().add(muted(s.nombreGenerico()));
        }
        tarjeta.getChildren().add(muted(unir(s.principioActivo(), s.concentracion(), s.presentacion())));
        tarjeta.getChildren().add(muted(s.laboratorio() + " · " + s.categoria()));
        Label disponibilidad = new Label(
            "S/ " + s.precioVenta().setScale(2, java.math.RoundingMode.HALF_UP) + " · "
                + (s.hayStock() ? "Stock: " + s.stock() : "Sin stock")
        );
        disponibilidad.getStyleClass().add(s.hayStock() ? Styles.SUCCESS : Styles.DANGER);
        tarjeta.getChildren().add(disponibilidad);
        if (s.requiereReceta()) {
            Label receta = new Label("Requiere receta médica");
            receta.getStyleClass().add(Styles.WARNING);
            tarjeta.getChildren().add(receta);
        }
        Button ficha = new Button("Ver ficha");
        ficha.setOnAction(evento -> abrirFicha(s));
        tarjeta.getChildren().add(ficha);
        tarjeta.getStyleClass().add("kpi-card");
        tarjeta.setPadding(new Insets(12, 14, 12, 14));
        Animaciones.elevarAlPasar(tarjeta);
        tarjeta.setPrefWidth(250);
        tarjeta.setMaxWidth(250);
        return tarjeta;
    }

    private void abrirFicha(Sugerencia s) {
        VBox contenido = new VBox(10);
        contenido.setPrefWidth(520);
        agregarSeccion(contenido, "Indicaciones", s.indicaciones());
        agregarSeccion(contenido, "Contraindicaciones", s.contraindicaciones());
        agregarSeccion(contenido, "Advertencias", s.advertencias());
        agregarSeccion(contenido, "Precauciones", s.precauciones());
        agregarSeccion(contenido, "Vía de administración", s.viaAdministracion());
        agregarSeccion(contenido, "Almacenamiento", s.condicionesAlmacenamiento());
        String fuente = s.fuenteInformacion() == null ? "sin fuente registrada" : s.fuenteInformacion();
        String fecha = s.fechaInformacion() == null ? "" : " · actualizado el " + FECHA.format(s.fechaInformacion());
        contenido.getChildren().add(muted("Fuente: " + fuente + fecha));
        contenido.getChildren().add(muted(RespuestaAsistente.AVISO_ORIENTATIVO));

        ScrollPane marco = new ScrollPane(contenido);
        marco.setFitToWidth(true);
        marco.setPrefViewportHeight(420);
        Dialog<Void> dialogo = new Dialog<>();
        dialogo.initOwner(ventana());
        dialogo.setTitle("Ficha del producto");
        dialogo.setHeaderText(s.nombre() + (s.requiereReceta() ? " — requiere receta" : ""));
        dialogo.getDialogPane().setContent(marco);
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);
        dialogo.show();
    }

    private void agregarSeccion(VBox contenido, String titulo, String texto) {
        Label cabecera = new Label(titulo);
        cabecera.getStyleClass().add(Styles.TEXT_BOLD);
        Label cuerpo = new Label(texto == null ? "No registrado en el catálogo." : texto);
        cuerpo.setWrapText(true);
        if (texto == null) {
            cuerpo.getStyleClass().add("muted-text");
        }
        contenido.getChildren().add(new VBox(2, cabecera, cuerpo));
    }

    private void agregarBurbuja(String texto, boolean usuario) {
        HBox nueva = fila(burbuja(texto, usuario), usuario);
        conversacion.getChildren().add(nueva);
        Animaciones.entrada(nueva);
        desplazarAlFinal();
    }

    private Label burbuja(String texto, boolean usuario) {
        Label etiqueta = new Label(texto);
        etiqueta.setWrapText(true);
        etiqueta.setMaxWidth(560);
        etiqueta.setPadding(new Insets(8, 12, 8, 12));
        etiqueta.getStyleClass().add(usuario ? "burbuja-usuario" : "burbuja-asistente");
        return etiqueta;
    }

    private HBox fila(Label burbuja, boolean usuario) {
        HBox fila = new HBox(burbuja);
        fila.setAlignment(usuario ? Pos.CENTER_RIGHT : Pos.CENTER_LEFT);
        return fila;
    }

    private Label muted(String texto) {
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().add("muted-text");
        etiqueta.setWrapText(true);
        return etiqueta;
    }

    private static String unir(String... partes) {
        List<String> presentes = new ArrayList<>();
        for (String parte : partes) {
            if (parte != null && !parte.isBlank()) {
                presentes.add(parte);
            }
        }
        return presentes.isEmpty() ? "Sin detalle de composición" : String.join(" · ", presentes);
    }

    private void desplazarAlFinal() {
        Platform.runLater(() -> {
            conversacion.layout();
            desplazamiento.setVvalue(1.0);
        });
    }

    private Window ventana() {
        return raiz.getScene() == null ? null : raiz.getScene().getWindow();
    }
}
