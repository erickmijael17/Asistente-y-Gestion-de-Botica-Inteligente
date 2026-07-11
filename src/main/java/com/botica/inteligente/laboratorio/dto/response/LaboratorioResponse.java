package com.botica.inteligente.laboratorio.dto.response;

import java.time.LocalDateTime;

public record LaboratorioResponse(
        Long id,
        String nombre,
        String ruc,
        String telefono,
        String correo,
        String direccion,
        String sitioWeb,
        Boolean estado,
        LocalDateTime fechaCreacion,
        LocalDateTime fechaActualizacion
) {
}
