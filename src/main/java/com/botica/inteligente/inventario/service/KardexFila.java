package com.botica.inteligente.inventario.service;

import com.botica.inteligente.inventario.enums.TipoMovimiento;
import java.math.BigDecimal;
import java.time.LocalDateTime;

public record KardexFila(
    LocalDateTime fecha,
    TipoMovimiento tipo,
    String lote,
    int entrada,
    int salida,
    int saldo,
    BigDecimal costoUnitario,
    String documento
) {
}
