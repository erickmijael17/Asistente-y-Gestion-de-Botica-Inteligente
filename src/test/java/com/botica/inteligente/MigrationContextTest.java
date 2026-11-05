package com.botica.inteligente;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

@SpringBootTest
class MigrationContextTest {

    private static final Path TEMPORARY_DATABASE = createTemporaryDatabasePath();

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private Flyway flyway;

    @DynamicPropertySource
    static void configureTemporarySqlite(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", () -> "jdbc:sqlite:" + TEMPORARY_DATABASE);
    }

    @Test
    void startsContextAndAppliesInitialSchemaMigration() {
        List<String> tableNames = jdbcTemplate.queryForList(
            "SELECT name FROM sqlite_master WHERE type = 'table'",
            String.class
        );
        List<String> normalizedTableNames = tableNames.stream()
            .map(name -> name.toLowerCase(Locale.ROOT))
            .toList();

        assertEquals(2, flyway.info().applied().length);
        assertTrue(normalizedTableNames.containsAll(
            List.of("usuarios", "categorias", "laboratorios", "productos")
        ));

        List<String> productColumns = jdbcTemplate.query(
            "PRAGMA table_info(productos)",
            (resultSet, rowNumber) -> resultSet.getString("name")
        );
        assertFalse(productColumns.contains("ean13"));
        assertFalse(productColumns.contains("stock"));
    }

    private static Path createTemporaryDatabasePath() {
        try {
            return Files.createTempDirectory("botica-sqlite-")
                .resolve("botica-" + UUID.randomUUID() + ".db");
        } catch (IOException exception) {
            throw new IllegalStateException("No se pudo crear la carpeta de base de datos temporal", exception);
        }
    }
}
