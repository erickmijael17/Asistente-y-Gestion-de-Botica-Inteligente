package com.botica.inteligente.categoria.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CategoriaCreateRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 100, message = "El nombre no debe superar 100 caracteres")
        String nombre,
        @Size(max = 500, message = "La descripcion no debe superar 500 caracteres")
        String descripcion
) {
}
