package com.botica.inteligente.producto.service;

import com.botica.inteligente.producto.dto.request.ProductoCreateRequest;
import com.botica.inteligente.producto.dto.request.ProductoFilter;
import com.botica.inteligente.producto.dto.request.ProductoUpdateRequest;
import com.botica.inteligente.producto.dto.response.ProductoResponse;
import com.botica.inteligente.producto.dto.response.ProductoSummaryResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface ProductoService {

    Page<ProductoSummaryResponse> findAll(ProductoFilter filter, Pageable pageable);

    ProductoResponse findById(Long id);

    ProductoResponse findByCodigoBarras(String codigoBarras);

    ProductoResponse create(ProductoCreateRequest request);

    ProductoResponse update(Long id, ProductoUpdateRequest request);

    ProductoResponse changeStatus(Long id, Boolean estado);
}
