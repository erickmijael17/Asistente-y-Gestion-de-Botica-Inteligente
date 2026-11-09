package com.botica.inteligente.dashboard.service;

import com.botica.inteligente.inventario.service.InventarioFila;
import com.botica.inteligente.inventario.service.LoteResumen;
import java.util.List;

public record Alertas(
    int productosConStockBajo,
    int productosSinStock,
    int lotesPorVencer,
    int lotesVencidos,
    List<InventarioFila> stock,
    List<LoteResumen> vencimientos
) {
}
