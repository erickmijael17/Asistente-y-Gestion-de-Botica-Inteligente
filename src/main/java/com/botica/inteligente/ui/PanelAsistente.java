package com.botica.inteligente.ui;

import atlantafx.base.theme.Styles;
import com.botica.inteligente.asistente.service.AsistenteService;
import com.botica.inteligente.asistente.service.RespuestaAsistente;
import com.botica.inteligente.asistente.service.Sugerencia;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import javafx.application.Platform;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ScrollPane;
import javafx.scene.control.TextField;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Chat de consulta de medicamentos reutilizable: se usa a pantalla completa y como panel lateral
 * del punto de venta, donde cada sugerencia puede agregarse al carrito. Las sugerencias las valida
 * siempre una persona (farmaceutico) antes de entregar el producto.
 */
public class PanelAsistente extends VBox {

    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final List<String> EJEMPLOS = List.of(
        "Fiebre", "Dolor de garganta", "Alergia", "Acidez", "Tos"
    );

    private final AsistenteService asistenteService;
    private final Consumer<Sugerencia> alAgregar;
    private final boolean compacto;
    private final VBox conversacion = new VBox(10);
    private final ScrollPane desplazamiento = new ScrollPane(conversacion);
    private final TextField entrada = new TextField();
    private final Button enviar = new Button("Consultar");
    private boolean saludado;

    /**
     * @param alAgregar acciones al pulsar "Agregar" en una sugerencia; null si no hay carrito
     * @param compacto  disposicion para un panel lateral estrecho
     */
    public PanelAsistente(AsistenteService asistenteService, Consumer<Sugerencia> alAgregar, boolean compacto) {
        super(10);
        this.asistenteService = asistenteService;
        this.alAgregar = alAgregar;
        this.compacto = compacto;

        Label aviso = new Label(RespuestaAsistente.AVISO_ORIENTATIVO);
        aviso.getStyleClass().add("muted-text");
        aviso.setWrapText(true);
        aviso.setMinHeight(Region.USE_PREF_SIZE);
        aviso.prefWidthProperty().bind(widthProperty().subtract(24));
        if (compacto) {
            aviso.setStyle("-fx-font-size: 11px;");
        }

        FlowPane ejemplos = new FlowPane(6, 6);
        for (String ejemplo : EJEMPLOS) {
            Button boton = new Button(ejemplo);
            boton.getStyleClass().add(Styles.SMALL);
            boton.setOnAction(evento -> preguntar(ejemplo));
            ejemplos.getChildren().add(boton);
        }

        conversacion.setPadding(new Insets(6));
        desplazamiento.setFitToWidth(true);
        desplazamiento.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        desplazamiento.getStyleClass().add("marco-transparente");
        VBox.setVgrow(desplazamiento, Priority.ALWAYS);

        entrada.setPromptText(compacto
            ? "Dolencias del paciente…"
            : "Escriba un síntoma, un medicamento o un principio activo…");
        entrada.setOnAction(evento -> preguntar(entrada.getText()));
        HBox.setHgrow(entrada, Priority.ALWAYS);
        enviar.getStyleClass().add(Styles.ACCENT);
        enviar.setOnAction(evento -> preguntar(entrada.getText()));
        HBox barra = new HBox(8, entrada, enviar);
        barra.setAlignment(Pos.CENTER_LEFT);

        getChildren().addAll(aviso, ejemplos, desplazamiento, barra);
    }

    /** Muestra el saludo la primera vez y deja el cursor listo para escribir. */
    public void iniciar() {
        saludar();
        entrada.requestFocus();
    }

    /** Muestra el saludo la primera vez, sin cambiar el foco. */
    public void saludar() {
        if (!saludado) {
            saludado = true;
            agregarBurbuja(
                compacto
                    ? "Cuénteme las dolencias del paciente (por ejemplo: «fiebre y dolor de garganta») y le sugiero "
                        + "qué pastillas del inventario pueden servir. Usted valida la entrega."
                    : "Hola, puedo ayudarle a buscar medicamentos del inventario por síntoma, nombre o principio "
                        + "activo, y a sugerir alternativas con su stock y precio. ¿Qué necesita el cliente?",
                false
            );
        }
    }

    public void preguntar(String texto) {
        String pregunta = texto == null ? "" : texto.trim();
        if (pregunta.isEmpty()) {
            return;
        }
        entrada.clear();
        agregarBurbuja(pregunta, true);
        HBox buscando = fila(burbuja("Buscando en el inventario…", false), false);
        conversacion.getChildren().add(buscando);
        enviar.setDisable(true);
        Fondo.ejecutar(
            () -> asistenteService.consultar(pregunta),
            respuesta -> {
                conversacion.getChildren().remove(buscando);
                mostrarRespuesta(respuesta);
                enviar.setDisable(false);
            },
            error -> {
                conversacion.getChildren().remove(buscando);
                agregarBurbuja(DialogoFormulario.textoError(error), false);
                enviar.setDisable(false);
            }
        );
    }

    private void mostrarRespuesta(RespuestaAsistente respuesta) {
        HBox inicio = fila(burbuja(respuesta.texto(), false), false);
        conversacion.getChildren().add(inicio);
        Animaciones.entrada(inicio);
        if (!respuesta.sugerencias().isEmpty()) {
            Pane tarjetas = compacto ? new VBox(10) : new FlowPane(12, 12);
            for (Sugerencia sugerencia : respuesta.sugerencias()) {
                tarjetas.getChildren().add(tarjeta(sugerencia));
            }
            conversacion.getChildren().add(tarjetas);
            Animaciones.escalonada(tarjetas.getChildren());
        }
        desplazarA(inicio);
    }

    /** Deja visible el comienzo de la respuesta en lugar de saltar al final de las sugerencias. */
    private void desplazarA(Node destino) {
        Platform.runLater(() -> {
            conversacion.layout();
            double sobrante = conversacion.getHeight() - desplazamiento.getViewportBounds().getHeight();
            double y = destino.getBoundsInParent().getMinY();
            desplazamiento.setVvalue(sobrante <= 0 ? 0 : Math.min(1.0, Math.max(0, y / sobrante)));
        });
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
        if (!s.cubre().isEmpty()) {
            Label cubre = new Label("Sirve para: " + String.join(", ", s.cubre()));
            cubre.getStyleClass().add(Styles.ACCENT);
            cubre.setWrapText(true);
            tarjeta.getChildren().add(cubre);
        }
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

        HBox acciones = new HBox(8);
        if (alAgregar != null) {
            Button agregar = new Button("Agregar al carrito");
            agregar.getStyleClass().add(Styles.ACCENT);
            agregar.setDisable(!s.hayStock());
            if (!s.hayStock()) {
                agregar.setTooltip(new Tooltip("Sin stock disponible"));
            }
            agregar.setOnAction(evento -> alAgregar.accept(s));
            acciones.getChildren().add(agregar);
        }
        Button ficha = new Button("Ver ficha");
        ficha.setOnAction(evento -> abrirFicha(s));
        acciones.getChildren().add(ficha);
        tarjeta.getChildren().add(acciones);

        tarjeta.getStyleClass().add("kpi-card");
        tarjeta.setPadding(new Insets(12, 14, 12, 14));
        if (!compacto) {
            tarjeta.setPrefWidth(250);
            tarjeta.setMaxWidth(250);
        }
        Animaciones.elevarAlPasar(tarjeta);
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
        etiqueta.setMaxWidth(compacto ? 270 : 560);
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
        return getScene() == null ? null : getScene().getWindow();
    }
}
