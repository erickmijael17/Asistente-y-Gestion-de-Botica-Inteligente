package com.botica.inteligente.producto.mapper;

import com.botica.inteligente.producto.dto.request.ProductoCreateRequest;
import com.botica.inteligente.producto.dto.request.ProductoUpdateRequest;
import com.botica.inteligente.producto.dto.response.ProductoResponse;
import com.botica.inteligente.producto.dto.response.ProductoSummaryResponse;
import com.botica.inteligente.producto.entity.Producto;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface ProductoMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", constant = "true")
    @Mapping(target = "categoria", ignore = true)
    @Mapping(target = "laboratorio", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    Producto toEntity(ProductoCreateRequest request);

    @Mapping(target = "categoriaId", source = "categoria.id")
    @Mapping(target = "categoriaNombre", source = "categoria.nombre")
    @Mapping(target = "laboratorioId", source = "laboratorio.id")
    @Mapping(target = "laboratorioNombre", source = "laboratorio.nombre")
    ProductoResponse toResponse(Producto producto);

    @Mapping(target = "categoriaId", source = "categoria.id")
    @Mapping(target = "categoriaNombre", source = "categoria.nombre")
    @Mapping(target = "laboratorioId", source = "laboratorio.id")
    @Mapping(target = "laboratorioNombre", source = "laboratorio.nombre")
    ProductoSummaryResponse toSummaryResponse(Producto producto);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "categoria", ignore = true)
    @Mapping(target = "laboratorio", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    void updateEntity(ProductoUpdateRequest request, @MappingTarget Producto producto);
}
