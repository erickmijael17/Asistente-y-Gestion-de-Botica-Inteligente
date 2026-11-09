package com.botica.inteligente.dashboard.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public record ResumenVentas(
    Periodo periodo,
    LocalDate desde,
    LocalDate hasta,
    BigDecimal total,
    long cantidadVentas,
    BigDecimal ticketPromedio,
    BigDecimal utilidadBruta,
    BigDecimal totalPeriodoAnterior,
    /** Variacion porcentual contra el periodo anterior; null si no hay base de comparacion. */
    BigDecimal variacionPorcentual,
    List<PuntoSerie> serie,
    List<ProductoVendido> masVendidos
) {

    public record PuntoSerie(String etiqueta, BigDecimal total) {
    }
}
