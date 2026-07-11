package com.botica.inteligente.producto.repository;

import com.botica.inteligente.producto.entity.Producto;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface ProductoRepository extends JpaRepository<Producto, Long>, JpaSpecificationExecutor<Producto> {

    boolean existsByCodigoInternoIgnoreCase(String codigoInterno);

    boolean existsByCodigoInternoIgnoreCaseAndIdNot(String codigoInterno, Long id);

    boolean existsByCodigoBarras(String codigoBarras);

    boolean existsByCodigoBarrasAndIdNot(String codigoBarras, Long id);

    Optional<Producto> findByCodigoBarras(String codigoBarras);
}
