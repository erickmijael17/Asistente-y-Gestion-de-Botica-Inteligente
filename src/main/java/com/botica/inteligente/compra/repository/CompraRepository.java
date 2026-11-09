package com.botica.inteligente.compra.repository;

import com.botica.inteligente.compra.entity.Compra;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface CompraRepository extends JpaRepository<Compra, Long>, JpaSpecificationExecutor<Compra> {

    @Query("select coalesce(max(c.numero), 0) from Compra c")
    int ultimoNumero();

    @Query("""
        select c from Compra c left join fetch c.proveedor
        left join fetch c.detalles d left join fetch d.lote
        where c.id = :id
        """)
    Optional<Compra> buscarConDetalle(Long id);
}
