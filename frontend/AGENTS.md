# Frontend — Botica Inteligente

Aplicación web React + Vite para el sistema de gestión farmacéutica.

## Stack

- React 19 + TypeScript
- Vite 8
- Tailwind CSS 4
- Axios (cliente HTTP)
- Recharts (gráficos del panel)

## Estructura

```text
frontend/src/
├── api/client.ts          # Cliente Axios con JWT
├── context/AuthContext.tsx # Sesión y roles
├── services/              # Capa de integración con backend
├── types/                 # Tipos TypeScript alineados al backend
├── App.tsx                # Pantallas principales
└── main.tsx               # Punto de entrada
```

## Desarrollo local

```bash
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

Requisito: backend corriendo en `http://localhost:8080`.

Documentación de integración: [docs/INTEGRACION_BACKEND.md](docs/INTEGRACION_BACKEND.md)

## Variables de entorno

| Variable | Descripción | Ejemplo |
|----------|-------------|---------|
| `VITE_API_URL` | URL base de la API (incluye `/api`) | `http://localhost:8080/api` |

## Pantallas

| Pantalla | Rol | API utilizada |
|----------|-----|---------------|
| Login | Público | `POST /api/auth/login` |
| Dashboard | Gerente (`ROLE_OWNER`) | Ventas, productos |
| Punto de Venta | Todos | Productos, ventas |
| Catálogo | Todos (lectura) | Categorías, laboratorios, productos |

## Roles

- `ROLE_OWNER` → Gerente (acceso a panel y CRUD vía API)
- `ROLE_SELLER` → Vendedor (ventas y consulta de catálogo)
