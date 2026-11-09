package com.botica.inteligente.ui;

import com.botica.inteligente.producto.informacion.BusquedaWebService;
import com.botica.inteligente.producto.informacion.FichaWeb;
import com.botica.inteligente.producto.informacion.ResultadoBusquedaWeb;
import com.botica.inteligente.shared.i18n.Mensajes;
import java.net.URI;
import java.util.function.Consumer;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ButtonType;
import javafx.scene.control.Dialog;
import javafx.scene.control.Label;
import javafx.scene.control.ListCell;
import javafx.scene.control.ListView;
import javafx.scene.control.TextField;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Priority;
import javafx.scene.layout.VBox;
import javafx.stage.Window;

/**
 * Busca en la web la ficha de un medicamento (scraping heuristico, sin API oficial) y deja que el
 * farmaceutico elija la pagina y aplique el resultado; el formulario de producto sigue siendo
 * editable despues, asi que esto es solo un punto de partida, no un dato definitivo.
 */
public final class BusquedaInformacionDialogo {

    private BusquedaInformacionDialogo() {
    }

    public static void abrir(
        Window propietario, String consultaInicial, BusquedaWebService servicio, Consumer<FichaWeb> alAplicar
    ) {
        Dialog<Void> dialogo = new Dialog<>();
        dialogo.initOwner(propietario);
        dialogo.setTitle(Mensajes.get("producto.busquedaWeb.titulo"));
        dialogo.setHeaderText(Mensajes.get("producto.busquedaWeb.titulo"));
        dialogo.getDialogPane().getButtonTypes().add(ButtonType.CLOSE);

        TextField consulta = new TextField(consultaInicial == null ? "" : consultaInicial);
        consulta.setPromptText(Mensajes.get("producto.busquedaWeb.consultaPrompt"));
        HBox.setHgrow(consulta, Priority.ALWAYS);
        Button buscarButton = new Button(Mensajes.get("producto.busquedaWeb.buscar"));
        HBox barraBusqueda = new HBox(8, consulta, buscarButton);
        barraBusqueda.setAlignment(Pos.CENTER_LEFT);

        Label mensaje = new Label();
        mensaje.getStyleClass().add("muted-text");

        ListView<ResultadoBusquedaWeb> resultados = new ListView<>();
        resultados.setPrefHeight(220);
        resultados.setCellFactory(lista -> new ListCell<>() {
            @Override
            protected void updateItem(ResultadoBusquedaWeb item, boolean vacio) {
                super.updateItem(item, vacio);
                setText(vacio || item == null ? null : item.titulo() + "\n" + dominio(item.url()));
                setWrapText(true);
            }
        });

        Button usarButton = new Button(Mensajes.get("producto.busquedaWeb.usarResultado"));
        usarButton.setDisable(true);
        resultados.getSelectionModel().selectedItemProperty()
            .addListener((obs, anterior, nuevo) -> usarButton.setDisable(nuevo == null));

        buscarButton.setOnAction(evento -> {
            String texto = consulta.getText();
            buscarButton.setDisable(true);
            resultados.getItems().clear();
            mensaje.setText(Mensajes.get("producto.busquedaWeb.buscando"));
            Fondo.ejecutar(
                () -> servicio.buscar(texto),
                encontrados -> {
                    buscarButton.setDisable(false);
                    resultados.getItems().setAll(encontrados);
                    mensaje.setText(encontrados.isEmpty()
                        ? Mensajes.get("producto.busquedaWeb.sinResultados")
                        : "");
                },
                error -> {
                    buscarButton.setDisable(false);
                    mensaje.setText(Mensajes.get("producto.busquedaWeb.sinResultados"));
                }
            );
        });

        usarButton.setOnAction(evento -> {
            ResultadoBusquedaWeb elegido = resultados.getSelectionModel().getSelectedItem();
            if (elegido == null) {
                return;
            }
            usarButton.setDisable(true);
            mensaje.setText(Mensajes.get("producto.busquedaWeb.extrayendo"));
            Fondo.ejecutar(
                () -> servicio.extraerFicha(elegido.url()),
                ficha -> {
                    alAplicar.accept(ficha);
                    mensaje.setText(ficha.vacia()
                        ? Mensajes.get("producto.busquedaWeb.sinCoincidencias")
                        : Mensajes.get("producto.busquedaWeb.aplicada"));
                    usarButton.setDisable(false);
                },
                error -> {
                    mensaje.setText(Mensajes.get("producto.busquedaWeb.sinResultados"));
                    usarButton.setDisable(false);
                }
            );
        });

        VBox contenido = new VBox(10, barraBusqueda, resultados, usarButton, mensaje);
        contenido.setPadding(new Insets(4));
        contenido.setPrefWidth(480);
        dialogo.getDialogPane().setContent(contenido);
        dialogo.show();

        if (consultaInicial != null && !consultaInicial.isBlank()) {
            buscarButton.fire();
        }
    }

    private static String dominio(String url) {
        try {
            String host = URI.create(url).getHost();
            return host == null ? url : host;
        } catch (RuntimeException excepcion) {
            return url;
        }
    }
}
