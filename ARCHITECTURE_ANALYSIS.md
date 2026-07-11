# Análisis Arquitectónico y de Integración con Keycloak

## 1. Arquitectura del Proyecto
El proyecto "Asistente y Gestión de Botica Inteligente" está construido bajo una arquitectura **Monolítica Modular** utilizando el framework Spring Boot (versión 3.3.5) y Java 22.

### Características de la Arquitectura:
- **Organización Modular:** El código fuente se estructura en módulos funcionales, tales como `usuario`, `categoria`, `laboratorio`, y `producto`. Cada módulo agrupa las responsabilidades asociadas a su contexto de dominio.
- **Capas por Módulo:** Dentro de cada módulo, se aplican los principios de separación de responsabilidades en las siguientes capas (paquetes):
    - `controller`: Expone los endpoints de la API REST (Spring Web).
    - `service`: Contiene la lógica de negocio y validaciones. Los controladores interactúan exclusivamente con los servicios.
    - `repository`: Interfaces de acceso a datos utilizando Spring Data JPA. No contienen lógica de negocio.
    - `entity`: Clases que mapean el modelo relacional a la base de datos.
    - `dto`: Objetos de Transferencia de Datos (`request` y `response`) para evitar la exposición directa de las entidades de dominio a través de la API.
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