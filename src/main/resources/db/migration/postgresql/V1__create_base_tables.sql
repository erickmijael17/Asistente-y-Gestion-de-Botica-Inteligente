CREATE TABLE usuario_referencia (
    id BIGSERIAL PRIMARY KEY,
    keycloak_user_id VARCHAR(100) NOT NULL,
    username VARCHAR(100) NOT NULL,
    nombres VARCHAR(150),
    apellidos VARCHAR(150),
    estado BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_usuario_referencia_keycloak_user_id UNIQUE (keycloak_user_id)
);

CREATE INDEX idx_usuario_referencia_estado ON usuario_referencia (estado);
