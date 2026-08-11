package com.botica.inteligente.laboratorio.specification;

import com.botica.inteligente.laboratorio.dto.request.LaboratorioFilter;
import com.botica.inteligente.laboratorio.entity.Laboratorio;
import org.springframework.data.jpa.domain.Specification;

public final class LaboratorioSpecification {

    private LaboratorioSpecification() {
    }

    public static Specification<Laboratorio> withFilters(LaboratorioFilter filter) {
        return Specification.where(nombreContains(filter.nombre()))
                .and(rucContains(filter.ruc()))
                .and(estadoEquals(filter.estado()));
    }

    private static Specification<Laboratorio> nombreContains(String nombre) {
        return (root, query, cb) -> nombre == null || nombre.isBlank()
                ? cb.conjunction()
                : cb.like(cb.lower(root.get("nombre")), "%" + nombre.toLowerCase() + "%");
    }

    private static Specification<Laboratorio> rucContains(String ruc) {
        return (root, query, cb) -> ruc == null || ruc.isBlank()
                ? cb.conjunction()
                : cb.like(root.get("ruc"), "%" + ruc + "%");
    }

    private static Specification<Laboratorio> estadoEquals(Boolean estado) {
        return (root, query, cb) -> estado == null ? cb.conjunction() : cb.equal(root.get("estado"), estado);
    }
}
