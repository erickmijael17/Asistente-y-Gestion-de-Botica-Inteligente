package com.botica.inteligente.venta.specification;

import com.botica.inteligente.shared.util.Textos;
import com.botica.inteligente.venta.entity.Venta;
import com.botica.inteligente.venta.enums.EstadoVenta;
import jakarta.persistence.criteria.Predicate;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class VentaSpecification {

    private VentaSpecification() {
    }

    public static Specification<Venta> filtrar(String texto, EstadoVenta estado, LocalDate desde, LocalDate hasta) {
        return (root, query, cb) -> {
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("usuario");
            }
            List<Predicate> predicados = new ArrayList<>();
            String patron = Textos.patronBusqueda(texto);
            if (patron != null) {
                predicados.add(cb.or(
                    cb.like(cb.lower(root.get("comprobante")), patron),
                    cb.like(cb.lower(cb.coalesce(root.<String>get("clienteNombre"), "")), patron),
                    cb.like(cb.coalesce(root.<String>get("clienteDocumento"), ""), patron)
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
