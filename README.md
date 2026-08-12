# Asistente y Gestión de Botica Inteligente

Sistema web para la gestión integral de una botica, organizado en dos componentes:

- `backend/` — Backend monolítico modular en Spring Boot
- `frontend/` — Aplicación web React + Vite

## Visión del Proyecto

El sistema permite gestionar **ventas**, **inventario**, generar **reportes** y administrar **alertas**. Está diseñado para dos tipos de usuarios:

- **Gerente (Dueño)**: Supervisa reportes, ventas e inventario.
- **Vendedor (Farmacéutico)**: Realiza ventas y atiende a los clientes.

Adicionalmente, el sistema contará con un **Chatbot IA** conectado a la base de datos de productos (pendiente de implementación).

[Ver Modelado de Arquitectura C4](backend/docs/architecture/c4-model.md)  
[Ver Integración Frontend-Backend](frontend/docs/INTEGRACION_BACKEND.md)

## Tecnologías

### Backend

- Java 22, Spring Boot 3.3.5
- Spring Web, Data JPA, Validation, Security
- JWT nativo (io.jsonwebtoken)
- PostgreSQL, Flyway, Docker Compose
- Lombok, MapStruct, Springdoc OpenAPI
- JUnit 5, Mockito, Testcontainers

### Frontend

- React 19, TypeScript, Vite 8
- Tailwind CSS 4, Axios, Recharts

## Levantar infraestructura

1. Crear archivo `.env` desde `backend/.env.example`.
2. Levantar PostgreSQL (desde `backend/`):

```bash
docker compose up -d
```

Servicio: PostgreSQL en `localhost:5432`, base `botica_inteligente_db`.

## Ejecutar backend

Requisito: JDK 22 activo en `JAVA_HOME`.

```bash
cd backend
mvn clean test
mvn spring-boot:run
```

Swagger: `http://localhost:8080/swagger-ui.html`  
Health: `http://localhost:8080/actuator/health`

## Ejecutar frontend

```bash
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

UI: `http://localhost:5173`

## Variables de entorno principales

### Backend

```text
SPRING_PROFILES_ACTIVE=dev
DB_URL=jdbc:postgresql://localhost:5432/botica_inteligente_db
DB_USERNAME=botica_user
DB_PASSWORD=botica_password
JWT_SECRET=<secreto-base64>
CORS_ALLOWED_ORIGINS=http://localhost:5173
```

### Frontend

```text
VITE_API_URL=http://localhost:8080/api
```

## Endpoints principales

| Recurso | Base path |
|---------|-----------|
| Auth | `/api/auth/login`, `/api/auth/register` |
| Categorías | `/api/v1/categorias` |
| Laboratorios | `/api/v1/laboratorios` |
| Productos | `/api/v1/productos` |
| Ventas | `/api/v1/ventas` |

## Seguridad

Autenticación con **JWT nativo** (Spring Security). El frontend envía `Authorization: Bearer <token>`.

Roles: `ROLE_OWNER` (Gerente), `ROLE_SELLER` (Vendedor).

- `GET` en catálogo: OWNER o SELLER
- `POST`, `PUT`, `PATCH` en catálogo: solo OWNER
- Ventas: crear OWNER o SELLER; anular solo OWNER

## Alcance actual

Implementado: usuarios, seguridad JWT, catálogo (categorías, laboratorios, productos), ventas, frontend conectado al backend.

Pendiente: inventario, lotes, compras, proveedores, clientes, pagos, reportes, chatbot IA.
