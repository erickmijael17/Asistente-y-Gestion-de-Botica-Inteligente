package com.botica.inteligente.ui;

import javafx.fxml.FXML;
import javafx.scene.control.Label;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

@Component
@Scope("prototype")
public class PlaceholderController implements Refrescable {

    @FXML
    private Label estadoVista;

    @Override
    public void refrescar() {
        if (estadoVista != null) {
            estadoVista.setText("Vista preparada para una fase posterior");
        }
    }
}
