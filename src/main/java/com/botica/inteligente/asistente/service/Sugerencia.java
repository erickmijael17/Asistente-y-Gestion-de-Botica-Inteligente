package com.botica.inteligente.asistente.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Producto del catalogo propuesto por el asistente, con su ficha y disponibilidad. */
public record Sugerencia(
    Long productoId,
    String nombre,
    String nombreGenerico,
    String principioActivo,
    String concentracion,
    String presentacion,
    String laboratorio,
    String categoria,
    BigDecimal precioVenta,
    long stock,
    boolean requiereReceta,
    String indicaciones,
    String contraindicaciones,
    String advertencias,
    String precauciones,
    String viaAdministracion,
    String condicionesAlmacenamiento,
    String fuenteInformacion,
    LocalDate fechaInformacion,
    int puntaje
) {

    public boolean hayStock() {
        return stock > 0;
    }
}
