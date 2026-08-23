# 💊 Asistente y Gestión de Botica Inteligente

<p align="center">
  <img src="https://img.shields.io/badge/Java-22-007396?style=for-the-badge&logo=openjdk&logoColor=white" />
  <img src="https://img.shields.io/badge/Spring_Boot-3.3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white" />
  <img src="https://img.shields.io/badge/React-19-61DAFB?style=for-the-badge&logo=react&logoColor=black" />
  <img src="https://img.shields.io/badge/PostgreSQL-16-4169E1?style=for-the-badge&logo=postgresql&logoColor=white" />
  <img src="https://img.shields.io/badge/Docker-Compose-2496ED?style=for-the-badge&logo=docker&logoColor=white" />
</p>

<p align="center">
  <b>Sistema web interno para la gestión integral de boticas</b><br/>
  Ventas · Inventario · Reportes · Alertas · Chatbot IA farmacéutico
</p>

<p align="center">
  <a href="http://localhost:8080/swagger-ui.html"><img src="https://img.shields.io/badge/Swagger-UI-85EA2D?style=flat-square&logo=swagger&logoColor=black" /></a>
  <a href="http://localhost:8080/actuator/health"><img src="https://img.shields.io/badge/Health-UP-brightgreen?style=flat-square" /></a>
  <a href="backend/docs/architecture/c4-model.md"><img src="https://img.shields.io/badge/Arquitectura-C4-blue?style=flat-square" /></a>
  <img src="https://img.shields.io/badge/Estado-Desarrollo_Activo-orange?style=flat-square" />
  <img src="https://img.shields.io/badge/Licencia-MIT-lightgrey?style=flat-square" />
</p>

---

## ✨ Visión

Plataforma para que **Gerentes** y **Vendedores** gestionen una botica de forma ágil, con un **Chatbot IA** conectado directamente al inventario.

> **Ejemplo:** el vendedor pregunta *“pastillas para la fiebre”* y el chatbot, en lugar de ofrecer siempre el mismo medicamento, consulta `productos.principio_activo` e `indicaciones_oficiales` y sugiere alternativas disponibles en stock.

| Rol | Permisos clave |
|-----|----------------|
| **Gerente `ROLE_OWNER`** | CRUD catálogo, anular ventas, dashboard, reportes |
| **Vendedor `ROLE_SELLER`** | Vender, consultar catálogo y productos |

---

## 🧱 Arquitectura — Monolito Modular (una sola BD)

```text
┌──────────────────────────────────────────────────────────────────┐
│  Frontend  React 19 + Vite 8 + Tailwind 4  (http://localhost:5173) │
│  api/client.ts (Axios + JWT) → services/* → features/*            │
└───────────────────────────┬──────────────────────────────────────┘
                            │  /api  (Vite proxy → 8080, CORS)
                            ▼
┌──────────────────────────────────────────────────────────────────┐
│  Backend  Spring Boot 3.3.5  Java 22  (http://localhost:8080)      │
│  Security JWT (stateless) | Controllers | Services | Repositories │
│  PostgreSQL 16  botica_inteligente_db  schema botica  ddl-auto:update │
└───────────────────────────┬──────────────────────────────────────┘
                            │ 5432
                            ▼
              Docker  botica-postgres + botica-keycloak (8081)
```

* **Sin Flyway en `dev`** — `resources` limpio con un único `backend/src/main/resources/application.yml`. Tablas se generan vía `@Entity`/`@Table` por módulo (`categoria`, `laboratorio`, `producto`, `usuario`, `venta`) + `hibernate.default_schema=botica` + `hbm2ddl.create_namespaces=true`.
* **Para prod futuro:** reintroducir Flyway con `ddl-auto:validate` y `V1__*.sql`.

> Ver [C4 Model](backend/docs/architecture/c4-model.md) y [Integración Frontend ↔ Backend](frontend/docs/INTEGRACION_BACKEND.md) — verificado `2026-08-23` `Vite 5173 /api -> 8080` y `JWT` case-insensitive.

---

## 🚀 Stack

| Capa | Tecnologías |
|------|-------------|
| **Backend** | Spring Boot 3.3.5, Web, Data JPA, Validation, Security + JWT (jjwt 0.12.5), OAuth2 Resource Server (Keycloak 25), Hibernate 6.5, HikariCP, MapStruct 1.6, Lombok |
| **Frontend** | React 19, TypeScript 5.7, Vite 8, Tailwind 4, Axios 1.19, React Router 7, Recharts 3, jwt-decode 4 |
| **Datos** | PostgreSQL 16 (Docker), ddl-auto:update |
| **Docs & Test** | Springdoc OpenAPI 2.6 / Swagger, JUnit 5, Mockito, Testcontainers 1.20, ArchUnit 1.3, AssertJ |

---

## 📁 Estructura

```text
├── backend/               # Monolito Spring Boot
│   ├── src/main/java/com/botica/inteligente
│   │   ├── categoria, laboratorio, producto, venta, usuario
│   │   ├── security (JwtService, SecurityConfig)
│   │   └── shared (audit, exception, response)
│   └── src/main/resources/application.yml  # único, limpio
├── frontend/              # React + Vite
│   ├── src/api/client.ts  # Axios + Bearer + 401 -> sesion expirada
│   ├── src/features/{auth,dashboard,pos,catalogo}
│   └── vite.config.ts     # proxy /api -> 8080
├── docker-compose.yml     # Postgres + Keycloak (si aplica)
└── docs/
```

---

## ⚡ Quick Start (dev monolito)

### 1. Infraestructura

```bash
cd backend
docker compose up -d          # botica-postgres:5432 healthy + botica-keycloak:8081
docker ps                     # verifica Up
```

### 2. Backend

