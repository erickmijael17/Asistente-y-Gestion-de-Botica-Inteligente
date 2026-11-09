CREATE TABLE productos_nuevo (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
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
    requiere_receta BOOLEAN NOT NULL DEFAULT 0,
    registro_sanitario VARCHAR(100),
    precio_compra NUMERIC(12, 2) NOT NULL,
    precio_venta NUMERIC(12, 2) NOT NULL,
    stock_minimo INTEGER NOT NULL,
    imagen_url VARCHAR(500),
    estado BOOLEAN NOT NULL DEFAULT 1,
    categoria_id BIGINT NOT NULL,
    laboratorio_id BIGINT,
    fuente_informacion VARCHAR(200),
    fecha_actualizacion_informacion DATE,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL,
    costo_promedio NUMERIC(12, 4),
    CONSTRAINT fk_productos_categoria FOREIGN KEY (categoria_id) REFERENCES categorias(id),
    CONSTRAINT fk_productos_laboratorio FOREIGN KEY (laboratorio_id) REFERENCES laboratorios(id)
);

INSERT INTO productos_nuevo (
    id, codigo_interno, codigo_barras, nombre_comercial, nombre_generico, descripcion,
    tipo_producto, principio_activo, concentracion, presentacion, via_administracion,
    indicaciones_oficiales, contraindicaciones, advertencias, precauciones, condiciones_almacenamiento,
    requiere_receta, registro_sanitario, precio_compra, precio_venta, stock_minimo, imagen_url,
    estado, categoria_id, laboratorio_id, fuente_informacion, fecha_actualizacion_informacion,
    fecha_creacion, fecha_actualizacion, costo_promedio
)
SELECT
    id, codigo_interno, codigo_barras, nombre_comercial, nombre_generico, descripcion,
    tipo_producto, principio_activo, concentracion, presentacion, via_administracion,
    indicaciones_oficiales, contraindicaciones, advertencias, precauciones, condiciones_almacenamiento,
    requiere_receta, registro_sanitario, precio_compra, precio_venta, stock_minimo, imagen_url,
    estado, categoria_id, laboratorio_id, fuente_informacion, fecha_actualizacion_informacion,
    fecha_creacion, fecha_actualizacion, costo_promedio
FROM productos;

DROP TABLE productos;
ALTER TABLE productos_nuevo RENAME TO productos;

CREATE UNIQUE INDEX ux_productos_codigo_barras_no_nulo
    ON productos (codigo_barras)
    WHERE codigo_barras IS NOT NULL;
CREATE INDEX ix_productos_categoria_id ON productos (categoria_id);
CREATE INDEX ix_productos_laboratorio_id ON productos (laboratorio_id);
CREATE UNIQUE INDEX ux_productos_codigo_interno ON productos (codigo_interno COLLATE NOCASE);
CREATE INDEX ix_productos_nombre_comercial ON productos (nombre_comercial COLLATE NOCASE);
