package com.botica.inteligente.venta.specification;

import com.botica.inteligente.venta.dto.request.VentaFilter;
import com.botica.inteligente.venta.entity.Venta;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public class VentaSpecification {

    public static Specification<Venta> withFilters(VentaFilter filter) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            if (filter.usuarioId() != null) {
                predicates.add(cb.equal(root.get("usuario").get("id"), filter.usuarioId()));
            }
            if (filter.estado() != null) {
                predicates.add(cb.equal(root.get("estado"), filter.estado()));
            }
            if (filter.fechaDesde() != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("fechaVenta"), filter.fechaDesde().atStartOfDay()));
            }
            if (filter.fechaHasta() != null) {
                predicates.add(cb.lessThanOrEqualTo(root.get("fechaVenta"), filter.fechaHasta().plusDays(1).atStartOfDay()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };
    }
}
