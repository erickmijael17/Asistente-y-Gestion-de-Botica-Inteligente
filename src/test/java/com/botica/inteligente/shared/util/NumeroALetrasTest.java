package com.botica.inteligente.shared.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class NumeroALetrasTest {

    @Test
    void conviertePesosComunes() {
        assertEquals("SON: CIENTO DIECIOCHO CON 00/100 SOLES", NumeroALetras.soles(new BigDecimal("118.00")));
        assertEquals("SON: CERO CON 50/100 SOLES", NumeroALetras.soles(new BigDecimal("0.50")));
        assertEquals("SON: CIEN CON 00/100 SOLES", NumeroALetras.soles(new BigDecimal("100")));
        assertEquals("SON: VEINTIUNO CON 05/100 SOLES", NumeroALetras.soles(new BigDecimal("21.05")));
        assertEquals("SON: TREINTA Y CINCO CON 99/100 SOLES", NumeroALetras.soles(new BigDecimal("35.99")));
    }

    @Test
    void convierteMilesYMillonesConApocope() {
        assertEquals("MIL", NumeroALetras.convertir(1000));
        assertEquals("VEINTIUN MIL", NumeroALetras.convertir(21_000));
        assertEquals("DOS MIL QUINIENTOS", NumeroALetras.convertir(2_500));
        assertEquals("UN MILLON", NumeroALetras.convertir(1_000_000));
        assertEquals("DOS MILLONES CIENTO UN MIL UNO", NumeroALetras.convertir(2_101_001));
    }

    @Test
    void redondeaCentimosAlMasCercano() {
        assertEquals("SON: UNO CON 01/100 SOLES", NumeroALetras.soles(new BigDecimal("1.005")));
    }
}
