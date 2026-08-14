package com.botica.inteligente.venta.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import java.util.List;

public record VentaCreateRequest(
    @NotEmpty(message = "La venta debe tener al menos un producto")
    @Valid
    List<VentaDetalleRequest> detalles
) {}
