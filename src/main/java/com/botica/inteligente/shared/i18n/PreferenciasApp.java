package com.botica.inteligente.shared.i18n;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Properties;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public final class PreferenciasApp {

    private static final String CLAVE_IDIOMA = "idioma";
    private static final Path ARCHIVO = Path.of(
        System.getProperty("user.home"), ".botica-inteligente", "app-settings.properties"
    );

    private PreferenciasApp() {
    }

    public static Idioma obtenerIdioma() {
        Properties propiedades = cargar();
        return Idioma.desdeCodigo(propiedades.getProperty(CLAVE_IDIOMA, Idioma.ES.codigo()));
    }

    public static void guardarIdioma(Idioma idioma) {
        Properties propiedades = cargar();
        propiedades.setProperty(CLAVE_IDIOMA, idioma.codigo());
        guardar(propiedades);
    }

    private static Properties cargar() {
        Properties propiedades = new Properties();
        if (Files.exists(ARCHIVO)) {
            try (InputStream entrada = Files.newInputStream(ARCHIVO)) {
                propiedades.load(entrada);
            } catch (IOException excepcion) {
                log.warn("No fue posible leer las preferencias locales en {}", ARCHIVO, excepcion);
            }
        }
        return propiedades;
    }

    private static void guardar(Properties propiedades) {
        try {
            Files.createDirectories(ARCHIVO.getParent());
            try (OutputStream salida = Files.newOutputStream(ARCHIVO)) {
                propiedades.store(salida, "Preferencias locales de Botica Inteligente");
            }
        } catch (IOException excepcion) {
            log.warn("No fue posible guardar las preferencias locales en {}", ARCHIVO, excepcion);
        }
    }
}
