package com.botica.inteligente.dashboard.service;

import com.botica.inteligente.shared.i18n.Mensajes;
import java.time.LocalDate;

public enum Periodo {
    HOY("periodo.hoy"),
    ULTIMOS_7("periodo.ultimos7"),
    ULTIMOS_30("periodo.ultimos30"),
    ESTE_MES("periodo.esteMes"),
    ESTE_ANIO("periodo.esteAnio");

    private final String clave;

    Periodo(String clave) {
        this.clave = clave;
    }

    public String etiqueta() {
        return Mensajes.get(clave);
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
