package com.botica.inteligente.venta.service;

import com.botica.inteligente.venta.dto.request.VentaCreateRequest;
import com.botica.inteligente.venta.dto.request.VentaFilter;
import com.botica.inteligente.venta.dto.response.VentaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface VentaService {
    Page<VentaResponse> findAll(VentaFilter filter, Pageable pageable);
    VentaResponse findById(Long id);
    VentaResponse create(VentaCreateRequest request);
    VentaResponse anular(Long id);
}
