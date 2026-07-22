package com.botica.inteligente.venta.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public record VentaCreateRequest(
    @NotNull(message = "El usuario/vendedor es obligatorio")
    Long usuarioId,

    @NotEmpty(message = "La venta debe tener al menos un producto")
    @Valid
    List<VentaDetalleRequest> detalles
) {}
