package com.botica.inteligente.laboratorio.controller;

import com.botica.inteligente.laboratorio.dto.request.EstadoRequest;
import com.botica.inteligente.laboratorio.dto.request.LaboratorioCreateRequest;
import com.botica.inteligente.laboratorio.dto.request.LaboratorioFilter;
import com.botica.inteligente.laboratorio.dto.request.LaboratorioUpdateRequest;
import com.botica.inteligente.laboratorio.dto.response.LaboratorioResponse;
import com.botica.inteligente.laboratorio.service.LaboratorioService;
import com.botica.inteligente.shared.response.ApiResponse;
import com.botica.inteligente.shared.response.PageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/laboratorios")
@SecurityRequirement(name = "bearer-jwt")
public class LaboratorioController {

    private final LaboratorioService laboratorioService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','SELLER')")
    @Operation(summary = "Listar laboratorios", description = "Roles permitidos: OWNER, SELLER")
    public ApiResponse<PageResponse<LaboratorioResponse>> findAll(LaboratorioFilter filter, @ParameterObject Pageable pageable) {
        return ApiResponse.ok("Laboratorios obtenidos correctamente", PageResponse.from(laboratorioService.findAll(filter, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','SELLER')")
    public ApiResponse<LaboratorioResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok("Laboratorio obtenido correctamente", laboratorioService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<LaboratorioResponse> create(@Valid @RequestBody LaboratorioCreateRequest request) {
        return ApiResponse.ok("Laboratorio creado correctamente", laboratorioService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<LaboratorioResponse> update(@PathVariable Long id, @Valid @RequestBody LaboratorioUpdateRequest request) {
        return ApiResponse.ok("Laboratorio actualizado correctamente", laboratorioService.update(id, request));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<LaboratorioResponse> changeStatus(@PathVariable Long id, @Valid @RequestBody EstadoRequest request) {
        return ApiResponse.ok("Estado de laboratorio actualizado correctamente", laboratorioService.changeStatus(id, request.estado()));
    }
}
