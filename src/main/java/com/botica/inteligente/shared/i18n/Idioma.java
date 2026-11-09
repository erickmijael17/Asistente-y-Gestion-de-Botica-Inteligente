package com.botica.inteligente.shared.i18n;

import java.util.Locale;

public enum Idioma {
    ES("es", "Español"),
    EN("en", "English");

    private final String codigo;
    private final String etiqueta;

    Idioma(String codigo, String etiqueta) {
        this.codigo = codigo;
        this.etiqueta = etiqueta;
    }

    public Locale locale() {
        return Locale.forLanguageTag(codigo);
    }

    public String codigo() {
        return codigo;
    }

    @Override
    public String toString() {
        return etiqueta;
    }

    public static Idioma desdeCodigo(String codigo) {
        for (Idioma idioma : values()) {
            if (idioma.codigo.equalsIgnoreCase(codigo)) {
                return idioma;
            }
        }
        return ES;
    }
}
