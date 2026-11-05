package com.botica.inteligente.producto.service;

/** Resultado de guardar un producto; la advertencia (si existe) no bloquea el guardado. */
public record ProductoResultado(ProductoResumen producto, String advertencia) {
}
