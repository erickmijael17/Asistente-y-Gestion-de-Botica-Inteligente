package com.botica.inteligente.ui;

import atlantafx.base.theme.Styles;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javafx.geometry.Insets;
import javafx.scene.Node;
import javafx.scene.control.CheckBox;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Control;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextField;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;

/** Formulario con validacion inline: cada campo puede mostrar su propio mensaje de error. */
public class Formulario {

    private final GridPane rejilla = new GridPane();
    private final VBox raiz = new VBox(12, rejilla);
    private final Label errorGeneral = new Label();
    private final Map<String, Label> errores = new HashMap<>();
    private final Map<String, Control> controles = new HashMap<>();
    private final int columnas;
    private int fila;
    private int columna;

    public Formulario(int columnas) {
        this.columnas = columnas;
        rejilla.setHgap(14);
        rejilla.setVgap(10);
        for (int i = 0; i < columnas; i++) {
            ColumnConstraints restriccion = new ColumnConstraints();
            restriccion.setPercentWidth(100.0 / columnas);
            restriccion.setHgrow(Priority.ALWAYS);
            rejilla.getColumnConstraints().add(restriccion);
        }
        errorGeneral.getStyleClass().add(Styles.DANGER);
        errorGeneral.setWrapText(true);
        errorGeneral.setVisible(false);
        errorGeneral.setManaged(false);
        raiz.setPadding(new Insets(4));
        raiz.getChildren().add(errorGeneral);
    }

    public Node nodo() {
        return raiz;
    }

    public void titulo(String texto) {
        siguienteFila();
        Label etiqueta = new Label(texto);
        etiqueta.getStyleClass().addAll(Styles.TEXT_BOLD, "muted-text");
        etiqueta.setPadding(new Insets(8, 0, 0, 0));
        rejilla.add(etiqueta, 0, fila, columnas, 1);
        fila++;
    }

    public TextField texto(String clave, String etiqueta, String prompt, boolean completo) {
        TextField campo = new TextField();
        campo.setPromptText(prompt);
        agregar(clave, etiqueta, campo, completo);
        return campo;
    }

    public TextArea area(String clave, String etiqueta, int filas) {
        TextArea campo = new TextArea();
        campo.setPrefRowCount(filas);
        campo.setWrapText(true);
        agregar(clave, etiqueta, campo, true);
        return campo;
    }

    public <T> ComboBox<T> combo(String clave, String etiqueta, List<T> opciones, boolean completo) {
        ComboBox<T> campo = new ComboBox<>();
        campo.getItems().setAll(opciones);
        agregar(clave, etiqueta, campo, completo);
        return campo;
    }

    public CheckBox check(String clave, String etiqueta) {
        CheckBox campo = new CheckBox(etiqueta);
        agregar(clave, "", campo, false);
        return campo;
    }

    public void agregar(String clave, String etiqueta, Control control, boolean completo) {
        if (completo || columnas == 1) {
            siguienteFila();
        }
        control.setMaxWidth(Double.MAX_VALUE);
        Label error = new Label();
        error.getStyleClass().add(Styles.DANGER);
        error.setWrapText(true);
        error.setVisible(false);
        error.setManaged(false);

        VBox contenedor = new VBox(4);
        if (!etiqueta.isEmpty()) {
            contenedor.getChildren().add(new Label(etiqueta));
        }
        contenedor.getChildren().addAll(control, error);
        int ancho = completo ? columnas : 1;
        rejilla.add(contenedor, columna, fila, ancho, 1);
        errores.put(clave, error);
        controles.put(clave, control);

        columna += ancho;
        if (columna >= columnas) {
            columna = 0;
            fila++;
        }
    }

    public void limpiarErrores() {
        errores.values().forEach(error -> ocultar(error));
        controles.values().forEach(control -> control.pseudoClassStateChanged(Styles.STATE_DANGER, false));
        ocultar(errorGeneral);
    }

    /** Muestra el error junto al campo; si la clave no existe, lo muestra como error general. */
    public void mostrarError(String clave, String mensaje) {
        Label error = clave == null ? null : errores.get(clave);
        if (error == null) {
            errorGeneral.setText(mensaje);
            errorGeneral.setVisible(true);
            errorGeneral.setManaged(true);
            return;
        }
        error.setText(mensaje);
        error.setVisible(true);
        error.setManaged(true);
        controles.get(clave).pseudoClassStateChanged(Styles.STATE_DANGER, true);
    }

    private void siguienteFila() {
        if (columna != 0) {
            columna = 0;
            fila++;
        }
    }

    private void ocultar(Label etiqueta) {
        etiqueta.setText("");
        etiqueta.setVisible(false);
        etiqueta.setManaged(false);
    }
}
