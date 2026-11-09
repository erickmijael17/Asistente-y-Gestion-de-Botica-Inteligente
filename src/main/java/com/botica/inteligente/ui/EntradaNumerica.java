package com.botica.inteligente.ui;

import com.botica.inteligente.shared.exception.CampoInvalidoException;
import com.botica.inteligente.shared.i18n.Mensajes;
import java.math.BigDecimal;

/** Lectura de numeros desde campos de texto, con error asociado al campo si el formato es invalido. */
public final class EntradaNumerica {

    private EntradaNumerica() {
    }

    /** Devuelve null si el texto esta vacio; acepta coma o punto decimal. */
    public static BigDecimal decimal(String campo, String texto) {
        String limpio = texto == null ? "" : texto.trim().replace(',', '.');
        if (limpio.isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(limpio);
        } catch (NumberFormatException error) {
            throw new CampoInvalidoException(campo, Mensajes.get("error.numeroInvalido"));
        }
    }

    /** Devuelve null si el texto esta vacio. */
    public static Integer entero(String campo, String texto) {
        String limpio = texto == null ? "" : texto.trim();
        if (limpio.isEmpty()) {
            return null;
        }
        try {
            return Integer.valueOf(limpio);
        } catch (NumberFormatException error) {
            throw new CampoInvalidoException(campo, Mensajes.get("inventario.error.numeroEntero"));
        }
    }
}
