package com.botica.inteligente.producto.dto.response;

import com.botica.inteligente.producto.enums.TipoProducto;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public record ProductoResponse(
        Long id,
        String codigoInterno,
        String codigoBarras,
        String nombreComercial,
        String nombreGenerico,
        String descripcion,
        TipoProducto tipoProducto,
        String principioActivo,
        String concentracion,
        String presentacion,
        String viaAdministracion,
        String indicacionesOficiales,
        String contraindicaciones,
        String advertencias,
        String precauciones,
        String condicionesAlmacenamiento,
        Boolean requiereReceta,
        String registroSanitario,
        BigDecimal precioCompra,
        BigDecimal precioVenta,
        Integer stockMinimo,
        String imagenUrl,
        Boolean estado,
        Long categoriaId,
        String categoriaNombre,
        Long laboratorioId,
        String laboratorioNombre,
        String fuenteInformacion,
        LocalDate fechaActualizacionInformacion,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}
