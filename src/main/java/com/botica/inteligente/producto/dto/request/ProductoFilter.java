package com.botica.inteligente.producto.dto.request;

import com.botica.inteligente.producto.enums.TipoProducto;

public record ProductoFilter(
        String codigoInterno,
        String codigoBarras,
        String nombreComercial,
        String nombreGenerico,
        String principioActivo,
        TipoProducto tipoProducto,
        Long categoriaId,
        Long laboratorioId,
        Boolean requiereReceta,
        Boolean estado
) {
}
