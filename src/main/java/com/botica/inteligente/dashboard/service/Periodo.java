package com.botica.inteligente.dashboard.service;

import java.time.LocalDate;

public enum Periodo {
    HOY("Hoy"),
    ULTIMOS_7("Últimos 7 días"),
    ULTIMOS_30("Últimos 30 días"),
    ESTE_MES("Este mes"),
    ESTE_ANIO("Este año");

    private final String etiqueta;

    Periodo(String etiqueta) {
        this.etiqueta = etiqueta;
    }

    public String etiqueta() {
        return etiqueta;
    }

    public LocalDate desde(LocalDate hoy) {
        return switch (this) {
            case HOY -> hoy;
            case ULTIMOS_7 -> hoy.minusDays(6);
            case ULTIMOS_30 -> hoy.minusDays(29);
            case ESTE_MES -> hoy.withDayOfMonth(1);
            case ESTE_ANIO -> hoy.withDayOfYear(1);
        };
    }
}