```bash
cd backend
# requiere JDK 22 en JAVA_HOME
mvn clean compile
mvn spring-boot:run           # http://localhost:8080
# alternativo
mvn package -DskipTests && java -jar target/*.jar
```

*Swagger:* `http://localhost:8080/swagger-ui.html` · *Health:* `http://localhost:8080/actuator/health`

### 3. Frontend

```bash
cd frontend
cp .env.example .env.local   # VITE_API_URL=/api
npm install
npm run dev                   # http://localhost:5173
```

> `VITE_API_URL=/api` usa el proxy de Vite (`vite.config.ts:22` `/api -> http://localhost:8080`). En prod usa URL absoluta `https://api.tu-dominio.com/api`.

### 4. Verificar integración (probado 2026-08-23)

```bash
# Crear vendedor
curl -X POST http://localhost:8080/api/auth/register \
  -H "Content-Type: application/json" \
  -d '{"username":"vendedor.botica","password":"Vendedor123!","nombres":"Vendedor","apellidos":"Botica"}'

# Login (case-insensitive + trim)
curl -X POST http://localhost:8080/api/auth/login \
  -d '{"username":"vendedor.botica","password":"Vendedor123!"}'

# Vía Vite proxy (frontend -> backend)
curl -X POST http://localhost:5173/api/auth/login \
  -d '{"username":"ADMINISTRADOR","password":"Admin123!"}'  # funciona igual

# Con token
curl http://localhost:8080/api/v1/categorias -H "Authorization: Bearer <token>"
```

O desde la UI: `http://localhost:5173/login` -> `Administrador / Admin123!` (o el usuario que creaste). El sistema es **case-insensitive** y hace `trim()`.

---

## 🔐 Seguridad

* **JWT stateless** `JwtService.java:20` `spring.security.jwt.secret` + `expiration`
* **Roles** `ROLE_OWNER` / `ROLE_SELLER` desde claim `roles`, convertidos por `SecurityConfig.java:37`
* **Rutas públicas:** `/api/auth/**`, `/actuator/health`, `/v3/api-docs/**`, `/swagger-ui/**`
* **Reglas:** `GET` catálogo `OWNER|SELLER`, `POST/PUT/PATCH` solo `OWNER`, `anyRequest.authenticated`

---

## 🗄️ Base de Datos (dev)

```text
POSTGRES_DB=botica_inteligente_db
POSTGRES_USER=botica_user
POSTGRES_PASSWORD=botica_password
schema: botica
```

Tablas auto-generadas: `categorias, laboratorios, productos, usuario, ventas, venta_detalles` + auditoría `fecha_creacion/actualizacion` (`shared/audit/AuditableEntity`).

```bash
docker exec botica-postgres psql -U botica_user -d botica_inteligente_db -c "\dt botica.*"
docker exec botica-postgres psql -U botica_user -d botica_inteligente_db -c "TRUNCATE botica.usuario CASCADE;"
```

---

## 🧪 Testing

```bash
cd backend
mvn test                                   # 29 tests (unit + integración con botica-postgres Docker)
mvn -Dtest=DatabaseMigrationIntegrationTest test  # verifica ddl-auto crea 6 tablas
```

*No usar H2 para simular PostgreSQL.*

---

## 🔑 Variables de entorno

| Variable | Descripción | Ejemplo dev |
|----------|-------------|-------------|
| `DB_URL` | JDBC con `currentSchema` | `jdbc:postgresql://localhost:5432/botica_inteligente_db?currentSchema=botica` |
| `DB_USERNAME` / `DB_PASSWORD` | Credenciales | `botica_user / botica_password` |
| `JWT_SECRET` / `JWT_EXPIRATION` | JWT | base64 de 64 bytes / `86400000` |
| `KEYCLOAK_ISSUER_URI` | Opcional OAuth2 | `http://localhost:8081/realms/botica-inteligente` |
| `CORS_ALLOWED_ORIGINS` | CORS | `http://localhost:5173,http://127.0.0.1:5173` |
| `VITE_API_URL` | Frontend | `/api` (dev) / `https://api.../api` (prod) |

Ver `backend/.env.example` y `frontend/.env.example`.

---

## 📦 Endpoints Fase 1

| Módulo | Método | Ruta |
|--------|--------|------|
| **Auth** | `POST` | `/api/auth/register`, `/api/auth/login` |
| **Categorías** | `GET` `/api/v1/categorias`, `GET /{id}`, `POST`, `PUT /{id}`, `PATCH /{id}/estado` |
| **Laboratorios** | `GET` `/api/v1/laboratorios` ... |
| **Productos** | `GET` `/api/v1/productos`, `GET /codigo-barras/{codigo}`, `POST`, `PUT`, `PATCH` |
| **Ventas** | `GET` `/api/v1/ventas`, `POST`, `PATCH /{id}/anular` |

---

## 🐳 Producción

```bash
cd backend && mvn clean package -DskipTests && java -jar target/*.jar  # usa variables de prod
cd frontend && echo "VITE_API_URL=https://api.tu-dominio.com/api" > .env.production && npm run build
# servir dist/ con Nginx (SPA)
```

---

## 📚 Documentación

* [AGENTS.md](AGENTS.md) — guía para agentes y colaboradores
* [C4 Model](backend/docs/architecture/c4-model.md)
* [Integración Frontend-Backend](frontend/docs/INTEGRACION_BACKEND.md)
* [Guía Despliegue](GUIA_DESPLIEGUE.md)

---

## 📄 Licencia

MIT — Proyecto académico para la gestión de boticas.

<p align="center">
  <b>Hecho con ❤️ para boticas inteligentes</b> — <code>monolito dev</code> · <code>resources limpio</code> · <code>29 tests OK</code>
</p>
