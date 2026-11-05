package com.botica.inteligente.inventario.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Fila del listado de inventario: producto con su stock agregado de todos los lotes. */
public record InventarioFila(
    Long productoId,
    String codigoInterno,
    String nombreComercial,
    Integer stockMinimo,
    Long stock,
    LocalDate proximoVencimiento,
    BigDecimal costoPromedio,
    BigDecimal precioVenta
) {

    public EstadoStock estadoStock() {
        if (stock <= 0) {
            return EstadoStock.SIN_STOCK;
        }
        return stock <= stockMinimo ? EstadoStock.BAJO : EstadoStock.DISPONIBLE;
    }

    public BigDecimal valorCosto() {
        return costoPromedio == null ? BigDecimal.ZERO : costoPromedio.multiply(BigDecimal.valueOf(stock));
    }

    public BigDecimal valorVenta() {
        return precioVenta.multiply(BigDecimal.valueOf(stock));
    }

    public enum EstadoStock {
        DISPONIBLE, BAJO, SIN_STOCK
    }
}
