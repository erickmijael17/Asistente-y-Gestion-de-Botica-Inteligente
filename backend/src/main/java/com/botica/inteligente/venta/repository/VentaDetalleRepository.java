package com.botica.inteligente.venta.repository;

import com.botica.inteligente.venta.entity.VentaDetalle;
import org.springframework.data.jpa.repository.JpaRepository;

public interface VentaDetalleRepository extends JpaRepository<VentaDetalle, Long> {
}
