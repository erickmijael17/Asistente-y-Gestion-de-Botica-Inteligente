package com.botica.inteligente.dashboard.service;

import java.math.BigDecimal;

public record ProductoVendido(Long productoId, String nombre, Long unidades, BigDecimal importe) {
}
