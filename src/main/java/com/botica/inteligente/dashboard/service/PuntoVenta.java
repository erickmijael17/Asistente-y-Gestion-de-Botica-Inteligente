package com.botica.inteligente.dashboard.service;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/** Venta emitida reducida a fecha e importe, para armar series por dia o mes. */
public record PuntoVenta(LocalDateTime fecha, BigDecimal total) {
}
