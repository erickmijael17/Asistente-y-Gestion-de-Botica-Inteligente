CREATE TABLE compras_nueva (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    numero INTEGER NOT NULL UNIQUE,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    fecha TIMESTAMP NOT NULL,
    proveedor_id BIGINT,
    usuario_id BIGINT NOT NULL,
    tipo_documento VARCHAR(20) NOT NULL,
    fecha_documento DATE NOT NULL,
    aplica_igv BOOLEAN NOT NULL,
    precios_incluyen_igv BOOLEAN NOT NULL,
    tasa_igv NUMERIC(5, 4) NOT NULL,
    base_imponible NUMERIC(12, 2) NOT NULL,
    igv NUMERIC(12, 2) NOT NULL,
    total NUMERIC(12, 2) NOT NULL,
    estado VARCHAR(12) NOT NULL CHECK (estado IN ('REGISTRADA', 'ANULADA')),
    observacion VARCHAR(500),
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL,
    CONSTRAINT fk_compras_proveedor FOREIGN KEY (proveedor_id) REFERENCES proveedores(id),
    CONSTRAINT fk_compras_usuario FOREIGN KEY (usuario_id) REFERENCES usuarios(id)
);

INSERT INTO compras_nueva (
    id, numero, codigo, fecha, proveedor_id, usuario_id, tipo_documento, fecha_documento,
    aplica_igv, precios_incluyen_igv, tasa_igv, base_imponible, igv, total, estado, observacion,
    fecha_creacion, fecha_actualizacion
)
SELECT
    id, numero, codigo, fecha, proveedor_id, usuario_id, tipo_documento, fecha_documento,
    aplica_igv, precios_incluyen_igv, tasa_igv, base_imponible, igv, total, estado, observacion,
    fecha_creacion, fecha_actualizacion
FROM compras;

DROP TABLE compras;
ALTER TABLE compras_nueva RENAME TO compras;

CREATE INDEX ix_compras_fecha ON compras (fecha);
