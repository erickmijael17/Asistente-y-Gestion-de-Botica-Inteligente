package com.botica.inteligente.venta.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

public record VentaDetalleRequest(
    @NotNull(message = "El producto es obligatorio")
    Long productoId,

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor a cero")
    Integer cantidad,

    @NotNull(message = "El precio unitario es obligatorio")
    @Min(value = 0, message = "El precio unitario no puede ser negativo")
    BigDecimal precioUnitario
) {}
