# Asistente y Gestión de Botica Inteligente

Sistema web para la gestión integral de una botica, organizado en dos componentes:

- `backend/` — Backend monolítico modular en Spring Boot
- `frontend/` — Aplicación web Angular (por el momento vacía)

## Visión del Proyecto
El sistema permite gestionar **ventas**, **inventario**, generar **reportes** y administrar **alertas**. Está diseñado para dos tipos de usuarios:
- **Gerente (Dueño)**: Supervisa reportes, ventas e inventario.
- **Vendedor (Farmacéutico)**: Realiza ventas y atiende a los clientes.

Adicionalmente, el sistema cuenta con un **Chatbot IA** conectado directamente a nuestra base de datos. Este chatbot permite consultar información médica (ej. "pastillas para la fiebre") y devuelve un abanico de alternativas médicas basadas en nuestro inventario. De este modo, los vendedores pueden ofrecer mayor variedad de productos a los usuarios en lugar de limitarse siempre a los mismos medicamentos conocidos.

[Ver Modelado de Arquitectura C4](backend/docs/architecture/c4-model.md)

## Tecnologias

- Java 22
- Spring Boot 3.3.5
- Spring Web, Data JPA, Validation, Security y OAuth 2.0 Resource Server
- Keycloak
- PostgreSQL 16 (Docker)
- Lombok
- MapStruct
- Springdoc OpenAPI / Swagger
- JUnit 5, Mockito y Testcontainers
- Docker Compose (dev monolito)

## Levantar infraestructura

1. Crear archivo `.env` desde `backend/.env.example`.
2. Levantar PostgreSQL y Keycloak (desde `backend/`):

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

Requisito: JDK 22 activo en `JAVA_HOME` y Docker `botica-postgres`/`botica-keycloak` arriba (`docker compose up -d`).

```bash
mvn clean compile
mvn spring-boot:run
```

Config unica: `backend/src/main/resources/application.yml` (sin perfiles, `resources` limpio). Ejecutar dentro de `backend/`.

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

## Base de datos (dev)

Tablas generadas via `@Entity` + `ddl-auto:update` con `single application.yml` (sin Flyway, `resources` limpio). Cada modulo define su `@Entity`/`@Table` en `*/entity/*.java`.

Al arrancar contra Docker `botica-postgres` (`localhost:5432`, `schema botica`) Hibernate crea `categorias`, `laboratorios`, `productos`, `usuario_referencia`, `ventas`, `venta_detalles` automaticamente. Para prod futuro se puede reintroducir Flyway con `validate`.

## Alcance Actual
Hasta la fecha se encuentran implementados los módulos Base (Usuarios, Seguridad, Keycloak), Catálogo (Categorías, Laboratorios, Productos) y Transaccional (Ventas). 

*Pendiente de implementar: Inventario, Lotes, Compras, Proveedores, Clientes, Pagos, Reportes y el Chatbot IA.*
