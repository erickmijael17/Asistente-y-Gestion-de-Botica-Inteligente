package com.botica.inteligente.shared.util;

import com.botica.inteligente.shared.exception.CampoInvalidoException;
import com.botica.inteligente.shared.i18n.Mensajes;

public final class Textos {

    private Textos() {
    }

    /** Devuelve el texto sin espacios laterales, o null si queda vacio. */
    public static String limpiar(String texto) {
        if (texto == null) {
            return null;
        }
        String limpio = texto.trim();
        return limpio.isEmpty() ? null : limpio;
    }

    public static String requerido(String campo, String etiqueta, String texto, int maximo) {
        String limpio = limpiar(texto);
        if (limpio == null) {
            throw new CampoInvalidoException(campo, Mensajes.get("validacion.requerido", etiqueta));
        }
        return maximo(campo, etiqueta, limpio, maximo);
    }

    public static String opcional(String campo, String etiqueta, String texto, int maximo) {
        return maximo(campo, etiqueta, limpiar(texto), maximo);
    }

    private static String maximo(String campo, String etiqueta, String texto, int maximo) {
        if (texto != null && texto.length() > maximo) {
            throw new CampoInvalidoException(
                campo, Mensajes.get("validacion.maximoCaracteres", etiqueta, String.valueOf(maximo))
            );
        }
        return texto;
    }

    /** Texto en minusculas para busquedas LIKE; null si esta vacio. */
    public static String patronBusqueda(String texto) {
        String limpio = limpiar(texto);
        return limpio == null ? null : "%" + limpio.toLowerCase() + "%";
    }
}
