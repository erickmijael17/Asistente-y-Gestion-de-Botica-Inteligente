package com.botica.inteligente.venta.dto.response;

import com.botica.inteligente.venta.enums.EstadoVenta;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record VentaResponse(
    Long id,
    Long usuarioId,
    String usuarioUsername,
    LocalDateTime fechaVenta,
    BigDecimal subtotal,
    BigDecimal impuestos,
    BigDecimal total,
    EstadoVenta estado,
    List<VentaDetalleResponse> detalles
) {}
