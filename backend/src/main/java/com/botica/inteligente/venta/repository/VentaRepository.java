package com.botica.inteligente.venta.repository;

import com.botica.inteligente.venta.entity.Venta;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface VentaRepository extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {

    @Override
    @EntityGraph(attributePaths = {"usuario", "detalles", "detalles.producto"})
    Page<Venta> findAll(Specification<Venta> spec, Pageable pageable);
}
