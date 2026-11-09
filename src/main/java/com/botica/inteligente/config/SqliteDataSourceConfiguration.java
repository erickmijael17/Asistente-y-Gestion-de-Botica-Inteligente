package com.botica.inteligente.config;

import com.zaxxer.hikari.HikariDataSource;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.autoconfigure.jdbc.DataSourceProperties;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(DataSourceProperties.class)
public class SqliteDataSourceConfiguration {

    @Bean
    @ConfigurationProperties("spring.datasource.hikari")
    public HikariDataSource dataSource(DataSourceProperties properties) throws IOException {
        ensureSqliteDirectory(properties.getUrl());
        return properties.initializeDataSourceBuilder()
            .type(HikariDataSource.class)
            .build();
    }

    private void ensureSqliteDirectory(String jdbcUrl) throws IOException {
        String sqlitePrefix = "jdbc:sqlite:";
        if (jdbcUrl == null || !jdbcUrl.startsWith(sqlitePrefix)) {
            return;
        }

        String databaseLocation = jdbcUrl.substring(sqlitePrefix.length()).split("\\?", 2)[0];
        if (databaseLocation.isBlank() || databaseLocation.equals(":memory:")
            || databaseLocation.startsWith("file:")) {
            return;
        }

        Path parentDirectory = Path.of(databaseLocation).toAbsolutePath().getParent();
        if (parentDirectory != null) {
            Files.createDirectories(parentDirectory);
        }
    }
}
