package com.botica.inteligente.inventario.enums;

public enum TipoMovimiento {
    COMPRA(true, "Compra"),
    ANULACION_VENTA(true, "Anulación de venta"),
    AJUSTE_ENTRADA(true, "Ajuste (entrada)"),
    VENTA(false, "Venta"),
    ANULACION_COMPRA(false, "Anulación de compra"),
    MERMA(false, "Merma / baja");

    private final boolean entrada;
    private final String etiqueta;

    TipoMovimiento(boolean entrada, String etiqueta) {
        this.entrada = entrada;
        this.etiqueta = etiqueta;
    }

    public boolean esEntrada() {
        return entrada;
    }

    public String etiqueta() {
        return etiqueta;
    }
}
