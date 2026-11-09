package com.botica.inteligente.venta.enums;

import com.botica.inteligente.shared.i18n.Mensajes;

public enum TipoComprobante {
    BOLETA("tipoComprobante.boleta"),
    NOTA_VENTA("tipoComprobante.notaVenta");

    private final String clave;

    TipoComprobante(String clave) {
        this.clave = clave;
    }

    public String etiqueta() {
        return Mensajes.get(clave);
    }
}
