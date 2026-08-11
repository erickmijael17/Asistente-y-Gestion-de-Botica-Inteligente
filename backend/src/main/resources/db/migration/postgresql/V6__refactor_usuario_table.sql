ALTER TABLE usuario_referencia RENAME TO usuario;

ALTER TABLE usuario DROP CONSTRAINT uk_usuario_referencia_keycloak_user_id;
ALTER TABLE usuario DROP COLUMN keycloak_user_id;

ALTER TABLE usuario ADD COLUMN password VARCHAR(255) NOT NULL DEFAULT '';
ALTER TABLE usuario ADD COLUMN roles VARCHAR(255) NOT NULL DEFAULT 'ROLE_SELLER';

ALTER TABLE usuario ADD CONSTRAINT uk_usuario_username UNIQUE (username);
