package com.botica.inteligente.laboratorio.mapper;

import com.botica.inteligente.laboratorio.dto.request.LaboratorioCreateRequest;
import com.botica.inteligente.laboratorio.dto.request.LaboratorioUpdateRequest;
import com.botica.inteligente.laboratorio.dto.response.LaboratorioResponse;
import com.botica.inteligente.laboratorio.entity.Laboratorio;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface LaboratorioMapper {

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", constant = "true")
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    Laboratorio toEntity(LaboratorioCreateRequest request);

    LaboratorioResponse toResponse(Laboratorio laboratorio);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "estado", ignore = true)
    @Mapping(target = "fechaCreacion", ignore = true)
    @Mapping(target = "fechaActualizacion", ignore = true)
    void updateEntity(LaboratorioUpdateRequest request, @MappingTarget Laboratorio laboratorio);
}
