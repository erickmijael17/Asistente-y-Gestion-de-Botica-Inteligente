# AGENTS.md

Guia para agentes y colaboradores. Plan, fases y reglas de negocio: `MIGRACION.md`.

## Proyecto

**Asistente y Gestion de Botica Inteligente**: aplicacion de escritorio (rama `escritorio`) para una botica: inventario con lotes y vencimientos, catalogo maestro con EAN-13, ventas, dashboard y asistente IA. Perfiles: `OWNER` y `SELLER`.

El proyecto web anterior (Spring REST + React + PostgreSQL + Keycloak) vive solo en la rama `main`.

## Stack

- Java 17 (JDK 17; `JAVA_HOME` debe apuntar a un JDK 17)
- Spring Boot 3.3 sin servidor web (`web-application-type=none`), Spring Data JPA, Bean Validation
- JavaFX 21 + AtlantaFX + Ikonli
- SQLite embebida (`${user.home}/.botica-inteligente/botica.db`), Hikari pool = 1
- Flyway (`db/migration`), `ddl-auto=validate`
- Login local con BCrypt (`spring-security-crypto`); sin Keycloak ni JWT
- Lombok, Maven, JUnit 5

## Estructura

Estructura en la raiz del repo, paquete base `com.botica.inteligente`, organizada por funcionalidad:

```text
src/main/java/com/botica/inteligente
├── config  security  shared  ui
└── usuario  categoria  laboratorio  producto  (y luego inventario, venta, compra, dashboard, asistente)
    └── controller | entity | repository | service | enums
```

No crear carpetas globales de entities/repositories; no usar `model`.

## Reglas de codigo

- Inyeccion por constructor; sin `@Autowired` en atributos.
- Controllers JavaFX sin logica de negocio; reglas en `service`.
- `BigDecimal` para dinero; `LocalDateTime` para auditoria; borrado logico con `estado`.
- Consultas a BD en `javafx.concurrent.Task`, nunca en el hilo de UI.
- SLF4J (nunca loguear contrasenas); sin `System.out`; sin `TODO`.
- Cambios de esquema solo con una migracion Flyway nueva (`V<n>__...sql`); no editar migraciones ya aplicadas.
- Sin secretos en el repo. No versionar `*.db`, `.idea/`, `target/`, `.env`.

## Validacion

```powershell
$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot"
mvn clean test        # pruebas
mvn javafx:run        # ejecutar la app
```

Las pruebas usan SQLite temporal; no usar H2. Si una validacion no puede ejecutarse, reportar la causa exacta.
