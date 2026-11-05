package com.botica.inteligente.config;

import javax.sql.DataSource;
import org.flywaydb.core.Flyway;
import org.springframework.boot.autoconfigure.flyway.FlywayMigrationInitializer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
public class FlywayConfiguration {

    // Evita llamadas a la API de Flyway que Spring Boot 3.3.4 ya no encuentra en Flyway 13.
    @Bean
    public Flyway flyway(DataSource dataSource) {
        return Flyway.configure()
            .dataSource(dataSource)
            .locations("classpath:db/migration")
            .load();
    }

    @Bean
    public FlywayMigrationInitializer flywayMigrationInitializer(Flyway flyway) {
        return new FlywayMigrationInitializer(flyway, Flyway::migrate);
    }
}
