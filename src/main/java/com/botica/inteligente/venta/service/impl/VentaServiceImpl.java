package com.botica.inteligente.venta.service.impl;

import com.botica.inteligente.producto.entity.Producto;
import com.botica.inteligente.producto.repository.ProductoRepository;
import com.botica.inteligente.shared.exception.ConflictException;
import com.botica.inteligente.shared.exception.ResourceNotFoundException;
import com.botica.inteligente.usuario.entity.UsuarioReferencia;
import com.botica.inteligente.usuario.repository.UsuarioReferenciaRepository;
import com.botica.inteligente.venta.dto.request.VentaCreateRequest;
import com.botica.inteligente.venta.dto.request.VentaDetalleRequest;
import com.botica.inteligente.venta.dto.request.VentaFilter;
import com.botica.inteligente.venta.dto.response.VentaResponse;
import com.botica.inteligente.venta.entity.Venta;
import com.botica.inteligente.venta.entity.VentaDetalle;
import com.botica.inteligente.venta.enums.EstadoVenta;
import com.botica.inteligente.venta.mapper.VentaMapper;
import com.botica.inteligente.venta.repository.VentaRepository;
import com.botica.inteligente.venta.service.VentaService;
import com.botica.inteligente.venta.specification.VentaSpecification;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VentaServiceImpl implements VentaService {

    private final VentaRepository ventaRepository;
    private final ProductoRepository productoRepository;
    // Asumiendo que existe un repositorio para UsuarioReferencia
    private final UsuarioReferenciaRepository usuarioRepository;
    private final VentaMapper ventaMapper;

    @Override
    public Page<VentaResponse> findAll(VentaFilter filter, Pageable pageable) {
        return ventaRepository.findAll(VentaSpecification.withFilters(filter), pageable)
                .map(ventaMapper::toResponse);
    }

    @Override
    public VentaResponse findById(Long id) {
        return ventaMapper.toResponse(findEntityById(id));
    }

    @Override
    @Transactional
    public VentaResponse create(VentaCreateRequest request) {
        UsuarioReferencia usuario = usuarioRepository.findById(request.usuarioId())
                .orElseThrow(() -> new ResourceNotFoundException("Usuario no encontrado"));

        Venta venta = new Venta();
        venta.setUsuario(usuario);
        venta.setFechaVenta(LocalDateTime.now());
        venta.setEstado(EstadoVenta.COMPLETADA);

        BigDecimal subtotalTotal = BigDecimal.ZERO;

        for (VentaDetalleRequest detRequest : request.detalles()) {
            Producto producto = productoRepository.findById(detRequest.productoId())
                    .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + detRequest.productoId()));
            
            if (!producto.getEstado()) {
                throw new ConflictException("El producto " + producto.getNombreComercial() + " se encuentra inactivo");
            }
            
            // Logica básica de reducción de stock
            if (producto.getStockMinimo() != null && detRequest.cantidad() > 0) {
                 // Aquí deberíamos descontar el stock, pero como no hay tabla estricta de inventario aún,
                 // asumiremos que la validación será más compleja en el futuro.
                 // producto.setStockMinimo(producto.getStockMinimo() - detRequest.cantidad());
            }

            VentaDetalle detalle = new VentaDetalle();
            detalle.setProducto(producto);
            detalle.setCantidad(detRequest.cantidad());
            detalle.setPrecioUnitario(detRequest.precioUnitario());
            
            BigDecimal subtotalDetalle = detRequest.precioUnitario().multiply(new BigDecimal(detRequest.cantidad()));
            detalle.setSubtotal(subtotalDetalle);
            
            venta.addDetalle(detalle);
            
            subtotalTotal = subtotalTotal.add(subtotalDetalle);
        }

        venta.setSubtotal(subtotalTotal);
        // Supongamos que no hay impuestos configurados dinámicamente por ahora (o ya están incluidos en el precio)
        venta.setImpuestos(BigDecimal.ZERO);
        venta.setTotal(subtotalTotal);

        return ventaMapper.toResponse(ventaRepository.save(venta));
    }

    @Override
    @Transactional
    public VentaResponse anular(Long id) {
        Venta venta = findEntityById(id);
        if (venta.getEstado() == EstadoVenta.ANULADA) {
            throw new ConflictException("La venta ya se encuentra anulada");
        }
        venta.setEstado(EstadoVenta.ANULADA);
        // Aquí se debería devolver el stock a inventario en el futuro
        return ventaMapper.toResponse(ventaRepository.save(venta));
    }

    private Venta findEntityById(Long id) {
        return ventaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Venta no encontrada: " + id));
    }
}
