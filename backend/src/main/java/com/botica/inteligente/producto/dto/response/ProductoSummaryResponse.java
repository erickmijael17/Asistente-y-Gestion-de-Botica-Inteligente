package com.botica.inteligente.producto.dto.response;

import com.botica.inteligente.producto.enums.TipoProducto;
import java.math.BigDecimal;

public record ProductoSummaryResponse(
        Long id,
        String codigoInterno,
        String codigoBarras,
        String nombreComercial,
        TipoProducto tipoProducto,
        BigDecimal precioVenta,
        Boolean estado,
        Long categoriaId,
        String categoriaNombre,
        Long laboratorioId,
        String laboratorioNombre
) {
}
