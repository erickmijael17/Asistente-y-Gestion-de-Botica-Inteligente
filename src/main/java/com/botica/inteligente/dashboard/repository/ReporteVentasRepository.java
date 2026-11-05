package com.botica.inteligente.dashboard.repository;

import com.botica.inteligente.dashboard.service.ProductoVendido;
import com.botica.inteligente.dashboard.service.PuntoVenta;
import com.botica.inteligente.venta.entity.Venta;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Consultas de solo lectura para el dashboard; solo cuentan ventas emitidas. */
public interface ReporteVentasRepository extends JpaRepository<Venta, Long> {

    @Query("""
        select new com.botica.inteligente.dashboard.service.PuntoVenta(v.fecha, v.total)
        from Venta v
        where v.estado = com.botica.inteligente.venta.enums.EstadoVenta.EMITIDA
          and v.fecha >= :desde and v.fecha < :hasta
        order by v.fecha
        """)
    List<PuntoVenta> ventasEmitidas(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("""
        select new com.botica.inteligente.dashboard.service.ProductoVendido(
            d.producto.id, d.nombreProducto, sum(d.cantidad), sum(d.importe))
        from VentaDetalle d join d.venta v
        where v.estado = com.botica.inteligente.venta.enums.EstadoVenta.EMITIDA
          and v.fecha >= :desde and v.fecha < :hasta
        group by d.producto.id, d.nombreProducto
        order by sum(d.cantidad) desc, sum(d.importe) desc
        """)
    List<ProductoVendido> masVendidos(
        @Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta, Pageable pageable
    );

    /** Ingreso sin IGV: cada linea aporta la parte proporcional de la base imponible de su venta. */
    @Query("""
        select coalesce(sum(d.importe * v.baseImponible / v.total), 0)
        from VentaDetalle d join d.venta v
        where v.estado = com.botica.inteligente.venta.enums.EstadoVenta.EMITIDA
          and v.fecha >= :desde and v.fecha < :hasta
        """)
    BigDecimal ingresoSinIgv(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);

    @Query("""
        select coalesce(sum(d.cantidad * coalesce(d.costoUnitario, 0)), 0)
        from VentaDetalle d join d.venta v
        where v.estado = com.botica.inteligente.venta.enums.EstadoVenta.EMITIDA
          and v.fecha >= :desde and v.fecha < :hasta
        """)
    BigDecimal costoVendido(@Param("desde") LocalDateTime desde, @Param("hasta") LocalDateTime hasta);
}
