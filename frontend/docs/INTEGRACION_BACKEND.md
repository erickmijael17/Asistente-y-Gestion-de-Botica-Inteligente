# Integración Frontend ↔ Backend

## Decisión arquitectónica: ¿API única (Gateway/BFF)?

**No se recomienda** crear una API única de redirección en el backend para este proyecto.

| Enfoque | Cuándo usarlo | Este proyecto |
|---------|---------------|---------------|
| **Cliente HTTP + servicios por dominio** | Monolito REST con pocos módulos | ✅ Recomendado |
| **BFF (Backend for Frontend)** | Agregar datos de varios microservicios o adaptar contratos distintos por cliente | ❌ No aplica |
| **API Gateway** | Enrutar tráfico entre múltiples servicios, rate limiting, TLS termination | ❌ No aplica |

El backend ya es un **monolito modular** con endpoints REST bien definidos. El frontend debe consumirlos directamente mediante una **capa de cliente HTTP** centralizada.

## Arquitectura implementada

```text
┌─────────────────────────────────────────────────────────┐
│  Frontend (React + Vite)                                │
│                                                         │
│  router/AppRouter.tsx (rutas + lazy)                    │
│       ↓                                                 │
│  features/ (Login, Dashboard, POS, Catálogo)            │
│       ↓                                                 │
│  services/  (auth, producto, categoria, venta, …)       │
│       ↓                                                 │
│  api/client.ts  (Axios + JWT interceptor)               │
└───────────────────────┬─────────────────────────────────┘
                        │ HTTPS / HTTP
                        │ Authorization: Bearer <JWT>
                        ▼
┌─────────────────────────────────────────────────────────┐
│  Backend (Spring Boot monolito)                         │
│                                                         │
│  /api/auth/login          → AuthController              │
│  /api/v1/categorias       → CategoriaController         │
│  /api/v1/laboratorios     → LaboratorioController       │
│  /api/v1/productos        → ProductoController          │
│  /api/v1/ventas           → VentaController              │
└─────────────────────────────────────────────────────────┘
```

### Principios

1. **Un solo `VITE_API_URL`**: en desarrollo `http://localhost:8080/api` vía **proxy de Vite** (`/api` → `http://localhost:8080`), en producción la URL absoluta de la API. Todas las rutas son relativas a esta base.
2. **JWT en interceptor**: el token se adjunta automáticamente; en 401 se limpia la sesión. `api/client.ts` dispara el evento `botica:sesion-expirada` que `AuthContext` escucha para cerrar sesión y redirigir a `/login`.
3. **Servicios por módulo de negocio**: cada servicio conoce su recurso REST y el formato `ApiResponse<T>`.
4. **Roles alineados con backend**: `ROLE_OWNER` (Gerente), `ROLE_SELLER` (Vendedor).

El proxy de Vite es solo para desarrollo (elimina errores CORS). En producción el frontend sirve la carpeta `dist/` y el navegador apunta directamente a la URL absoluta de la API.

## Endpoints consumidos

| Servicio frontend | Método | Ruta backend |
|-------------------|--------|--------------|
| `authService.login` | POST | `/api/auth/login` |
| `categoriaService.listar` | GET | `/api/v1/categorias` |
| `laboratorioService.listar` | GET | `/api/v1/laboratorios` |
| `productoService.listar` | GET | `/api/v1/productos` |
| `ventaService.listar` | GET | `/api/v1/ventas` |
| `ventaService.crear` | POST | `/api/v1/ventas` |

## Configuración local

```bash
# frontend/.env.local
VITE_API_URL=/api
```

En desarrollo, `vite.config.ts:22` define proxy `/api` → `http://localhost:8080` (verificado `5173/api/auth/login` -> `8080/api/auth/login` OK). CORS en backend `application.yml:33` permite `http://localhost:5173`.

## Formato de respuesta

Los endpoints de negocio devuelven:

```json
{
  "success": true,
  "message": "...",
  "data": { ... },
  "timestamp": "2026-08-12T10:00:00"
}
```

Auth devuelve directamente `{ token, userId, username, roles, type }`.

## Cuándo sí considerar BFF/Gateway

- Migración a **microservicios** (inventario, chatbot, reportes como servicios separados).
- Necesidad de **agregar** en una sola llamada datos de catálogo + inventario + IA.
- Despliegue con **múltiples clientes** (web, móvil, kiosko) con contratos distintos.

Hasta entonces, mantener la integración directa es más simple, testeable y alineada con el monolito actual.

## Verificación implícita 2026-08-23 (dev Docker)

* **Infra:** `docker ps` `botica-postgres:16 Up healthy 5432` + `botica-keycloak Up 8081`, `application.yml:4` `jdbc:postgresql://localhost:5432/botica_inteligente_db?currentSchema=botica` + `ddl-auto:update` (resources limpio, sin `db/migration`)
* **Backend:** `actuator/health 200 UP`, `v3/api-docs 200`, `CORS preflight 200`, `POST /api/auth/register` -> `ROLE_SELLER` + `POST /api/auth/login` -> JWT, `GET /api/v1/categorias` con `Bearer` OK, `POST /api/v1/categorias` como `ROLE_OWNER` OK
* **Frontend -> Backend:** `Vite 5173` proxy `/api` -> `8080` verificado `POST http://localhost:5173/api/auth/login` -> token `ROLE_OWNER` y `GET http://localhost:5173/api/v1/categorias` -> `VitaminasTest` con paginación `ApiResponse<T>`
* **DB:** `botica` 6 tablas (`categorias`, `laboratorios`, `productos`, `usuario`, `ventas`, `venta_detalles`) creadas por Hibernate
