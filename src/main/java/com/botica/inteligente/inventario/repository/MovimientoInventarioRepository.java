package com.botica.inteligente.inventario.repository;

import com.botica.inteligente.inventario.entity.MovimientoInventario;
import com.botica.inteligente.inventario.enums.TipoMovimiento;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface MovimientoInventarioRepository extends JpaRepository<MovimientoInventario, Long> {

    Page<MovimientoInventario> findByProductoId(Long productoId, Pageable pageable);

    long countByCompraId(Long compraId);

    /** Movimientos de un tipo generados por una venta, con su lote (para revertirlos al anular). */
    @Query("""
        select m from MovimientoInventario m join fetch m.producto join fetch m.lote
        where m.ventaId = :ventaId and m.tipo = :tipo
        """)
    List<MovimientoInventario> deVenta(@Param("ventaId") Long ventaId, @Param("tipo") TipoMovimiento tipo);
}
