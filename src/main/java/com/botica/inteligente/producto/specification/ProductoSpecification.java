package com.botica.inteligente.producto.specification;

import com.botica.inteligente.producto.entity.Producto;
import com.botica.inteligente.producto.service.ProductoFiltro;
import com.botica.inteligente.shared.util.Textos;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import org.springframework.data.jpa.domain.Specification;

public final class ProductoSpecification {

    private ProductoSpecification() {
    }

    public static Specification<Producto> filtrar(ProductoFiltro filtro) {
        return (root, query, cb) -> {
            if (query != null && query.getResultType() != Long.class && query.getResultType() != long.class) {
                root.fetch("categoria");
                root.fetch("laboratorio", JoinType.LEFT);
            }
            List<Predicate> predicados = new ArrayList<>();
            String patron = Textos.patronBusqueda(filtro.texto());
            if (patron != null) {
                predicados.add(cb.or(
                    cb.like(cb.lower(root.get("nombreComercial")), patron),
                    cb.like(cb.lower(cb.coalesce(root.<String>get("nombreGenerico"), "")), patron),
                    cb.like(cb.lower(root.get("codigoInterno")), patron),
                    cb.like(cb.coalesce(root.<String>get("codigoBarras"), ""), patron),
                    cb.like(cb.lower(cb.coalesce(root.<String>get("principioActivo"), "")), patron)
                ));
            }
            if (filtro.estado() != null) {
                predicados.add(cb.equal(root.get("estado"), filtro.estado()));
            }
            if (filtro.categoriaId() != null) {
                predicados.add(cb.equal(root.get("categoria").get("id"), filtro.categoriaId()));
            }
            if (filtro.laboratorioId() != null) {
                predicados.add(cb.equal(root.get("laboratorio").get("id"), filtro.laboratorioId()));
            }
            if (filtro.tipoProducto() != null) {
                predicados.add(cb.equal(root.get("tipoProducto"), filtro.tipoProducto()));
            }
            return cb.and(predicados.toArray(Predicate[]::new));
        };
    }
}
