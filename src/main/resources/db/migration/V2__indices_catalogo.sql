CREATE UNIQUE INDEX ux_categorias_nombre ON categorias (nombre COLLATE NOCASE);

CREATE UNIQUE INDEX ux_laboratorios_nombre ON laboratorios (nombre COLLATE NOCASE);

CREATE UNIQUE INDEX ux_laboratorios_ruc_no_nulo ON laboratorios (ruc) WHERE ruc IS NOT NULL;

CREATE UNIQUE INDEX ux_productos_codigo_interno ON productos (codigo_interno COLLATE NOCASE);

CREATE INDEX ix_productos_nombre_comercial ON productos (nombre_comercial COLLATE NOCASE);
