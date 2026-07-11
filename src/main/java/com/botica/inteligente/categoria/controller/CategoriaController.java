package com.botica.inteligente.categoria.controller;

import com.botica.inteligente.categoria.dto.request.CategoriaCreateRequest;
import com.botica.inteligente.categoria.dto.request.CategoriaFilter;
import com.botica.inteligente.categoria.dto.request.CategoriaUpdateRequest;
import com.botica.inteligente.categoria.dto.request.EstadoRequest;
import com.botica.inteligente.categoria.dto.response.CategoriaResponse;
import com.botica.inteligente.categoria.service.CategoriaService;
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
@RequestMapping("/api/v1/categorias")
@SecurityRequirement(name = "bearer-jwt")
public class CategoriaController {

    private final CategoriaService categoriaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','SELLER')")
    @Operation(summary = "Listar categorias", description = "Roles permitidos: OWNER, SELLER")
    public ApiResponse<PageResponse<CategoriaResponse>> findAll(CategoriaFilter filter, @ParameterObject Pageable pageable) {
        return ApiResponse.ok("Categorias obtenidas correctamente", PageResponse.from(categoriaService.findAll(filter, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','SELLER')")
    public ApiResponse<CategoriaResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok("Categoria obtenida correctamente", categoriaService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<CategoriaResponse> create(@Valid @RequestBody CategoriaCreateRequest request) {
        return ApiResponse.ok("Categoria creada correctamente", categoriaService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<CategoriaResponse> update(@PathVariable Long id, @Valid @RequestBody CategoriaUpdateRequest request) {
        return ApiResponse.ok("Categoria actualizada correctamente", categoriaService.update(id, request));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<CategoriaResponse> changeStatus(@PathVariable Long id, @Valid @RequestBody EstadoRequest request) {
        return ApiResponse.ok("Estado de categoria actualizado correctamente", categoriaService.changeStatus(id, request.estado()));
    }
}
