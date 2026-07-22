package com.botica.inteligente.venta.controller;

import com.botica.inteligente.shared.response.ApiResponse;
import com.botica.inteligente.shared.response.PaginatedResponse;
import com.botica.inteligente.venta.dto.request.VentaCreateRequest;
import com.botica.inteligente.venta.dto.request.VentaFilter;
import com.botica.inteligente.venta.dto.response.VentaResponse;
import com.botica.inteligente.venta.service.VentaService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/ventas")
@RequiredArgsConstructor
public class VentaController {

    private final VentaService ventaService;

    @GetMapping
    @PreAuthorize("hasAnyRole('OWNER', 'SELLER')")
    public ApiResponse<PaginatedResponse<VentaResponse>> findAll(
            VentaFilter filter,
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.ok(PaginatedResponse.of(ventaService.findAll(filter, pageable)));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('OWNER', 'SELLER')")
    public ApiResponse<VentaResponse> findById(@PathVariable Long id) {
        return ApiResponse.ok(ventaService.findById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasAnyRole('OWNER', 'SELLER')")
    public ApiResponse<VentaResponse> create(@Valid @RequestBody VentaCreateRequest request) {
        return ApiResponse.created(ventaService.create(request), "Venta registrada exitosamente");
    }

    @PatchMapping("/{id}/anular")
    @PreAuthorize("hasRole('OWNER')")
    public ApiResponse<VentaResponse> anular(@PathVariable Long id) {
        return ApiResponse.ok(ventaService.anular(id), "Venta anulada exitosamente");
    }
}
