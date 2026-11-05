package com.botica.inteligente.venta.repository;

import com.botica.inteligente.venta.entity.Venta;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface VentaRepository extends JpaRepository<Venta, Long>, JpaSpecificationExecutor<Venta> {

    @Query("select coalesce(max(v.numero), 0) from Venta v where v.serie = :serie")
    int ultimoNumero(@Param("serie") String serie);

    @Query("""
        select v from Venta v join fetch v.usuario
        left join fetch v.detalles
        where v.id = :id
        """)
    Optional<Venta> buscarConDetalle(@Param("id") Long id);

    @Query("""
        select coalesce(sum(v.total), 0) from Venta v
        where v.estado = com.botica.inteligente.venta.enums.EstadoVenta.EMITIDA
          and v.fecha >= :desde and v.fecha < :hasta
        """)
    BigDecimal totalEmitido(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
