package com.botica.inteligente.compra.service;

import com.botica.inteligente.compra.enums.EstadoCompra;
import com.botica.inteligente.compra.enums.TipoDocumentoCompra;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public record CompraResumen(
    Long id,
    String codigo,
    LocalDateTime fecha,
    String proveedor,
    String ruc,
    TipoDocumentoCompra tipoDocumento,
    String numeroDocumento,
    LocalDate fechaDocumento,
    BigDecimal baseImponible,
    BigDecimal igv,
    BigDecimal total,
    EstadoCompra estado,
    List<Detalle> detalles
) {

    public record Detalle(
        String codigoProducto, String nombreProducto, int cantidad, BigDecimal costoUnitario,
        BigDecimal importe, String codigoLote, LocalDate fechaVencimiento
    ) {
    }
}
