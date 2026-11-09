package com.botica.inteligente.inventario.service;

import java.time.LocalDateTime;

/** Datos del documento que origina un movimiento de inventario. */
public record MovimientoContexto(
    String documento,
    Long compraId,
    Long ventaId,
    Long usuarioId,
    LocalDateTime fecha
) {

    public static MovimientoContexto ajuste(String documento, Long usuarioId) {
        return new MovimientoContexto(documento, null, null, usuarioId, LocalDateTime.now());
    }
}
