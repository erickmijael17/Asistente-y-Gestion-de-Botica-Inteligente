CREATE TABLE usuarios (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    username VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    roles VARCHAR(20) NOT NULL,
    nombres VARCHAR(150),
    apellidos VARCHAR(150),
    estado BOOLEAN NOT NULL DEFAULT 1,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL
);

CREATE TABLE categorias (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500),
    estado BOOLEAN NOT NULL DEFAULT 1,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL
);

CREATE TABLE laboratorios (
    id INTEGER NOT NULL PRIMARY KEY AUTOINCREMENT,
    nombre VARCHAR(150) NOT NULL,
    ruc VARCHAR(20),
    telefono VARCHAR(30),
    correo VARCHAR(150),
    direccion VARCHAR(250),
    sitio_web VARCHAR(150),
    estado BOOLEAN NOT NULL DEFAULT 1,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL
);

CREATE TABLE productos (
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
    laboratorio_id BIGINT NOT NULL,
    fuente_informacion VARCHAR(200),
    fecha_actualizacion_informacion DATE,
    fecha_creacion TIMESTAMP NOT NULL,
    fecha_actualizacion TIMESTAMP NOT NULL,
    CONSTRAINT fk_productos_categoria FOREIGN KEY (categoria_id) REFERENCES categorias(id),
    CONSTRAINT fk_productos_laboratorio FOREIGN KEY (laboratorio_id) REFERENCES laboratorios(id)
);

CREATE UNIQUE INDEX ux_productos_codigo_barras_no_nulo
    ON productos (codigo_barras)
    WHERE codigo_barras IS NOT NULL;

CREATE INDEX ix_productos_categoria_id ON productos (categoria_id);
CREATE INDEX ix_productos_laboratorio_id ON productos (laboratorio_id);
