package com.botica.inteligente.producto.service;

import com.botica.inteligente.producto.enums.TipoProducto;
import java.math.BigDecimal;

public record ProductoComando(
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
    boolean requiereReceta,
    String registroSanitario,
    BigDecimal precioCompra,
    BigDecimal precioVenta,
    Integer stockMinimo,
    Long categoriaId,
    Long laboratorioId,
    String fuenteInformacion
) {
}
