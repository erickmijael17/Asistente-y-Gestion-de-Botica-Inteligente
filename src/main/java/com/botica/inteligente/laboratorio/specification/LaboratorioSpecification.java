package com.botica.inteligente.laboratorio.specification;

import com.botica.inteligente.laboratorio.entity.Laboratorio;
import com.botica.inteligente.shared.util.Textos;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class LaboratorioSpecification {

    private LaboratorioSpecification() {
    }

    public static Specification<Laboratorio> filtrar(String texto, Boolean estado) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            String patron = Textos.patronBusqueda(texto);
            if (patron != null) {
                predicados.add(cb.or(
                    cb.like(cb.lower(root.get("nombre")), patron),
                    cb.like(cb.lower(cb.coalesce(root.<String>get("ruc"), "")), patron),
                    cb.like(cb.lower(cb.coalesce(root.<String>get("correo"), "")), patron)
                ));
            }
            if (estado != null) {
                predicados.add(cb.equal(root.get("estado"), estado));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }
}
