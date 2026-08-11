package com.botica.inteligente.producto.controller;

import com.botica.inteligente.producto.dto.request.EstadoRequest;
import com.botica.inteligente.producto.dto.request.ProductoCreateRequest;
import com.botica.inteligente.producto.dto.request.ProductoFilter;
import com.botica.inteligente.producto.dto.request.ProductoUpdateRequest;
import com.botica.inteligente.producto.dto.response.ProductoResponse;
import com.botica.inteligente.producto.dto.response.ProductoSummaryResponse;
import com.botica.inteligente.producto.service.ProductoService;
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
@RequestMapping("/api/v1/productos")
@SecurityRequirement(name = "bearer-jwt")
public class ProductoController {

    private final ProductoService productoService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER','SELLER')")
    @Operation(summary = "Listar productos", description = "Roles permitidos: OWNER, SELLER")
    public ApiResponse<PageResponse<ProductoSummaryResponse>> findAll(ProductoFilter filter, @ParameterObject Pageable pageable) {
        return ApiResponse.ok("Productos obtenidos correctamente", PageResponse.from(productoService.findAll(filter, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER','SELLER')")
    public ApiResponse<ProductoResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok("Producto obtenido correctamente", productoService.findById(id));
    }

    @GetMapping("/codigo-barras/{codigoBarras}")
    @PreAuthorize("hasAnyRole('OWNER','SELLER')")
    public ApiResponse<ProductoResponse> findByCodigoBarras(@PathVariable String codigoBarras) {
        return ApiResponse.ok("Producto obtenido correctamente", productoService.findByCodigoBarras(codigoBarras));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<ProductoResponse> create(@Valid @RequestBody ProductoCreateRequest request) {
        return ApiResponse.ok("Producto creado correctamente", productoService.create(request));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<ProductoResponse> update(@PathVariable Long id, @Valid @RequestBody ProductoUpdateRequest request) {
        return ApiResponse.ok("Producto actualizado correctamente", productoService.update(id, request));
    }

    @PatchMapping("/{id}/estado")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<ProductoResponse> changeStatus(@PathVariable Long id, @Valid @RequestBody EstadoRequest request) {
        return ApiResponse.ok("Estado de producto actualizado correctamente", productoService.changeStatus(id, request.estado()));
    }
}
