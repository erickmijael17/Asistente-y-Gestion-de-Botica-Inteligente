package com.botica.inteligente.compra.repository;

import com.botica.inteligente.compra.entity.Compra;
import com.botica.inteligente.compra.enums.EstadoCompra;
import com.botica.inteligente.compra.enums.TipoDocumentoCompra;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

public interface CompraRepository extends JpaRepository<Compra, Long>, JpaSpecificationExecutor<Compra> {

    @Query("select coalesce(max(c.numero), 0) from Compra c")
    int ultimoNumero();

    boolean existsByProveedorIdAndTipoDocumentoAndNumeroDocumentoIgnoreCaseAndEstado(
        Long proveedorId, TipoDocumentoCompra tipoDocumento, String numeroDocumento, EstadoCompra estado
    );

    @Query("""
        select c from Compra c join fetch c.proveedor
        left join fetch c.detalles d left join fetch d.lote
        where c.id = :id
        """)
    Optional<Compra> buscarConDetalle(Long id);
}
