package com.botica.inteligente.inventario.repository;

import com.botica.inteligente.inventario.entity.Lote;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoteRepository extends JpaRepository<Lote, Long> {

    Optional<Lote> findByProductoIdAndCodigoLote(Long productoId, String codigoLote);

    @Query("select coalesce(sum(l.cantidadActual), 0) from Lote l where l.producto.id = :productoId")
    long stockTotal(@Param("productoId") Long productoId);

    /** Stock vendible: excluye lotes vencidos. */
    @Query("""
        select coalesce(sum(l.cantidadActual), 0) from Lote l
        where l.producto.id = :productoId
          and (l.fechaVencimiento is null or l.fechaVencimiento >= :hoy)
        """)
    long stockDisponible(@Param("productoId") Long productoId, @Param("hoy") LocalDate hoy);

    /** Lotes con stock y vigentes, del que vence primero al que vence despues (FEFO). */
    @Query("""
        select l from Lote l
        where l.producto.id = :productoId and l.cantidadActual > 0
          and (l.fechaVencimiento is null or l.fechaVencimiento >= :hoy)
        order by case when l.fechaVencimiento is null then 1 else 0 end, l.fechaVencimiento, l.id
        """)
    List<Lote> vigentesFefo(@Param("productoId") Long productoId, @Param("hoy") LocalDate hoy);

    @Query("select l from Lote l where l.producto.id = :productoId order by l.fechaVencimiento, l.id")
    List<Lote> deProducto(@Param("productoId") Long productoId);

    /** Lotes con stock que vencen hasta la fecha limite (incluye los ya vencidos). */
    @Query("""
        select l from Lote l join fetch l.producto
        where l.cantidadActual > 0 and l.fechaVencimiento is not null and l.fechaVencimiento <= :limite
        order by l.fechaVencimiento, l.id
        """)
    List<Lote> conStockQueVencenHasta(@Param("limite") LocalDate limite);
}
