package com.botica.inteligente.inventario.repository;

import com.botica.inteligente.inventario.service.InventarioFila;
import com.botica.inteligente.producto.entity.Producto;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/** Consultas agregadas de inventario (stock por producto). Se apoya en el repositorio de productos. */
public interface InventarioConsultaRepository extends JpaRepository<Producto, Long> {

    @Query("""
        select new com.botica.inteligente.inventario.service.InventarioFila(
            p.id, p.codigoInterno, p.nombreComercial, p.stockMinimo,
            coalesce(sum(l.cantidadActual), 0L),
            min(case when l.cantidadActual > 0 then l.fechaVencimiento else null end),
            p.costoPromedio, p.precioVenta)
        from Producto p left join Lote l on l.producto = p
        where p.estado = true
          and (:patron = '' or lower(p.nombreComercial) like :patron or lower(p.codigoInterno) like :patron
               or coalesce(p.codigoBarras, '') like :patron)
        group by p.id, p.codigoInterno, p.nombreComercial, p.stockMinimo, p.costoPromedio, p.precioVenta
        order by p.nombreComercial
        """)
    List<InventarioFila> filas(@Param("patron") String patron);
}
