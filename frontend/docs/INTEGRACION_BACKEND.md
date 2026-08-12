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
│  App.tsx / pantallas                                    │
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

1. **Un solo `VITE_API_URL`**: `http://localhost:8080/api` — todas las rutas son relativas a esta base.
2. **JWT en interceptor**: el token se adjunta automáticamente; en 401 se limpia la sesión.
3. **Servicios por módulo de negocio**: cada servicio conoce su recurso REST y el formato `ApiResponse<T>`.
4. **Roles alineados con backend**: `ROLE_OWNER` (Gerente), `ROLE_SELLER` (Vendedor).

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
VITE_API_URL=http://localhost:8080/api
```

CORS en backend (`application-dev.yml`) permite `http://localhost:5173`.

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
