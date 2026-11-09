package com.botica.inteligente.compra.service;

import com.botica.inteligente.compra.enums.TipoDocumentoCompra;
import java.time.LocalDate;
import java.util.List;

public record NuevaCompra(
    String ruc,
    TipoDocumentoCompra tipoDocumento,
    LocalDate fechaDocumento,
    boolean aplicaIgv,
    boolean preciosIncluyenIgv,
    String observacion,
    List<LineaCompra> lineas
) {
}
