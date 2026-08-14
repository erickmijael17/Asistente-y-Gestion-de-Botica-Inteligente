package com.botica.inteligente.venta.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record VentaDetalleRequest(
    @NotNull(message = "El producto es obligatorio")
    Long productoId,

    @NotNull(message = "La cantidad es obligatoria")
    @Min(value = 1, message = "La cantidad debe ser mayor a cero")
    Integer cantidad
) {}
