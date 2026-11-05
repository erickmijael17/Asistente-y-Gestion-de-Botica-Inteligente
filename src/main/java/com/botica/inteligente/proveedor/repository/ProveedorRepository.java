package com.botica.inteligente.proveedor.repository;

import com.botica.inteligente.proveedor.entity.Proveedor;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProveedorRepository extends JpaRepository<Proveedor, Long> {

    Optional<Proveedor> findByRuc(String ruc);
}
