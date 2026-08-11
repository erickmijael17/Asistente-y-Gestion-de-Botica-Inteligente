package com.botica.inteligente.producto.specification;

import com.botica.inteligente.producto.dto.request.ProductoFilter;
import com.botica.inteligente.producto.entity.Producto;
import org.springframework.data.jpa.domain.Specification;

public final class ProductoSpecification {

    private ProductoSpecification() {
    }

    public static Specification<Producto> withFilters(ProductoFilter filter) {
        return Specification.where(like("codigoInterno", filter.codigoInterno()))
                .and(like("codigoBarras", filter.codigoBarras()))
                .and(like("nombreComercial", filter.nombreComercial()))
                .and(like("nombreGenerico", filter.nombreGenerico()))
                .and(like("principioActivo", filter.principioActivo()))
                .and((root, query, cb) -> filter.tipoProducto() == null ? cb.conjunction() : cb.equal(root.get("tipoProducto"), filter.tipoProducto()))
                .and((root, query, cb) -> filter.categoriaId() == null ? cb.conjunction() : cb.equal(root.get("categoria").get("id"), filter.categoriaId()))
                .and((root, query, cb) -> filter.laboratorioId() == null ? cb.conjunction() : cb.equal(root.get("laboratorio").get("id"), filter.laboratorioId()))
                .and((root, query, cb) -> filter.requiereReceta() == null ? cb.conjunction() : cb.equal(root.get("requiereReceta"), filter.requiereReceta()))
                .and((root, query, cb) -> filter.estado() == null ? cb.conjunction() : cb.equal(root.get("estado"), filter.estado()));
    }

    private static Specification<Producto> like(String field, String value) {
        return (root, query, cb) -> value == null || value.isBlank()
                ? cb.conjunction()
                : cb.like(cb.lower(root.get(field)), "%" + value.toLowerCase() + "%");
    }
}
