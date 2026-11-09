package com.botica.inteligente.producto.service;

import com.botica.inteligente.producto.enums.TipoProducto;

public record ProductoFiltro(
    String texto,
    Boolean estado,
    Long categoriaId,
    Long laboratorioId,
    TipoProducto tipoProducto
) {

    public static ProductoFiltro vacio() {
        return new ProductoFiltro(null, null, null, null, null);
    }
}
