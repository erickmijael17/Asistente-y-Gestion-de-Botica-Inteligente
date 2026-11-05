package com.botica.inteligente;

import atlantafx.base.theme.PrimerLight;
import java.io.IOException;
import lombok.extern.slf4j.Slf4j;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.context.ConfigurableApplicationContext;

@Slf4j
public class JavaFxApplication extends Application {

    private ConfigurableApplicationContext context;

    @Override
    public void init() {
        context = new SpringApplicationBuilder(BoticaApplication.class)
            .web(WebApplicationType.NONE)
            .headless(false)
            .run(getParameters().getRaw().toArray(String[]::new));
        log.info("Contexto de Botica Inteligente iniciado");
    }

    @Override
    public void start(Stage stage) throws IOException {
        Application.setUserAgentStylesheet(new PrimerLight().getUserAgentStylesheet());

        FXMLLoader loader = new FXMLLoader(
            JavaFxApplication.class.getResource("/view/main-view.fxml")
        );
        loader.setControllerFactory(context::getBean);

        Scene scene = new Scene(loader.load(), 1180, 760);
        scene.getStylesheets().add(
            JavaFxApplication.class.getResource("/css/app.css").toExternalForm()
        );

        stage.setTitle("Botica Inteligente");
        stage.setMinWidth(960);
        stage.setMinHeight(640);
        stage.setScene(scene);
        stage.show();
    }

    @Override
    public void stop() {
        if (context != null) {
            context.close();
            log.info("Contexto de Botica Inteligente cerrado");
        }
    }
}
