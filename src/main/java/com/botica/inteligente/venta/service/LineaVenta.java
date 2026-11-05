package com.botica.inteligente.venta.service;

import java.math.BigDecimal;

/** Linea de una venta nueva. Si el precio es null se usa el precio de venta del catalogo. */
public record LineaVenta(Long productoId, int cantidad, BigDecimal precioUnitario) {
}
