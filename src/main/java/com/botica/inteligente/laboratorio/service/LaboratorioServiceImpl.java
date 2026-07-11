package com.botica.inteligente.laboratorio.service;

import com.botica.inteligente.laboratorio.dto.request.LaboratorioCreateRequest;
import com.botica.inteligente.laboratorio.dto.request.LaboratorioFilter;
import com.botica.inteligente.laboratorio.dto.request.LaboratorioUpdateRequest;
import com.botica.inteligente.laboratorio.dto.response.LaboratorioResponse;
import com.botica.inteligente.laboratorio.entity.Laboratorio;
import com.botica.inteligente.laboratorio.mapper.LaboratorioMapper;
import com.botica.inteligente.laboratorio.repository.LaboratorioRepository;
import com.botica.inteligente.laboratorio.specification.LaboratorioSpecification;
import com.botica.inteligente.shared.exception.ConflictException;
import com.botica.inteligente.shared.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class LaboratorioServiceImpl implements LaboratorioService {

    private final LaboratorioRepository laboratorioRepository;
    private final LaboratorioMapper laboratorioMapper;

    @Override
    public Page<LaboratorioResponse> findAll(LaboratorioFilter filter, Pageable pageable) {
        return laboratorioRepository.findAll(LaboratorioSpecification.withFilters(filter), pageable)
                .map(laboratorioMapper::toResponse);
    }

    @Override
    public LaboratorioResponse findById(Long id) {
        return laboratorioMapper.toResponse(findEntityById(id));
    }

    @Override
    @Transactional
    public LaboratorioResponse create(LaboratorioCreateRequest request) {
        validateUniqueName(request.nombre());
        validateUniqueRuc(request.ruc());
        return laboratorioMapper.toResponse(laboratorioRepository.save(laboratorioMapper.toEntity(request)));
    }

    @Override
    @Transactional
    public LaboratorioResponse update(Long id, LaboratorioUpdateRequest request) {
        Laboratorio laboratorio = findEntityById(id);
        validateUniqueNameForUpdate(request.nombre(), id);
        validateUniqueRucForUpdate(request.ruc(), id);
        laboratorioMapper.updateEntity(request, laboratorio);
        return laboratorioMapper.toResponse(laboratorioRepository.save(laboratorio));
    }

    @Override
    @Transactional
    public LaboratorioResponse changeStatus(Long id, Boolean estado) {
        Laboratorio laboratorio = findEntityById(id);
        laboratorio.setEstado(estado);
        return laboratorioMapper.toResponse(laboratorioRepository.save(laboratorio));
    }

    private Laboratorio findEntityById(Long id) {
        return laboratorioRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Laboratorio no encontrado: " + id));
    }

    private void validateUniqueName(String nombre) {
        if (laboratorioRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictException("Ya existe un laboratorio con el nombre indicado");
        }
    }

    private void validateUniqueNameForUpdate(String nombre, Long id) {
        if (laboratorioRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ConflictException("Ya existe un laboratorio con el nombre indicado");
        }
    }

    private void validateUniqueRuc(String ruc) {
        if (ruc != null && !ruc.isBlank() && laboratorioRepository.existsByRuc(ruc)) {
            throw new ConflictException("Ya existe un laboratorio con el RUC indicado");
        }
    }

    private void validateUniqueRucForUpdate(String ruc, Long id) {
        if (ruc != null && !ruc.isBlank() && laboratorioRepository.existsByRucAndIdNot(ruc, id)) {
            throw new ConflictException("Ya existe un laboratorio con el RUC indicado");
        }
    }
}
