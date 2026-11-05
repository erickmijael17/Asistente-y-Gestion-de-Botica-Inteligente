package com.botica.inteligente.laboratorio.service;

public record LaboratorioResumen(Long id, LaboratorioComando datos, boolean estado) {

    public String nombre() {
        return datos.nombre();
    }

    @Override
    public String toString() {
        return datos.nombre();
    }
}
