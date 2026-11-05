package com.botica.inteligente.shared.util;

public final class Ean13 {

    private static final int LONGITUD = 13;

    private Ean13() {
    }

    /** Valida longitud de 13 digitos y digito verificador. */
    public static boolean esValido(String codigo) {
        if (codigo == null || codigo.length() != LONGITUD) {
            return false;
        }
        int suma = 0;
        for (int i = 0; i < LONGITUD; i++) {
            char caracter = codigo.charAt(i);
            if (caracter < '0' || caracter > '9') {
                return false;
            }
            if (i < LONGITUD - 1) {
                int digito = caracter - '0';
                suma += i % 2 == 0 ? digito : digito * 3;
            }
        }
        int verificador = (10 - suma % 10) % 10;
        return verificador == codigo.charAt(LONGITUD - 1) - '0';
    }
}
