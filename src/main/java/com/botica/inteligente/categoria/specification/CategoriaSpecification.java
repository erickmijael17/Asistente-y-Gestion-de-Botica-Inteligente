package com.botica.inteligente.categoria.specification;

import com.botica.inteligente.categoria.entity.Categoria;
import com.botica.inteligente.shared.util.Textos;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class CategoriaSpecification {

    private CategoriaSpecification() {
    }

    public static Specification<Categoria> filtrar(String texto, Boolean estado) {
        return (root, query, cb) -> {
            List<Predicate> predicados = new ArrayList<>();
            String patron = Textos.patronBusqueda(texto);
            if (patron != null) {
                predicados.add(cb.or(
                    cb.like(cb.lower(root.get("nombre")), patron),
                    cb.like(cb.lower(cb.coalesce(root.<String>get("descripcion"), "")), patron)
                ));
            }
            if (estado != null) {
                predicados.add(cb.equal(root.get("estado"), estado));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }
}
