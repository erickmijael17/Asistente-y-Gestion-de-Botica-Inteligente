package com.botica.inteligente.categoria.service.impl;

import com.botica.inteligente.categoria.service.CategoriaService;
import com.botica.inteligente.categoria.dto.request.CategoriaCreateRequest;
import com.botica.inteligente.categoria.dto.request.CategoriaFilter;
import com.botica.inteligente.categoria.dto.request.CategoriaUpdateRequest;
import com.botica.inteligente.categoria.dto.response.CategoriaResponse;
import com.botica.inteligente.categoria.entity.Categoria;
import com.botica.inteligente.categoria.mapper.CategoriaMapper;
import com.botica.inteligente.categoria.repository.CategoriaRepository;
import com.botica.inteligente.categoria.specification.CategoriaSpecification;
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
public class CategoriaServiceImpl implements CategoriaService {

    private final CategoriaRepository categoriaRepository;
    private final CategoriaMapper categoriaMapper;

    @Override
    public Page<CategoriaResponse> findAll(CategoriaFilter filter, Pageable pageable) {
        return categoriaRepository.findAll(CategoriaSpecification.withFilters(filter), pageable)
                .map(categoriaMapper::toResponse);
    }

    @Override
    public CategoriaResponse findById(Long id) {
        return categoriaMapper.toResponse(findEntityById(id));
    }

    @Override
    @Transactional
    public CategoriaResponse create(CategoriaCreateRequest request) {
        validateUniqueName(request.nombre());
        return categoriaMapper.toResponse(categoriaRepository.save(categoriaMapper.toEntity(request)));
    }

    @Override
    @Transactional
    public CategoriaResponse update(Long id, CategoriaUpdateRequest request) {
        Categoria categoria = findEntityById(id);
        validateUniqueNameForUpdate(request.nombre(), id);
        categoriaMapper.updateEntity(request, categoria);
        return categoriaMapper.toResponse(categoriaRepository.save(categoria));
    }

    @Override
    @Transactional
    public CategoriaResponse changeStatus(Long id, Boolean estado) {
        Categoria categoria = findEntityById(id);
        categoria.setEstado(estado);
        return categoriaMapper.toResponse(categoriaRepository.save(categoria));
    }

    private Categoria findEntityById(Long id) {
        return categoriaRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Categoria no encontrada: " + id));
    }

    private void validateUniqueName(String nombre) {
        if (categoriaRepository.existsByNombreIgnoreCase(nombre)) {
            throw new ConflictException("Ya existe una categoria con el nombre indicado");
        }
    }

    private void validateUniqueNameForUpdate(String nombre, Long id) {
        if (categoriaRepository.existsByNombreIgnoreCaseAndIdNot(nombre, id)) {
            throw new ConflictException("Ya existe una categoria con el nombre indicado");
        }
    }
}
