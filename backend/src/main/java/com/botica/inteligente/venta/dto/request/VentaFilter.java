package com.botica.inteligente.venta.dto.request;

import com.botica.inteligente.venta.enums.EstadoVenta;
import java.time.LocalDate;

public record VentaFilter(
    Long usuarioId,
    EstadoVenta estado,
    LocalDate fechaDesde,
    LocalDate fechaHasta
) {}
