package com.botica.inteligente.laboratorio.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LaboratorioUpdateRequest(
        @NotBlank(message = "El nombre es obligatorio")
        @Size(max = 150, message = "El nombre no debe superar 150 caracteres")
        String nombre,
        @Size(max = 20, message = "El RUC no debe superar 20 caracteres")
        String ruc,
        @Size(max = 30, message = "El telefono no debe superar 30 caracteres")
        String telefono,
        @Email(message = "El correo no tiene un formato valido")
        @Size(max = 150, message = "El correo no debe superar 150 caracteres")
        String correo,
        @Size(max = 250, message = "La direccion no debe superar 250 caracteres")
        String direccion,
        @Size(max = 150, message = "El sitio web no debe superar 150 caracteres")
        String sitioWeb
) {
}
