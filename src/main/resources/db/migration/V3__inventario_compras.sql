ALTER TABLE productos ADD COLUMN costo_promedio NUMERIC(12, 4);

CREATE TABLE proveedores (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    ruc VARCHAR(11) NOT NULL UNIQUE,
    razon_social VARCHAR(200) NOT NULL,
    direccion VARCHAR(250),
    telefono VARCHAR(30),
    estado BOOLEAN NOT NULL DEFAULT 1,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL
);

CREATE TABLE compras (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    numero INTEGER NOT NULL UNIQUE,
    codigo VARCHAR(20) NOT NULL UNIQUE,
    fecha TIMESTAMP NOT NULL,
    proveedor_id BIGINT NOT NULL,
    usuario_id BIGINT NOT NULL,
    tipo_documento VARCHAR(20) NOT NULL,
    numero_documento VARCHAR(40) NOT NULL,
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

CREATE UNIQUE INDEX ux_compras_documento_vigente
    ON compras (proveedor_id, tipo_documento, numero_documento)
    WHERE estado = 'REGISTRADA';
CREATE INDEX ix_compras_fecha ON compras (fecha);

CREATE TABLE lotes (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    producto_id BIGINT NOT NULL,
    codigo_lote VARCHAR(60) NOT NULL,
    fecha_vencimiento DATE,
    cantidad_inicial INTEGER NOT NULL CHECK (cantidad_inicial > 0),
    cantidad_actual INTEGER NOT NULL CHECK (cantidad_actual >= 0),
    costo_unitario NUMERIC(12, 4) NOT NULL,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL,
    CONSTRAINT fk_lotes_producto FOREIGN KEY (producto_id) REFERENCES productos(id),
    CONSTRAINT uk_lotes_producto_codigo UNIQUE (producto_id, codigo_lote)
);

CREATE INDEX ix_lotes_producto ON lotes (producto_id);
CREATE INDEX ix_lotes_vencimiento ON lotes (fecha_vencimiento);

CREATE TABLE compras_detalle (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    compra_id BIGINT NOT NULL,
    item INTEGER NOT NULL,
    producto_id BIGINT NOT NULL,
    lote_id BIGINT NOT NULL,
    codigo_producto VARCHAR(60) NOT NULL,
    nombre_producto VARCHAR(180) NOT NULL,
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    costo_unitario NUMERIC(12, 4) NOT NULL,
    importe NUMERIC(12, 2) NOT NULL,
    CONSTRAINT fk_compras_detalle_compra FOREIGN KEY (compra_id) REFERENCES compras(id),
    CONSTRAINT fk_compras_detalle_producto FOREIGN KEY (producto_id) REFERENCES productos(id),
    CONSTRAINT fk_compras_detalle_lote FOREIGN KEY (lote_id) REFERENCES lotes(id)
);

CREATE INDEX ix_compras_detalle_compra ON compras_detalle (compra_id);

CREATE TABLE movimientos_inventario (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    fecha TIMESTAMP NOT NULL,
    producto_id BIGINT NOT NULL,
    lote_id BIGINT,
    tipo VARCHAR(30) NOT NULL,
    cantidad INTEGER NOT NULL CHECK (cantidad > 0),
    stock_resultante INTEGER NOT NULL,
    costo_unitario NUMERIC(12, 4),
    documento VARCHAR(40),
    compra_id BIGINT,
    venta_id BIGINT,
    usuario_id BIGINT,
    CONSTRAINT fk_movimientos_producto FOREIGN KEY (producto_id) REFERENCES productos(id),
    CONSTRAINT fk_movimientos_lote FOREIGN KEY (lote_id) REFERENCES lotes(id)
);

CREATE INDEX ix_movimientos_producto_fecha ON movimientos_inventario (producto_id, fecha);
CREATE INDEX ix_movimientos_compra ON movimientos_inventario (compra_id);
CREATE INDEX ix_movimientos_venta ON movimientos_inventario (venta_id);
