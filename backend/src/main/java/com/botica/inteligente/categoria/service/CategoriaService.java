package com.botica.inteligente.categoria.service;

import com.botica.inteligente.categoria.dto.request.CategoriaCreateRequest;
import com.botica.inteligente.categoria.dto.request.CategoriaFilter;
import com.botica.inteligente.categoria.dto.request.CategoriaUpdateRequest;
import com.botica.inteligente.categoria.dto.response.CategoriaResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface CategoriaService {

    Page<CategoriaResponse> findAll(CategoriaFilter filter, Pageable pageable);

    CategoriaResponse findById(Long id);

    CategoriaResponse create(CategoriaCreateRequest request);

    CategoriaResponse update(Long id, CategoriaUpdateRequest request);

    CategoriaResponse changeStatus(Long id, Boolean estado);
}
