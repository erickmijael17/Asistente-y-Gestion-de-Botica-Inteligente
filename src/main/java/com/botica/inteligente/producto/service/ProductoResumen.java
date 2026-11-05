package com.botica.inteligente.producto.service;

public record ProductoResumen(
    Long id,
    ProductoComando datos,
    String categoriaNombre,
    String laboratorioNombre,
    boolean estado
) {

    @Override
    public String toString() {
        return datos.nombreComercial();
    }
}
