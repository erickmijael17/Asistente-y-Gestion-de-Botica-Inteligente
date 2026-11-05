package com.botica.inteligente.ui;

import java.util.Locale;
import java.util.Set;
import javafx.scene.control.Label;
import javafx.scene.control.TableCell;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.util.Callback;

/** Ayudas para tablas: columnas de estado con indicador de color. */
public final class Tablas {

    private static final Set<String> CORRECTO = Set.of("activo", "activa", "registrada", "emitida", "disponible", "vigente");
    private static final Set<String> AVISO = Set.of("stock bajo", "por vencer");
    private static final Set<String> ERROR = Set.of("anulada", "anulado", "vencido", "sin stock");

    private Tablas() {
    }

    /** Muestra el texto de las columnas indicadas como una etiqueta de color segun su valor. */
    public static <T> void aplicarIndicadores(TableView<T> tabla, String... titulos) {
        for (TableColumn<T, ?> columna : tabla.getColumns()) {
            for (String titulo : titulos) {
                if (titulo.equals(columna.getText())) {
                    @SuppressWarnings("unchecked")
                    TableColumn<T, String> texto = (TableColumn<T, String>) columna;
                    texto.setCellFactory(indicador());
                }
            }
        }
    }

    public static <T> Callback<TableColumn<T, String>, TableCell<T, String>> indicador() {
        return columna -> new TableCell<>() {
            private final Label etiqueta = new Label();

            @Override
            protected void updateItem(String valor, boolean vacio) {
                super.updateItem(valor, vacio);
                if (vacio || valor == null) {
                    setGraphic(null);
                    return;
                }
                etiqueta.setText(valor);
                etiqueta.getStyleClass().setAll("chip", clase(valor));
                setGraphic(etiqueta);
            }
        };
    }

    static String clase(String valor) {
        String clave = valor.toLowerCase(Locale.ROOT);
        if (CORRECTO.contains(clave)) {
            return "chip-ok";
        }
        if (AVISO.contains(clave)) {
            return "chip-warn";
        }
        return ERROR.contains(clave) ? "chip-bad" : "chip-off";
    }
}
