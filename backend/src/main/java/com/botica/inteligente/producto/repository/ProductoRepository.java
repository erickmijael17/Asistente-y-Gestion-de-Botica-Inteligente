package com.botica.inteligente.producto.repository;

import com.botica.inteligente.producto.entity.Producto;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    boolean existsByCodigoInternoIgnoreCase(String codigoInterno);

    boolean existsByCodigoInternoIgnoreCaseAndIdNot(String codigoInterno, Long id);

    boolean existsByCodigoBarras(String codigoBarras);

    boolean existsByCodigoBarrasAndIdNot(String codigoBarras, Long id);

    Optional<Producto> findByCodigoBarras(String codigoBarras);

    @Override
    @EntityGraph(attributePaths = {"categoria", "laboratorio"})
    Page<Producto> findAll(Specification<Producto> spec, Pageable pageable);
}
