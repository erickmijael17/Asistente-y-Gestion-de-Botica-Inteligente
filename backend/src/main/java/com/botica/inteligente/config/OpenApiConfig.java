package com.botica.inteligente.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {

    @Bean
    public OpenAPI boticaOpenApi() {
        String schemeName = "bearer-jwt";
        return new OpenAPI()
                .info(new Info()
                        .title("Asistente y Gestion de Botica Inteligente")
                        .description("API interna para gestion inicial de categorias, laboratorios y productos. Seguridad con Keycloak y roles OWNER/SELLER.")
                        .version("1.0.0-fase-1"))
                .addSecurityItem(new SecurityRequirement().addList(schemeName))
                .components(new Components().addSecuritySchemes(schemeName,
                        new SecurityScheme()
                                .name(schemeName)
                                .type(SecurityScheme.Type.HTTP)
                                .scheme("bearer")
                                .bearerFormat("JWT")));
    }
}
