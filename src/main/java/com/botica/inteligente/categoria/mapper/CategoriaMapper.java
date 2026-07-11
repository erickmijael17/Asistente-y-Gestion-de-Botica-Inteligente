package com.botica.inteligente.categoria.mapper;

import com.botica.inteligente.categoria.dto.request.CategoriaCreateRequest;
import com.botica.inteligente.categoria.dto.request.CategoriaUpdateRequest;
import com.botica.inteligente.categoria.dto.response.CategoriaResponse;
import com.botica.inteligente.categoria.entity.Categoria;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface CategoriaMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", constant = "true")
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    Categoria toEntity(CategoriaCreateRequest request);

    CategoriaResponse toResponse(Categoria categoria);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    void updateEntity(CategoriaUpdateRequest request, @MappingTarget Categoria categoria);
}
