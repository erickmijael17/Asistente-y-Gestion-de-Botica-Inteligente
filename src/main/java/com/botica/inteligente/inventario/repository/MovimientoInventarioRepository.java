package com.botica.inteligente.inventario.repository;

import com.botica.inteligente.inventario.entity.MovimientoInventario;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    Page<MovimientoInventario> findByProductoId(Long productoId, Pageable pageable);

    long countByCompraId(Long compraId);
}
