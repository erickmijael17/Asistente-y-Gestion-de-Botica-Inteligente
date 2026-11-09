package com.botica.inteligente.compra.specification;

import com.botica.inteligente.compra.entity.Compra;
import com.botica.inteligente.compra.enums.EstadoCompra;
import com.botica.inteligente.proveedor.entity.Proveedor;
import com.botica.inteligente.shared.util.Textos;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class CompraSpecification {

    private CompraSpecification() {
    }

    public static Specification<Compra> filtrar(
        String texto, EstadoCompra estado, LocalDate desde, LocalDate hasta
    ) {
        return (root, query, cb) -> {
            jakarta.persistence.criteria.Join<Compra, Proveedor> proveedor =
                root.join("proveedor", JoinType.LEFT);
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("proveedor", JoinType.LEFT);
            }
            List<Predicate> predicados = new ArrayList<>();
            String patron = Textos.patronBusqueda(texto);
            if (patron != null) {
                predicados.add(cb.or(
                    cb.like(cb.lower(root.get("codigo")), patron),
                    cb.like(cb.lower(proveedor.get("razonSocial")), patron),
                    cb.like(proveedor.get("ruc"), patron)
                ));
            }
            if (estado != null) {
                predicados.add(cb.equal(root.get("estado"), estado));
            }
            if (desde != null) {
                predicados.add(cb.greaterThanOrEqualTo(root.get("fecha"), desde.atStartOfDay()));
            }
            if (hasta != null) {
                predicados.add(cb.lessThan(root.get("fecha"), hasta.plusDays(1).atStartOfDay()));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }
}
