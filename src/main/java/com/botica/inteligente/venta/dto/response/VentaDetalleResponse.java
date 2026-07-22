package com.botica.inteligente.venta.dto.response;

import java.math.BigDecimal;

public record VentaDetalleResponse(
    Long id,
    Long productoId,
    String productoNombre,
    Integer cantidad,
    BigDecimal precioUnitario,
    BigDecimal subtotal
) {}
