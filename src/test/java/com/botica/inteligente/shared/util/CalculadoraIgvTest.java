package com.botica.inteligente.shared.util;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;

class CalculadoraIgvTest {

    private static final BigDecimal TASA = new BigDecimal("0.18");

    @Test
    void preciosSinIgvSumaElImpuestoAlTotal() {
        CalculadoraIgv.Totales t = CalculadoraIgv.calcular(List.of(new BigDecimal("100.00")), true, false, TASA);

        assertEquals(new BigDecimal("100.00"), t.baseImponible());
        assertEquals(new BigDecimal("18.00"), t.igv());
        assertEquals(new BigDecimal("118.00"), t.total());
    }

    @Test
    void preciosConIgvDescomponenLaBase() {
        CalculadoraIgv.Totales t = CalculadoraIgv.calcular(List.of(new BigDecimal("118.00")), true, true, TASA);

        assertEquals(new BigDecimal("100.00"), t.baseImponible());
        assertEquals(new BigDecimal("18.00"), t.igv());
        assertEquals(new BigDecimal("118.00"), t.total());
    }

    @Test
    void sinIgvLaBaseEsElTotal() {
        CalculadoraIgv.Totales t = CalculadoraIgv.calcular(List.of(new BigDecimal("50.00")), false, false, TASA);

        assertEquals(new BigDecimal("50.00"), t.total());
        assertEquals(0, t.igv().signum());
        assertEquals(0, t.tasa().signum());
    }

    @Test
    void importeRedondeaHalfUpADosDecimales() {
        assertEquals(new BigDecimal("3.34"), CalculadoraIgv.importe(3, new BigDecimal("1.1133")));
    }

    @Test
    void costoSinIgvSoloDivideCuandoLosPreciosIncluyenIgv() {
        assertEquals(new BigDecimal("10.0000"), CalculadoraIgv.sinIgv(new BigDecimal("11.80"), true, true, TASA));
        assertEquals(new BigDecimal("11.8000"), CalculadoraIgv.sinIgv(new BigDecimal("11.80"), true, false, TASA));
        assertEquals(new BigDecimal("11.8000"), CalculadoraIgv.sinIgv(new BigDecimal("11.80"), false, true, TASA));
    }
}
