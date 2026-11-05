package com.botica.inteligente.inventario.service;

import java.math.BigDecimal;

public record ResumenInventario(
    int productos,
    long unidades,
    BigDecimal valorCosto,
    BigDecimal valorVenta,
    int conStockBajo,
    int sinStock
) {
}
