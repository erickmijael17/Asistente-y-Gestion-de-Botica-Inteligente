package com.botica.inteligente.categoria.service;

public record CategoriaResumen(Long id, String nombre, String descripcion, boolean estado) {

    @Override
    public String toString() {
        return nombre;
    }
}
