package com.botica.inteligente.categoria.dto.request;

import jakarta.validation.constraints.NotNull;

public record EstadoRequest(@NotNull(message = "El estado es obligatorio") Boolean estado) {
}
