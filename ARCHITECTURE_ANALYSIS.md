# Análisis de Arquitectura del Proyecto

Este documento describe las decisiones arquitectónicas del proyecto **Asistente y Gestión de Botica Inteligente**.

## 1. Backend: Monolito Modular

El backend es un **monolito modular** en Spring Boot, organizado por dominio de negocio (`usuario`, `categoria`, `laboratorio`, `producto`, `venta`).

Cada módulo sigue capas: controller → service → repository, con DTOs y MapStruct.

Infraestructura transversal en `shared/` y `config/`: CORS, JWT, `ApiResponse`, manejo global de excepciones, Flyway.

## 2. Seguridad: JWT nativo

La autenticación usa **Spring Security + JWT** (io.jsonwebtoken), con usuarios en PostgreSQL y contraseñas BCrypt.

- Token en header: `Authorization: Bearer <jwt>`
- Roles: `ROLE_OWNER`, `ROLE_SELLER`
- Endpoints públicos: `/api/auth/**`, health, Swagger

> Nota: documentación antigua refería Keycloak; la implementación actual es JWT nativo.

## 3. Frontend: Cliente HTTP por dominio

El frontend (React + Vite) **no** usa un gateway intermedio. Consume el monolito REST mediante:

```text
Pantallas → services/*.service.ts → api/client.ts (Axios) → Backend REST
```

**¿Por qué no una API única de redirección?**

| Criterio | Decisión |
|----------|----------|
| Backend monolítico con APIs REST claras | Llamadas directas desde servicios frontend |
| Sin microservicios | No se necesita API Gateway |
| Sin agregaciones multi-servicio | No se necesita BFF |

Un BFF o Gateway se evaluará cuando existan microservicios (inventario, chatbot, reportes) o contratos distintos por cliente.

Detalle completo: [frontend/docs/INTEGRACION_BACKEND.md](frontend/docs/INTEGRACION_BACKEND.md)

## 4. Integración Frontend ↔ Backend

| Aspecto | Implementación |
|---------|----------------|
| URL base | `VITE_API_URL` → `http://localhost:8080/api` |
| Auth | `POST /api/auth/login` → JWT + userId |
| Catálogo | `GET /api/v1/{categorias,laboratorios,productos}` |
| Ventas | `GET/POST /api/v1/ventas` |
| CORS dev | `http://localhost:5173` |
| Formato respuesta | `ApiResponse<T>` con `data`, `message`, `success` |

## 5. Evolución futura

Cuando se implementen inventario, chatbot IA y reportes:

- **Opción A (recomendada inicial):** nuevos módulos en el mismo monolito + nuevos servicios en frontend.
- **Opción B:** extraer servicios y añadir API Gateway + opcional BFF para agregaciones (p. ej. chatbot + catálogo + stock en una sola respuesta).
