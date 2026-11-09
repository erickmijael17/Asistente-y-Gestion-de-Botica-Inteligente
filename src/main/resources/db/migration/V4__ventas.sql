CREATE TABLE ventas (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    tipo_comprobante VARCHAR(20) NOT NULL CHECK (tipo_comprobante IN ('BOLETA', 'NOTA_VENTA')),
    serie VARCHAR(4) NOT NULL,
    numero INTEGER NOT NULL,
    comprobante VARCHAR(20) NOT NULL UNIQUE,
    fecha TIMESTAMP NOT NULL,
    usuario_id BIGINT NOT NULL,
    cliente_nombre VARCHAR(150),
    cliente_documento VARCHAR(15),
    aplica_igv BOOLEAN NOT NULL,
    precios_incluyen_igv BOOLEAN NOT NULL,
    tasa_igv NUMERIC(5, 4) NOT NULL,
    base_imponible NUMERIC(12, 2) NOT NULL,
    igv NUMERIC(12, 2) NOT NULL,
    total NUMERIC(12, 2) NOT NULL,
    estado VARCHAR(12) NOT NULL CHECK (estado IN ('EMITIDA', 'ANULADA')),
    observacion VARCHAR(500),
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL,
    CONSTRAINT fk_ventas_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id),
    CONSTRAINT uk_ventas_serie_numero UNIQUE (serie, numero)
);

CREATE INDEX ix_ventas_fecha ON ventas (fecha);

CREATE TABLE ventas_detalle (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    venta_id BIGINT NOT NULL,
    item INTEGER NOT NULL,
    producto_id BIGINT NOT NULL,
    codigo_producto VARCHAR(60) NOT NULL,
    nombre_producto VARCHAR(180) NOT NULL,
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    precio_unitario NUMERIC(12, 2) NOT NULL,
    importe NUMERIC(12, 2) NOT NULL,
    costo_unitario NUMERIC(12, 4),
    CONSTRAINT fk_ventas_detalle_venta FOREIGN KEY (venta_id) REFERENCES ventas(id),
    CONSTRAINT fk_ventas_detalle_producto FOREIGN KEY (producto_id) REFERENCES productos(id)
);

CREATE INDEX ix_ventas_detalle_venta ON ventas_detalle (venta_id);
CREATE INDEX ix_ventas_detalle_producto ON ventas_detalle (producto_id);
