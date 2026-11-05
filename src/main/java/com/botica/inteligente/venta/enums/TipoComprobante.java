package com.botica.inteligente.venta.enums;

public enum TipoComprobante {
    BOLETA("Boleta de venta"),
    NOTA_VENTA("Nota de venta");

    private final String etiqueta;

    TipoComprobante(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }
}
