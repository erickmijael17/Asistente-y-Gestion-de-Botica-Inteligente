package com.botica.inteligente.shared.util;

import java.math.BigDecimal;
import java.math.RoundingMode;

/** Convierte importes en soles a letras (espanol), por ejemplo para el pie del comprobante. */
public final class NumeroALetras {

    private static final String[] UNIDADES = {
        "", "UNO", "DOS", "TRES", "CUATRO", "CINCO", "SEIS", "SIETE", "OCHO", "NUEVE", "DIEZ", "ONCE", "DOCE",
        "TRECE", "CATORCE", "QUINCE", "DIECISEIS", "DIECISIETE", "DIECIOCHO", "DIECINUEVE", "VEINTE", "VEINTIUNO",
        "VEINTIDOS", "VEINTITRES", "VEINTICUATRO", "VEINTICINCO", "VEINTISEIS", "VEINTISIETE", "VEINTIOCHO",
        "VEINTINUEVE"
    };
    private static final String[] DECENAS = {
        "", "", "", "TREINTA", "CUARENTA", "CINCUENTA", "SESENTA", "SETENTA", "OCHENTA", "NOVENTA"
    };
    private static final String[] CENTENAS = {
        "", "CIENTO", "DOSCIENTOS", "TRESCIENTOS", "CUATROCIENTOS", "QUINIENTOS", "SEISCIENTOS", "SETECIENTOS",
        "OCHOCIENTOS", "NOVECIENTOS"
    };

    private NumeroALetras() {
    }

    /** Ejemplo: 118.00 -> "SON: CIENTO DIECIOCHO CON 00/100 SOLES". */
    public static String soles(BigDecimal monto) {
        BigDecimal redondeado = monto.setScale(2, RoundingMode.HALF_UP);
        long enteros = redondeado.longValue();
        int centimos = redondeado.remainder(BigDecimal.ONE).movePointRight(2).intValue();
        return "SON: " + convertir(enteros) + String.format(" CON %02d/100 SOLES", centimos);
    }

    static String convertir(long numero) {
        if (numero == 0) {
            return "CERO";
        }
        StringBuilder texto = new StringBuilder();
        long millones = numero / 1_000_000;
        long resto = numero % 1_000_000;
        if (millones > 0) {
            texto.append(millones == 1 ? "UN MILLON" : apocope(convertir(millones)) + " MILLONES");
        }
        long miles = resto / 1000;
        resto = resto % 1000;
        if (miles > 0) {
            separar(texto);
            texto.append(miles == 1 ? "MIL" : apocope(menorDeMil((int) miles)) + " MIL");
        }
        if (resto > 0) {
            separar(texto);
            texto.append(menorDeMil((int) resto));
        }
        return texto.toString();
    }

    private static String menorDeMil(int numero) {
        if (numero == 100) {
            return "CIEN";
        }
        int centena = numero / 100;
        int resto = numero % 100;
        StringBuilder texto = new StringBuilder(CENTENAS[centena]);
        if (resto > 0) {
            separar(texto);
            if (resto < 30) {
                texto.append(UNIDADES[resto]);
            } else {
                texto.append(DECENAS[resto / 10]);
                if (resto % 10 > 0) {
                    texto.append(" Y ").append(UNIDADES[resto % 10]);
                }
            }
        }
        return texto.toString();
    }

    /** "UNO" -> "UN" y "VEINTIUNO" -> "VEINTIUN" cuando preceden a MIL o MILLONES. */
    private static String apocope(String texto) {
        if (texto.endsWith("UNO")) {
            return texto.substring(0, texto.length() - 1);
        }
        return texto;
    }

    private static void separar(StringBuilder texto) {
        if (!texto.isEmpty()) {
            texto.append(' ');
        }
    }
}
