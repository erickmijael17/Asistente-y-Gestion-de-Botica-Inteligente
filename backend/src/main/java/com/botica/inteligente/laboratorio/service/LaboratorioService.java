package com.botica.inteligente.laboratorio.service;

import com.botica.inteligente.laboratorio.dto.request.LaboratorioCreateRequest;
import com.botica.inteligente.laboratorio.dto.request.LaboratorioFilter;
import com.botica.inteligente.laboratorio.dto.request.LaboratorioUpdateRequest;
import com.botica.inteligente.laboratorio.dto.response.LaboratorioResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface LaboratorioService {

    Page<LaboratorioResponse> findAll(LaboratorioFilter filter, Pageable pageable);

    LaboratorioResponse findById(Long id);

    LaboratorioResponse create(LaboratorioCreateRequest request);

    LaboratorioResponse update(Long id, LaboratorioUpdateRequest request);

    LaboratorioResponse changeStatus(Long id, Boolean estado);
}
