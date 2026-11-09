package com.botica.inteligente.shared.i18n;

import java.text.MessageFormat;
import java.util.Locale;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicReference;

public final class Mensajes {

    private static final String BASE_NOMBRE = "i18n.mensajes";
    private static final AtomicReference<Idioma> IDIOMA_ACTUAL = new AtomicReference<>(Idioma.ES);
    private static final AtomicReference<ResourceBundle> BUNDLE_ACTUAL =
        new AtomicReference<>(ResourceBundle.getBundle(BASE_NOMBRE, Idioma.ES.locale()));

    private Mensajes() {
    }

    /**
     * Tambien cambia el Locale por defecto de la JVM: los botones estandar de JavaFX
     * (ButtonType.CANCEL, CLOSE, YES, NO, etc.) toman su texto de ahi, no de este bundle.
     */
    public static void establecerIdioma(Idioma idioma) {
        IDIOMA_ACTUAL.set(idioma);
        BUNDLE_ACTUAL.set(ResourceBundle.getBundle(BASE_NOMBRE, idioma.locale()));
        Locale.setDefault(idioma.locale());
    }

    public static Idioma idiomaActual() {
        return IDIOMA_ACTUAL.get();
    }

    public static ResourceBundle bundleActual() {
        return BUNDLE_ACTUAL.get();
    }

    public static String get(String clave) {
        return BUNDLE_ACTUAL.get().getString(clave);
    }

    public static String get(String clave, Object... argumentos) {
        return MessageFormat.format(get(clave), argumentos);
    }
}
