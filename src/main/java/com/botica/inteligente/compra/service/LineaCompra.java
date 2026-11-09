package com.botica.inteligente.compra.service;

import java.math.BigDecimal;
import java.time.LocalDate;

public record LineaCompra(
    Long productoId,
    int cantidad,
    BigDecimal costoUnitario,
    String codigoLote,
    LocalDate fechaVencimiento
) {
}
