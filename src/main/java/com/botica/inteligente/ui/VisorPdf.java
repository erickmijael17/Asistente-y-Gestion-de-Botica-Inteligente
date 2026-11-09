package com.botica.inteligente.ui;

import com.botica.inteligente.shared.exception.NegocioException;
import com.botica.inteligente.shared.i18n.Mensajes;
import java.awt.Desktop;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.Callable;
import java.util.function.Consumer;

/** Guarda un PDF en un archivo temporal y lo abre con el visor del sistema, fuera del hilo de JavaFX. */
public final class VisorPdf {

    private VisorPdf() {
    }

    public static void abrir(String nombre, Callable<byte[]> generador, Consumer<String> alFallar) {
        Fondo.ejecutar(() -> {
            byte[] contenido = generador.call();
            Path archivo = guardar(nombre, contenido);
            if (!Desktop.isDesktopSupported() || !Desktop.getDesktop().isSupported(Desktop.Action.OPEN)) {
                throw new NegocioException(Mensajes.get("error.pdf.sinVisor", archivo.toString()));
            }
            Desktop.getDesktop().open(archivo.toFile());
            return archivo;
        }, archivo -> { }, error -> alFallar.accept(DialogoFormulario.textoError(error)));
    }

    private static Path guardar(String nombre, byte[] contenido) throws IOException {
        String limpio = nombre.replaceAll("[^A-Za-z0-9-]", "_");
        Path archivo = Files.createTempFile("comprobante-" + limpio + "-", ".pdf");
        Files.write(archivo, contenido);
        archivo.toFile().deleteOnExit();
        return archivo;
    }
}
