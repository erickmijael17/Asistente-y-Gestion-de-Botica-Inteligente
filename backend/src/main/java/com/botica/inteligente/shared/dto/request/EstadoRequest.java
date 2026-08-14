package com.botica.inteligente.shared.dto.request;

import jakarta.validation.constraints.NotNull;

public record EstadoRequest(@NotNull(message = "El estado es obligatorio") Boolean estado) {
}