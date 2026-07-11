package com.botica.inteligente.producto.dto.request;

import com.botica.inteligente.producto.enums.TipoProducto;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public record ProductoUpdateRequest(
        @NotBlank(message = "El codigo interno es obligatorio") @Size(max = 60) String codigoInterno,
        @Size(max = 80) String codigoBarras,
        @NotBlank(message = "El nombre comercial es obligatorio") @Size(max = 180) String nombreComercial,
        @Size(max = 180) String nombreGenerico,
        @Size(max = 700) String descripcion,
        @NotNull(message = "El tipo de producto es obligatorio") TipoProducto tipoProducto,
        @Size(max = 180) String principioActivo,
        @Size(max = 100) String concentracion,
        @Size(max = 120) String presentacion,
        @Size(max = 100) String viaAdministracion,
        @Size(max = 1000) String indicacionesOficiales,
        @Size(max = 1000) String contraindicaciones,
        @Size(max = 1000) String advertencias,
        @Size(max = 1000) String precauciones,
        @Size(max = 500) String condicionesAlmacenamiento,
        Boolean requiereReceta,
        @Size(max = 100) String registroSanitario,
        @NotNull(message = "El precio de compra es obligatorio") @DecimalMin(value = "0.00") BigDecimal precioCompra,
        @NotNull(message = "El precio de venta es obligatorio") @DecimalMin(value = "0.01") BigDecimal precioVenta,
        @NotNull(message = "El stock minimo es obligatorio") @Min(0) Integer stockMinimo,
        @Size(max = 500) String imagenUrl,
        @NotNull(message = "La categoria es obligatoria") Long categoriaId,
        @NotNull(message = "El laboratorio es obligatorio") Long laboratorioId,
        @Size(max = 200) String fuenteInformacion,
        LocalDate fechaActualizacionInformacion
) {
}
