package com.botica.inteligente.producto.dto.request;

import jakarta.validation.constraints.NotNull;

public record EstadoRequest(@NotNull(message = "El estado es obligatorio") Boolean estado) {
}
