package com.botica.inteligente.ui;

import com.botica.inteligente.shared.i18n.Mensajes;
import java.util.function.IntConsumer;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.Region;
import org.springframework.data.domain.Page;

/** Barra de paginacion para resultados de Spring Data. */
public class Paginador extends HBox {

    private final Button anterior = new Button("‹ " + Mensajes.get("paginador.anterior"));
    private final Button siguiente = new Button(Mensajes.get("paginador.siguiente") + " ›");
    private final Label informacion = new Label();
    private int pagina;

    public Paginador(IntConsumer alCambiarPagina) {
        super(10);
        setAlignment(Pos.CENTER_LEFT);
        Region espacio = new Region();
        HBox.setHgrow(espacio, Priority.ALWAYS);
        informacion.getStyleClass().add("muted-text");
        anterior.setOnAction(evento -> alCambiarPagina.accept(pagina - 1));
        siguiente.setOnAction(evento -> alCambiarPagina.accept(pagina + 1));
        getChildren().addAll(informacion, espacio, anterior, siguiente);
        vaciar();
    }

    public void actualizar(Page<?> resultado) {
        pagina = resultado.getNumber();
        int totalPaginas = Math.max(resultado.getTotalPages(), 1);
        informacion.setText(Mensajes.get(
            "paginador.resumen",
            String.valueOf(resultado.getTotalElements()),
            String.valueOf(pagina + 1),
            String.valueOf(totalPaginas)
        ));
        anterior.setDisable(!resultado.hasPrevious());
        siguiente.setDisable(!resultado.hasNext());
    }

    public void vaciar() {
        pagina = 0;
        informacion.setText("");
        anterior.setDisable(true);
        siguiente.setDisable(true);
    }
}
