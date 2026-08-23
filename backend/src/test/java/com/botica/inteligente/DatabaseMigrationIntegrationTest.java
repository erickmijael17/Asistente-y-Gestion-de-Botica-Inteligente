package com.botica.inteligente;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Verifica que Hibernate ddl-auto:update crea el esquema via @Entity.
 * Requiere PostgreSQL nativo en localhost:5432 con BD botica_inteligente_db
 * creada via backend/scripts/init-db.sql (ahora sin Flyway, las tablas se generan por JPA).
 */
@SpringBootTest
class DatabaseMigrationIntegrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void hibernateCreatesTablesViaEntities() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        // Verifica que las tablas existen (ddl-auto:update las crea desde @Entity)
        Integer categoriasTable = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema='botica' AND table_name='categorias'",
                Integer.class);
        Integer productosTable = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema='botica' AND table_name='productos'",
                Integer.class);
        Integer usuarioTable = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema='botica' AND table_name='usuario'",
                Integer.class);
        Integer ventasTable = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM information_schema.tables WHERE table_schema='botica' AND table_name='ventas'",
                Integer.class);

        assertThat(categoriasTable).isEqualTo(1);
        assertThat(productosTable).isEqualTo(1);
        assertThat(usuarioTable).isEqualTo(1);
        assertThat(ventasTable).isEqualTo(1);

        // Las tablas pueden estar vacias (sin seed), solo verifica que son consultables
        Integer categoriasCount = jdbcTemplate.queryForObject("select count(*) from botica.categorias", Integer.class);
        assertThat(categoriasCount).isGreaterThanOrEqualTo(0);
    }
}
