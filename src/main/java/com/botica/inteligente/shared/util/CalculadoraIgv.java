package com.botica.inteligente.shared.util;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Collection;

/** Calculos de IGV compartidos por compras y ventas. La tasa se recibe como parametro. */
public final class CalculadoraIgv {

    private CalculadoraIgv() {
    }

    public record Totales(BigDecimal baseImponible, BigDecimal igv, BigDecimal total, BigDecimal tasa) {
    }

    /** Importe de una linea: cantidad x precio, redondeado a 2 decimales (HALF_UP). */
    public static BigDecimal importe(int cantidad, BigDecimal precio) {
        return precio.multiply(BigDecimal.valueOf(cantidad)).setScale(2, RoundingMode.HALF_UP);
    }

    public static Totales calcular(
        Collection<BigDecimal> importes, boolean aplicaIgv, boolean preciosIncluyenIgv, BigDecimal tasa
    ) {
        BigDecimal suma = importes.stream().reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
        if (!aplicaIgv) {
            return new Totales(suma, BigDecimal.ZERO.setScale(2), suma, BigDecimal.ZERO);
        }
        if (preciosIncluyenIgv) {
            BigDecimal base = suma.divide(BigDecimal.ONE.add(tasa), 2, RoundingMode.HALF_UP);
            return new Totales(base, suma.subtract(base), suma, tasa);
        }
        BigDecimal igv = suma.multiply(tasa).setScale(2, RoundingMode.HALF_UP);
        return new Totales(suma, igv, suma.add(igv), tasa);
    }

    /** Valor unitario sin IGV (4 decimales), para costear inventario. */
    public static BigDecimal sinIgv(BigDecimal valor, boolean aplicaIgv, boolean preciosIncluyenIgv, BigDecimal tasa) {
        if (aplicaIgv && preciosIncluyenIgv) {
            return valor.divide(BigDecimal.ONE.add(tasa), 4, RoundingMode.HALF_UP);
        }
        return valor.setScale(4, RoundingMode.HALF_UP);
    }
}
