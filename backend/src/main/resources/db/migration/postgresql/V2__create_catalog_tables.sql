CREATE TABLE categorias (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    descripcion VARCHAR(500),
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE UNIQUE INDEX uk_categorias_nombre_lower ON categorias (lower(nombre));
CREATE INDEX idx_categorias_nombre ON categorias (nombre);
CREATE INDEX idx_categorias_estado ON categorias (estado);

CREATE TABLE laboratorios (
    id BIGSERIAL PRIMARY KEY,
    nombre VARCHAR(150) NOT NULL,
    ruc VARCHAR(20),
    telefono VARCHAR(30),
    correo VARCHAR(150),
    direccion VARCHAR(250),
    sitio_web VARCHAR(150),
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_laboratorios_correo CHECK (correo IS NULL OR correo ~* '^[A-Z0-9._%+-]+@[A-Z0-9.-]+\.[A-Z]{2,}$')
);

CREATE UNIQUE INDEX uk_laboratorios_nombre_lower ON laboratorios (lower(nombre));
CREATE UNIQUE INDEX uk_laboratorios_ruc ON laboratorios (ruc) WHERE ruc IS NOT NULL;
CREATE INDEX idx_laboratorios_nombre ON laboratorios (nombre);
CREATE INDEX idx_laboratorios_ruc ON laboratorios (ruc);
CREATE INDEX idx_laboratorios_estado ON laboratorios (estado);
