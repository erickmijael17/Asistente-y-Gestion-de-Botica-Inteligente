package com.botica.inteligente.producto.service;

import com.botica.inteligente.categoria.entity.Categoria;
import com.botica.inteligente.categoria.repository.CategoriaRepository;
import com.botica.inteligente.laboratorio.entity.Laboratorio;
import com.botica.inteligente.laboratorio.repository.LaboratorioRepository;
import com.botica.inteligente.producto.dto.request.ProductoCreateRequest;
import com.botica.inteligente.producto.dto.request.ProductoFilter;
import com.botica.inteligente.producto.dto.request.ProductoUpdateRequest;
import com.botica.inteligente.producto.dto.response.ProductoResponse;
import com.botica.inteligente.producto.dto.response.ProductoSummaryResponse;
import com.botica.inteligente.producto.entity.Producto;
import com.botica.inteligente.producto.mapper.ProductoMapper;
import com.botica.inteligente.producto.repository.ProductoRepository;
import com.botica.inteligente.producto.specification.ProductoSpecification;
import com.botica.inteligente.shared.exception.BusinessException;
import com.botica.inteligente.shared.exception.ConflictException;
import com.botica.inteligente.shared.exception.ResourceNotFoundException;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductoServiceImpl implements ProductoService {

    private final ProductoRepository productoRepository;
    private final CategoriaRepository categoriaRepository;
    private final LaboratorioRepository laboratorioRepository;
    private final ProductoMapper productoMapper;

    @Override
    public Page<ProductoSummaryResponse> findAll(ProductoFilter filter, Pageable pageable) {
        return productoRepository.findAll(ProductoSpecification.withFilters(filter), pageable)
                .map(productoMapper::toSummaryResponse);
    }

    @Override
    public ProductoResponse findById(Long id) {
        return productoMapper.toResponse(findEntityById(id));
    }

    @Override
    public ProductoResponse findByCodigoBarras(String codigoBarras) {
        return productoMapper.toResponse(productoRepository.findByCodigoBarras(codigoBarras)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado con codigo de barras: " + codigoBarras)));
    }

    @Override
    @Transactional
    public ProductoResponse create(ProductoCreateRequest request) {
        validateUniqueCodigoInterno(request.codigoInterno());
        validateUniqueCodigoBarras(request.codigoBarras());
        validatePrices(request.precioCompra(), request.precioVenta());
        Producto producto = productoMapper.toEntity(request);
        producto.setRequiereReceta(Boolean.TRUE.equals(request.requiereReceta()));
        producto.setCategoria(findCategoria(request.categoriaId()));
        producto.setLaboratorio(findLaboratorio(request.laboratorioId()));
        return productoMapper.toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoResponse update(Long id, ProductoUpdateRequest request) {
        Producto producto = findEntityById(id);
        validateUniqueCodigoInternoForUpdate(request.codigoInterno(), id);
        validateUniqueCodigoBarrasForUpdate(request.codigoBarras(), id);
        validatePrices(request.precioCompra(), request.precioVenta());
        productoMapper.updateEntity(request, producto);
        producto.setRequiereReceta(Boolean.TRUE.equals(request.requiereReceta()));
        producto.setCategoria(findCategoria(request.categoriaId()));
        producto.setLaboratorio(findLaboratorio(request.laboratorioId()));
        return productoMapper.toResponse(productoRepository.save(producto));
    }

    @Override
    @Transactional
    public ProductoResponse changeStatus(Long id, Boolean estado) {
        Producto producto = findEntityById(id);
        producto.setEstado(estado);
        return productoMapper.toResponse(productoRepository.save(producto));
    }

    private Producto findEntityById(Long id) {
        return productoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Producto no encontrado: " + id));
    }

    private Categoria findCategoria(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada: " + id));
    }

    private Laboratorio findLaboratorio(Long id) {
        return laboratorioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratorio no encontrado: " + id));
    }

    private void validateUniqueCodigoInterno(String codigoInterno) {
        if (productoRepository.existsByCodigoInternoIgnoreCase(codigoInterno)) {
            throw new ConflictException("Ya existe un producto con el codigo interno indicado");
        }
    }

    private void validateUniqueCodigoInternoForUpdate(String codigoInterno, Long id) {
        if (productoRepository.existsByCodigoInternoIgnoreCaseAndIdNot(codigoInterno, id)) {
            throw new ConflictException("Ya existe un producto con el codigo interno indicado");
        }
    }

    private void validateUniqueCodigoBarras(String codigoBarras) {
        if (codigoBarras != null && !codigoBarras.isBlank() && productoRepository.existsByCodigoBarras(codigoBarras)) {
            throw new ConflictException("Ya existe un producto con el codigo de barras indicado");
        }
    }

    private void validateUniqueCodigoBarrasForUpdate(String codigoBarras, Long id) {
        if (codigoBarras != null && !codigoBarras.isBlank() && productoRepository.existsByCodigoBarrasAndIdNot(codigoBarras, id)) {
            throw new ConflictException("Ya existe un producto con el codigo de barras indicado");
        }
    }

    private void validatePrices(BigDecimal precioCompra, BigDecimal precioVenta) {
        if (precioVenta.compareTo(precioCompra) < 0) {
            throw new BusinessException("El precio de venta no puede ser menor que el precio de compra");
        }
    }
}
