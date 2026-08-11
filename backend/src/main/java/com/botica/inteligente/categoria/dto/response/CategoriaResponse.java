package com.botica.inteligente.categoria.dto.response;

import java.time.LocalDateTime;

public record CategoriaResponse(
        Long id,
        String nombre,
        String descripcion,
        Boolean estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}
