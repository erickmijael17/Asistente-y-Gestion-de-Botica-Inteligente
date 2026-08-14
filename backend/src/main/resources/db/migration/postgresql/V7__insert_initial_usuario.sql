INSERT INTO usuario (username, password, nombres, apellidos, roles, estado)
VALUES ('gerente.botica', '$2a$10$wO08e/w66/0Q8Fm9nC5Xye2gOQjO3QZ.jT.n75s/2f8W.wA9090rS', 'Gerente', 'Principal', 'ROLE_OWNER', true)
ON CONFLICT (username) DO UPDATE 
SET password = EXCLUDED.password, roles = EXCLUDED.roles;
