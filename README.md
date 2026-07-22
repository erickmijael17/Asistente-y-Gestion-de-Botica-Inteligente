# Asistente y Gestión de Botica Inteligente

Backend monolítico modular para el sistema integral de una botica.

## Visión del Proyecto
El sistema permite gestionar **ventas**, **inventario**, generar **reportes** y administrar **alertas**. Está diseñado para dos tipos de usuarios:
- **Gerente (Dueño)**: Supervisa reportes, ventas e inventario.
- **Vendedor (Farmacéutico)**: Realiza ventas y atiende a los clientes.

Adicionalmente, el sistema cuenta con un **Chatbot IA** conectado directamente a nuestra base de datos. Este chatbot permite consultar información médica (ej. "pastillas para la fiebre") y devuelve un abanico de alternativas médicas basadas en nuestro inventario. De este modo, los vendedores pueden ofrecer mayor variedad de productos a los usuarios en lugar de limitarse siempre a los mismos medicamentos conocidos.

[Ver Modelado de Arquitectura C4](docs/architecture/c4-model.md)

## Tecnologias

- Java 22
- Spring Boot 3.3.5
- Spring Web, Data JPA, Validation, Security y OAuth 2.0 Resource Server
- Keycloak
- PostgreSQL
- Flyway
- Lombok
- MapStruct
- Springdoc OpenAPI / Swagger
- JUnit 5, Mockito y Testcontainers
- Docker Compose

## Levantar infraestructura

1. Crear archivo `.env` desde `.env.example`.
2. Levantar PostgreSQL y Keycloak:

```bash
docker compose up -d
```

Servicios:

- PostgreSQL: `localhost:5432`, base `botica_inteligente_db`
- Keycloak: `http://localhost:8081`
- Realm importado: `botica-inteligente`
- Roles: `OWNER`, `SELLER`
- Clientes: `botica-backend`, `botica-frontend`

## Ejecutar backend

Requisito: JDK 22 activo en `JAVA_HOME`.

```bash
mvn clean test
mvn spring-boot:run
```

Perfil por defecto: `dev`.

Swagger:

```text
http://localhost:8080/swagger-ui.html
```

Health:

```text
http://localhost:8080/actuator/health
```

## Variables de entorno principales

```text
SPRING_PROFILES_ACTIVE=dev
DB_URL=jdbc:postgresql://localhost:5432/botica_inteligente_db
DB_USERNAME=botica_user
DB_PASSWORD=botica_password
KEYCLOAK_ISSUER_URI=http://localhost:8081/realms/botica-inteligente
CORS_ALLOWED_ORIGINS=http://localhost:4200
```

## Endpoints de fase 1

Categorias:

- `GET /api/v1/categorias`
- `GET /api/v1/categorias/{id}`
- `POST /api/v1/categorias`
- `PUT /api/v1/categorias/{id}`
- `PATCH /api/v1/categorias/{id}/estado`

Laboratorios:

- `GET /api/v1/laboratorios`
- `GET /api/v1/laboratorios/{id}`
- `POST /api/v1/laboratorios`
- `PUT /api/v1/laboratorios/{id}`
- `PATCH /api/v1/laboratorios/{id}/estado`

Productos:

- `GET /api/v1/productos`
- `GET /api/v1/productos/{id}`
- `GET /api/v1/productos/codigo-barras/{codigoBarras}`
- `POST /api/v1/productos`
- `PUT /api/v1/productos/{id}`
- `PATCH /api/v1/productos/{id}/estado`

## Seguridad

El backend valida JWT emitidos por Keycloak como OAuth 2.0 Resource Server.

Rutas publicas:

- `/actuator/health`
- `/v3/api-docs/**`
- `/swagger-ui/**`
- `/swagger-ui.html`

Reglas:

- `GET` de categorias, laboratorios y productos: `OWNER` o `SELLER`
- `POST`, `PUT`, `PATCH`: solo `OWNER`

Los roles de Keycloak se leen desde `realm_access.roles` y `resource_access`, y se convierten a `ROLE_OWNER` y `ROLE_SELLER`.

## Migraciones

- `V1__create_base_tables.sql`: `usuario_referencia`
- `V2__create_catalog_tables.sql`: `categorias`, `laboratorios`
- `V3__create_product_table.sql`: `productos`
- `V4__insert_initial_catalog_data.sql`: categorias generales y laboratorios demo
- `V5__create_ventas_tables.sql`: Tablas de transacciones de ventas y detalles

## Alcance Actual
Hasta la fecha se encuentran implementados los módulos Base (Usuarios, Seguridad, Keycloak), Catálogo (Categorías, Laboratorios, Productos) y Transaccional (Ventas). 

*Pendiente de implementar: Inventario, Lotes, Compras, Proveedores, Clientes, Pagos, Reportes y el Chatbot IA.*
