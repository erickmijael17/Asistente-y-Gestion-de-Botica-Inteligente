package com.botica.inteligente.inventario.enums;

import com.botica.inteligente.shared.i18n.Mensajes;

public enum TipoMovimiento {
    COMPRA(true, "tipoMovimiento.compra"),
    ANULACION_VENTA(true, "tipoMovimiento.anulacionVenta"),
    AJUSTE_ENTRADA(true, "tipoMovimiento.ajusteEntrada"),
    VENTA(false, "tipoMovimiento.venta"),
    ANULACION_COMPRA(false, "tipoMovimiento.anulacionCompra"),
    MERMA(false, "tipoMovimiento.merma");

    private final boolean entrada;
    private final String clave;

    TipoMovimiento(boolean entrada, String clave) {
        this.entrada = entrada;
        this.clave = clave;
    }

    public boolean esEntrada() {
        return entrada;
    }

    public String etiqueta() {
        return Mensajes.get(clave);
    }
}
