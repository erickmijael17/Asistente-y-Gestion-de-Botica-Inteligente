package com.botica.inteligente.ui;

import com.botica.inteligente.asistente.service.AsistenteService;
import javafx.fxml.FXML;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

/** Pantalla completa del asistente; el mismo panel se usa como lateral en el punto de venta. */
@Component
@Scope("prototype")
public class AsistenteController implements Refrescable {

    private final AsistenteService asistenteService;

    @FXML
    private VBox raiz;

    private PanelAsistente panel;

    public AsistenteController(AsistenteService asistenteService) {
        this.asistenteService = asistenteService;
    }

    @FXML
    public void initialize() {
        panel = new PanelAsistente(asistenteService, null, false);
        VBox.setVgrow(panel, Priority.ALWAYS);
        raiz.getChildren().setAll(panel);
    }

    @Override
    public void refrescar() {
        panel.iniciar();
    }
}
