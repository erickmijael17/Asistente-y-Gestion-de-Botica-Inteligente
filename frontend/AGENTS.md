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
├── api/client.ts              # Cliente Axios con JWT (401 → evento sesion expirada)
├── components/
│   ├── layout/                # Sidebar, DashboardLayout
│   ├── ui/                    # Badge
│   └── icons.tsx              # Iconos SVG
├── context/AuthContext.tsx    # Sesión, roles y logout (escucha 401)
├── features/
│   ├── auth/                  # LoginScreen
│   ├── dashboard/             # DashboardScreen (gráficos Recharts)
│   ├── pos/                   # POSScreen (chatbot IA)
│   └── catalogo/              # CatalogoScreen
├── hooks/                     # useCatalogo (datos compartidos)
├── router/AppRouter.tsx       # Rutas, lazy loading, ProtectedRoute/GerenteRoute
├── services/                  # Capa de integración con backend
├── types/                     # Tipos TypeScript alineados al backend
├── utils/                     # format, roles
├── App.tsx                    # Renderiza AppRouter
└── main.tsx                   # Punto de entrada (BrowserRouter + AuthProvider)
```

Rutas (`AppRouter.tsx`): `/login`, `/dashboard` (GerenteRoute), `/pos`, `/catalogo`. Las pantallas se cargan con `React.lazy`; el gráfico del dashboard (Recharts) se descarga solo cuando se visita esa ruta.

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
| `VITE_API_URL` | URL base de la API. En dev usar `/api` (proxy Vite); en prod la URL absoluta | `/api` o `https://api.dominio.com/api` |

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
