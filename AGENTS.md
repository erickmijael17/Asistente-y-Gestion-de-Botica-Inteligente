# AGENTS.md

Guia para agentes y colaboradores que trabajen en este repositorio.

## Contexto del proyecto

Este proyecto es el backend de **Asistente y Gestión de Botica Inteligente**, un sistema web interno para una botica. 

**Visión Global:**
El sistema controlará ventas, inventario, reportes y un sistema de alertas. Existen 2 tipos de usuarios: **Gerente** (dueño) y **Vendedores** (farmacéuticos). Además, el proyecto incluye un **Chatbot IA** conectado directamente a la base de datos de productos. El chatbot será capaz de extraer información médica y sugerir alternativas; por ejemplo, si se le pregunta por "pastillas para la fiebre", buscará en el inventario y brindará opciones, permitiendo al vendedor ofrecer alternativas variadas en lugar de vender siempre el mismo medicamento.

El backend es un **monolito modular** en Spring Boot. No convertir a microservicios.

## Stack obligatorio

- Java 22
- Spring Boot
- Maven
- Spring Web
- Spring Data JPA
- Spring Security
- OAuth 2.0 Resource Server
- Keycloak
- PostgreSQL
- Flyway
- Bean Validation
- Lombok
- MapStruct
- Springdoc OpenAPI / Swagger
- JUnit 5
- Mockito
- Testcontainers
- Docker Compose

## Paquete base

Usar siempre:

```text
com.botica.inteligente
```

Clase principal:

```text
BoticaInteligenteApplication
```

## Arquitectura

Mantener una arquitectura monolitica modular organizada por funcionalidad:

```text
src/main/java/com/botica/inteligente
├── config
├── security
├── shared
│   ├── audit
│   ├── exception
│   ├── response
│   ├── validation
│   └── util
├── usuario
├── categoria
├── laboratorio
└── producto
```

Cada modulo funcional debe seguir esta estructura cuando aplique:

```text
modulo
├── controller
├── dto
│   ├── request
│   └── response
├── entity
├── mapper
├── repository
├── service
├── specification
└── enums
```

No crear carpetas globales para todos los controllers, entities o repositories. No usar carpeta `model` para entidades.

## Alcance actual

La fase actual incluye solo:

- Configuracion general
- Seguridad con Keycloak
- Respuestas estandar
- Manejo global de excepciones
- Auditoria de fechas
- Usuario de referencia de Keycloak
- Categorias
- Laboratorios
- Productos
- Ventas
- Migraciones Flyway
- Swagger
- Pruebas
- Docker Compose para PostgreSQL y Keycloak

No implementar todavia:

- Inventario
- Lotes
- Compras
- Proveedores
- Clientes
- Pagos
- Reportes
- Chatbot

## Reglas de codigo

- No exponer entidades JPA directamente desde controllers.
- Usar DTOs de request y response.
- Usar MapStruct para conversiones entre entidades y DTOs.
- Los controllers no deben contener logica de negocio.
- Los services deben manejar validaciones, reglas de negocio, relaciones y persistencia.
- Los repositories no deben contener logica de negocio.
- Usar inyeccion por constructor.
- No usar `@Autowired` en atributos.
- Usar `BigDecimal` para precios.
- Usar `LocalDate` para fechas de informacion de producto.
- Usar `LocalDateTime` para auditoria.
- Usar paginacion en listados.
- Usar `Specification` para filtros dinamicos.
- Usar eliminacion logica mediante `estado`.
- No usar `System.out.println`.
- Usar SLF4J para logs.
- No dejar `TODO`.
- No incluir secretos reales.
- No usar `ddl-auto: create` ni `ddl-auto: update`.
- Mantener `ddl-auto: validate`.

## Seguridad

Keycloak es el proveedor de identidad. Spring Boot funciona como OAuth 2.0 Resource Server.

Roles validos:

```text
OWNER
SELLER
```

Autoridades Spring esperadas:

```text
ROLE_OWNER
ROLE_SELLER
```

Los roles deben extraerse desde:

- `realm_access.roles`
- `resource_access`

Rutas publicas permitidas:

```text
/actuator/health
/v3/api-docs/**
/swagger-ui/**
/swagger-ui.html
```

Reglas generales:

- `GET` en categorias, laboratorios y productos: `OWNER` o `SELLER`
- `POST`, `PUT`, `PATCH`: solo `OWNER`

Tambien usar `@PreAuthorize` en metodos sensibles.

No almacenar contrasenas en PostgreSQL.

## Base de datos y migraciones

Usar Flyway para cambios de esquema.

Ubicacion:

```text
src/main/resources/db/migration
```

Migraciones existentes:

```text
V1__create_base_tables.sql
V2__create_catalog_tables.sql
V3__create_product_table.sql
V4__insert_initial_catalog_data.sql
V5__create_ventas_tables.sql
```

Para nuevas migraciones, crear una version incremental `V6__...sql`, `V7__...sql`, etc. No modificar migraciones ya aplicadas salvo que el usuario lo pida explicitamente y el entorno sea de desarrollo descartable.

## Configuracion

Perfiles:

```text
application.yml
application-dev.yml
application-test.yml
application-prod.yml
```

No escribir contrasenas reales en archivos del repositorio. Usar variables de entorno.

Variables principales:

```text
SPRING_PROFILES_ACTIVE
DB_URL
DB_USERNAME
DB_PASSWORD
KEYCLOAK_ISSUER_URI
CORS_ALLOWED_ORIGINS
```

## Docker

Infraestructura local:

```bash
docker compose up -d
```

Servicios:

- `botica-postgres`
- `botica-keycloak`

PostgreSQL:

```text
localhost:5432
botica_inteligente_db
```

Keycloak:

```text
http://localhost:8081
realm: botica-inteligente
```

## Validacion

El proyecto esta configurado para Java 22. Antes de validar, asegurar que `JAVA_HOME` apunte a un JDK 22.

Comandos esperados:

```bash
mvn clean compile
mvn clean test
mvn spring-boot:run
```

Swagger local:

```text
http://localhost:8080/swagger-ui.html
```

Health check:

```text
http://localhost:8080/actuator/health
```

## Pruebas

Mantener pruebas para:

- Services de categorias
- Services de laboratorios
- Services de productos
- Seguridad de endpoints
- Migraciones con PostgreSQL/Testcontainers

No usar H2 para simular PostgreSQL.

Si Testcontainers falla por Docker no disponible, reportar la causa exacta y no reemplazarlo por H2.

## Git y archivos locales

Mantener ignorados:

```gitignore
.idea/
*.iml
target/
.env
```

No versionar:

- `.env`
- secretos
- archivos `.iml`
- salida de build

## Criterio para cambios futuros

Antes de modificar:

1. Revisar estructura actual.
2. Revisar `pom.xml` y perfiles si el cambio toca configuracion.
3. Identificar archivos existentes relacionados.
4. Evitar reemplazar codigo funcional sin motivo.
5. Mantener el alcance de la fase actual.
6. Ejecutar pruebas relevantes.
7. Documentar limitaciones del entorno si alguna validacion no puede ejecutarse.
