CREATE TABLE ventas (
    id BIGSERIAL PRIMARY KEY,
    usuario_id BIGINT NOT NULL,
    fecha_venta TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    subtotal NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    impuestos NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    total NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    estado VARCHAR(30) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ventas_usuario FOREIGN KEY (usuario_id) REFERENCES usuario_referencia (id),
    CONSTRAINT ck_ventas_estado CHECK (estado IN ('COMPLETADA', 'ANULADA')),
    CONSTRAINT ck_ventas_total CHECK (total >= 0)
);

CREATE INDEX idx_ventas_usuario_id ON ventas (usuario_id);
CREATE INDEX idx_ventas_fecha_venta ON ventas (fecha_venta);
CREATE INDEX idx_ventas_estado ON ventas (estado);

CREATE TABLE venta_detalles (
    id BIGSERIAL PRIMARY KEY,
    venta_id BIGINT NOT NULL,
    producto_id BIGINT NOT NULL,
    cantidad INTEGER NOT NULL,
    precio_unitario NUMERIC(12,2) NOT NULL,
    subtotal NUMERIC(12,2) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_venta_detalles_venta FOREIGN KEY (venta_id) REFERENCES ventas (id) ON DELETE CASCADE,
    CONSTRAINT fk_venta_detalles_producto FOREIGN KEY (producto_id) REFERENCES productos (id),
    CONSTRAINT ck_venta_detalles_cantidad CHECK (cantidad > 0),
    CONSTRAINT ck_venta_detalles_precio_unitario CHECK (precio_unitario >= 0),
    CONSTRAINT ck_venta_detalles_subtotal CHECK (subtotal >= 0)
);

CREATE INDEX idx_venta_detalles_venta_id ON venta_detalles (venta_id);
CREATE INDEX idx_venta_detalles_producto_id ON venta_detalles (producto_id);
