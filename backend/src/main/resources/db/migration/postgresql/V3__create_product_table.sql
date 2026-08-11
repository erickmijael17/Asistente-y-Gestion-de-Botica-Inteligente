CREATE TABLE productos (
    id BIGSERIAL PRIMARY KEY,
    codigo_interno VARCHAR(60) NOT NULL,
    codigo_barras VARCHAR(80),
    nombre_comercial VARCHAR(180) NOT NULL,
    nombre_generico VARCHAR(180),
    descripcion VARCHAR(700),
    tipo_producto VARCHAR(30) NOT NULL,
    principio_activo VARCHAR(180),
    concentracion VARCHAR(100),
    presentacion VARCHAR(120),
    via_administracion VARCHAR(100),
    indicaciones_oficiales VARCHAR(1000),
    contraindicaciones VARCHAR(1000),
    advertencias VARCHAR(1000),
    precauciones VARCHAR(1000),
    condiciones_almacenamiento VARCHAR(500),
    requiere_receta BOOLEAN NOT NULL DEFAULT FALSE,
    registro_sanitario VARCHAR(100),
    precio_compra NUMERIC(12,2) NOT NULL,
    precio_venta NUMERIC(12,2) NOT NULL,
    stock_minimo INTEGER NOT NULL DEFAULT 0,
    imagen_url VARCHAR(500),
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    categoria_id BIGINT NOT NULL,
    laboratorio_id BIGINT NOT NULL,
    fuente_informacion VARCHAR(200),
    fecha_actualizacion_informacion DATE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_productos_categoria FOREIGN KEY (categoria_id) REFERENCES categorias (id),
    CONSTRAINT fk_productos_laboratorio FOREIGN KEY (laboratorio_id) REFERENCES laboratorios (id),
    CONSTRAINT ck_productos_tipo_producto CHECK (tipo_producto IN ('MEDICAMENTO', 'SUPLEMENTO', 'VITAMINA', 'OTRO')),
    CONSTRAINT ck_productos_precio_compra CHECK (precio_compra >= 0),
    CONSTRAINT ck_productos_precio_venta CHECK (precio_venta > 0),
    CONSTRAINT ck_productos_precio_venta_compra CHECK (precio_venta >= precio_compra),
    CONSTRAINT ck_productos_stock_minimo CHECK (stock_minimo >= 0)
);

CREATE UNIQUE INDEX uk_productos_codigo_interno_lower ON productos (lower(codigo_interno));
CREATE UNIQUE INDEX uk_productos_codigo_barras ON productos (codigo_barras) WHERE codigo_barras IS NOT NULL;
CREATE INDEX idx_productos_codigo_interno ON productos (codigo_interno);
CREATE INDEX idx_productos_codigo_barras ON productos (codigo_barras);
CREATE INDEX idx_productos_nombre_comercial ON productos (nombre_comercial);
CREATE INDEX idx_productos_nombre_generico ON productos (nombre_generico);
CREATE INDEX idx_productos_principio_activo ON productos (principio_activo);
CREATE INDEX idx_productos_registro_sanitario ON productos (registro_sanitario);
CREATE INDEX idx_productos_categoria_id ON productos (categoria_id);
CREATE INDEX idx_productos_laboratorio_id ON productos (laboratorio_id);
CREATE INDEX idx_productos_estado ON productos (estado);
