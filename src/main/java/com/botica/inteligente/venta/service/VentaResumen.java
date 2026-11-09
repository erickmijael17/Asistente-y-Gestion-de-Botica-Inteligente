package com.botica.inteligente.venta.service;

import com.botica.inteligente.venta.enums.EstadoVenta;
import com.botica.inteligente.venta.enums.TipoComprobante;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VentaResumen(
    Long id,
    String comprobante,
    TipoComprobante tipoComprobante,
    LocalDateTime fecha,
    String vendedor,
    String clienteNombre,
    String clienteDocumento,
    boolean aplicaIgv,
    boolean preciosIncluyenIgv,
    BigDecimal baseImponible,
    BigDecimal igv,
    BigDecimal total,
    EstadoVenta estado,
    String observacion,
    List<Detalle> detalles
) {

    public record Detalle(
        String codigoProducto, String nombreProducto, int cantidad, BigDecimal precioUnitario, BigDecimal importe
    ) {
    }
}
