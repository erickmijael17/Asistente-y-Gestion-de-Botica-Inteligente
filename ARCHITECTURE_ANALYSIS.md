# Análisis de Arquitectura del Proyecto

Este documento describe las decisiones arquitectónicas implementadas en el backend del proyecto **Asistente y Gestión de Botica Inteligente**.

## 1. Patrón Arquitectónico: Monolito Modular
El backend se ha diseñado como un **Monolito Modular** utilizando Spring Boot.

### Decisiones Clave de Diseño:
- **Separación por Dominio:** El código está organizado en paquetes según la funcionalidad de negocio (`usuario`, `categoria`, `laboratorio`, `producto`, `venta`), en lugar de una separación técnica (todos los controladores en un paquete, etc.).
- **Capas Internas por Módulo:** Dentro de cada módulo, se respeta una arquitectura en capas tradicional:
    - `controller`: Expone la API REST. No contiene lógica de negocio. Depende únicamente de los DTOs y de la capa `service`.
    - `dto`: Modelos para transferencia de datos (`request` y `response`). Se evita exponer las entidades de base de datos directamente al cliente.
    - `entity`: Clases mapeadas a la base de datos (JPA).
    - `repository`: Interfaces de acceso a datos (Spring Data JPA).
    - `service`: Contiene la lógica de negocio, validaciones y orquestación. Interactúa con repositorios y conversores.
    - `mapper`: Utiliza **MapStruct** para las conversiones eficientes y de tipo seguro entre DTOs y Entidades.
    - `specification`: Facilita la creación de consultas dinámicas (filtros) con JPA Specifications.
- **Inyección de Dependencias:** Se promueve la inyección por constructor sobre la inyección de campos (Field Injection con `@Autowired`), lo que favorece la inmutabilidad y la facilidad para realizar pruebas unitarias.
- **Infraestructura Transversal (`shared` y `config`):**
    - Contiene configuraciones globales como CORS (`CorsProperties`, `SecurityConfig`), manejo global de excepciones (`GlobalExceptionHandler`), y respuestas estandarizadas (`ApiResponse`, `PageResponse`, `ApiErrorResponse`).
    - Las entidades extienden de `AuditableEntity` (usando JPA Auditing) para registrar fechas de creación y modificación (`LocalDateTime`).
- **Base de Datos y Migraciones:**
    - Se utiliza PostgreSQL.
    - El esquema y los datos iniciales se gestionan mediante migraciones incrementales con **Flyway** (`V1__...sql`, `V2__...sql`, etc.), asegurando un control de versiones de la base de datos robusto. El modelo físico respeta eliminación lógica de registros.
- **Pruebas:**
    - Pruebas unitarias robustas utilizando **JUnit 5** y **Mockito** (por ejemplo, para la capa de servicios).
    - Pruebas de integración para las migraciones y base de datos con **Testcontainers**.

## 2. Integración con Keycloak y Seguridad (Fase 1)
El proyecto ha delegado la responsabilidad de autenticación y gestión de identidades a **Keycloak**. El backend de Spring Boot actúa exclusivamente como un **OAuth 2.0 Resource Server**.

### Implementación y Funcionamiento:
- **Flujo de Seguridad:**
    - Los clientes (e.g., Frontend Angular) se autentican directamente contra Keycloak y obtienen un JWT (JSON Web Token).
    - El cliente envía las peticiones REST al backend adjuntando el JWT en el encabezado `Authorization: Bearer <token>`.
    - La configuración en `SecurityConfig.java` intercepta la petición y valida la firma del JWT usando la URI del emisor (issuer-uri configurada en `application.yml`: `KEYCLOAK_ISSUER_URI`).
- **Conversión de Roles (`KeycloakRoleConverter`):**
    - Keycloak puede incluir roles en diferentes claims del JWT, como `realm_access.roles` (roles a nivel de reino) y `resource_access` (roles a nivel de cliente).
    - La clase `KeycloakRoleConverter` (que implementa `Converter<Jwt, Collection<GrantedAuthority>>`) se encarga de extraer ambos tipos de roles del JWT recibido.
    - Procesa estos roles y filtra específicamente los que son relevantes para la aplicación (`OWNER` y `SELLER`).
    - Convierte estos roles en el formato interno de autoridades de Spring Security añadiendo el prefijo `ROLE_` (resultando en `ROLE_OWNER` y `ROLE_SELLER`).
- **Políticas de Autorización (`SecurityConfig`):**
    - Se establecen reglas granulares por HTTP Method:
        - Endpoints públicos (sin autenticación requerida): Swagger (`/v3/api-docs/**`, `/swagger-ui/**`), Actuator (`/actuator/health`).
        - Endpoints de lectura (HTTP `GET` en categorías, laboratorios, productos): Accesibles por roles `OWNER` o `SELLER`.
        - Endpoints de modificación (HTTP `POST`, `PUT`, `PATCH`): Restringidos únicamente al rol `OWNER`.
- **Integración con CORS:** Se ha configurado cuidadosamente la política de orígenes cruzados en base a las propiedades (ej. `CORS_ALLOWED_ORIGINS=http://localhost:4200` desde variables de entorno) permitiendo integración segura con el cliente frontend.

---
El proyecto exhibe prácticas sólidas en el diseño arquitectónico y una integración de seguridad estándar en la industria, separando las responsabilidades de identidad (Keycloak) de las de dominio de negocio del monolito.
