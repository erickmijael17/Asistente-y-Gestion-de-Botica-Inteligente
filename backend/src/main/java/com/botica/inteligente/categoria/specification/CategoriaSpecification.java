package com.botica.inteligente.categoria.specification;

import com.botica.inteligente.categoria.dto.request.CategoriaFilter;
import com.botica.inteligente.categoria.entity.Categoria;
import org.springframework.data.jpa.domain.Specification;

public final class CategoriaSpecification {

    private CategoriaSpecification() {
    }

    public static Specification<Categoria> withFilters(CategoriaFilter filter) {
        return Specification.where(nombreContains(filter.nombre()))
                .and(estadoEquals(filter.estado()));
    }

    private static Specification<Categoria> nombreContains(String nombre) {
        return (root, query, cb) -> nombre == null || nombre.isBlank()
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%");
    }

    private static Specification<Categoria> estadoEquals(Boolean estado) {
        return (root, query, cb) -> estado == null ? cb.conjunction() : cb.equal(root.get("estado"), estado);
    }
}
