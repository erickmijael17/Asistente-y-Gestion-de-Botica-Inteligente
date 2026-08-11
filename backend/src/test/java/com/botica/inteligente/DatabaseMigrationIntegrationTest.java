package com.botica.inteligente;

import static org.assertj.core.api.Assertions.assertThat;

import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest
@ActiveProfiles("test")
class DatabaseMigrationIntegrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:16")
            .withDatabaseName("botica_inteligente_test")
            .withUsername("test")
            .withPassword("test");

    @Autowired
    private DataSource dataSource;

    @DynamicPropertySource
    static void registerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Test
    void flywayCreatesCatalogTablesAndInitialData() {
        JdbcTemplate jdbcTemplate = new JdbcTemplate(dataSource);

        Integer categorias = jdbcTemplate.queryForObject("select count(*) from categorias", Integer.class);
        Integer laboratorios = jdbcTemplate.queryForObject("select count(*) from laboratorios", Integer.class);

        assertThat(categorias).isGreaterThanOrEqualTo(6);
        assertThat(laboratorios).isGreaterThanOrEqualTo(2);
    }
}
