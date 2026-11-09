package com.botica.inteligente.inventario.service;

import java.time.LocalDate;

public record LoteResumen(
    Long id,
    Long productoId,
    String producto,
    String codigoLote,
    LocalDate fechaVencimiento,
    int cantidadActual,
    boolean vencido,
    long diasParaVencer
) {
}
