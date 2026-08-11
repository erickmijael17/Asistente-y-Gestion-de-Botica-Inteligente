package com.botica.inteligente.venta.mapper;

import com.botica.inteligente.venta.dto.response.VentaDetalleResponse;
import com.botica.inteligente.venta.dto.response.VentaResponse;
import com.botica.inteligente.venta.entity.Venta;
import com.botica.inteligente.venta.entity.VentaDetalle;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingConstants;

@Mapper(componentModel = MappingConstants.ComponentModel.SPRING)
public interface VentaMapper {

    @Mapping(target = "usuarioId", source = "usuario.id")
    @Mapping(target = "usuarioUsername", source = "usuario.username")
    VentaResponse toResponse(Venta venta);

    @Mapping(target = "productoId", source = "producto.id")
    @Mapping(target = "productoNombre", source = "producto.nombreComercial")
    VentaDetalleResponse toDetalleResponse(VentaDetalle detalle);
}
