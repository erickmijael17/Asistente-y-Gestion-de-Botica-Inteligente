package com.botica.inteligente.shared.util;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class Ean13Test {

    @Test
    void aceptaCodigoConDigitoVerificadorCorrecto() {
        assertTrue(Ean13.esValido("4006381333931"));
    }

    @Test
    void rechazaDigitoVerificadorIncorrecto() {
        assertFalse(Ean13.esValido("4006381333932"));
    }

    @Test
    void rechazaLongitudIncorrectaNulosYCaracteresNoNumericos() {
        assertFalse(Ean13.esValido(null));
        assertFalse(Ean13.esValido("400638133393"));
        assertFalse(Ean13.esValido("40063813339311"));
        assertFalse(Ean13.esValido("40063813339A1"));
    }
}
