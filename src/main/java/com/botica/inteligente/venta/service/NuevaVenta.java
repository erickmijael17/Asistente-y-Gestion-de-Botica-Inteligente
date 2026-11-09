package com.botica.inteligente.venta.service;

import com.botica.inteligente.venta.enums.TipoComprobante;
import java.util.List;

public record NuevaVenta(
    TipoComprobante tipoComprobante,
    String clienteNombre,
    String clienteDocumento,
    boolean aplicaIgv,
    boolean preciosIncluyenIgv,
    String observacion,
    List<LineaVenta> lineas
) {
}
