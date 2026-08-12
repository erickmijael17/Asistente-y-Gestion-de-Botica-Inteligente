# Guía General y Despliegue (Dev & Prod)

Esta guía describe cómo ejecutar **Asistente y Gestión de Botica Inteligente** en desarrollo y producción.

---

## 1. Resumen del proyecto

Sistema web interno para gestión de botica: ventas, catálogo de productos y panel gerencial.

| Componente | Tecnología | Puerto dev |
|------------|------------|------------|
| Backend | Spring Boot (Java 22) | 8080 |
| Frontend | React + Vite | 5173 |
| Base de datos | PostgreSQL (Docker) | 5432 |

**Integración:** el frontend consume la API REST del backend directamente (sin gateway intermedio). Ver [frontend/docs/INTEGRACION_BACKEND.md](frontend/docs/INTEGRACION_BACKEND.md).

---

## 2. Requisitos previos

- Java 22 (`JAVA_HOME` configurado)
- Maven
- Docker y Docker Compose
- Node.js 18+ y npm

---

## 3. Entorno de desarrollo

### 3.1 Base de datos

```bash
cd backend
docker compose up -d
```

PostgreSQL queda en `localhost:5432`, base `botica_inteligente_db`.

### 3.2 Backend

```bash
cd backend
mvn clean compile
mvn spring-boot:run
```

- API: http://localhost:8080
- Swagger: http://localhost:8080/swagger-ui.html
- Health: http://localhost:8080/actuator/health

### 3.3 Frontend

```bash
cd frontend
cp .env.example .env.local
npm install
npm run dev
```

- UI: http://localhost:5173
- Variable requerida: `VITE_API_URL=http://localhost:8080/api`

### 3.4 Verificar integración

1. Backend y PostgreSQL en ejecución.
2. Frontend con `.env.local` configurado.
3. Iniciar sesión con un usuario registrado en la BD (vía Swagger `POST /api/auth/register` si no existe).
4. Comprobar que el catálogo y punto de venta cargan datos reales.

---

## 4. Entorno de producción

### 4.1 Variables backend

```env
SPRING_PROFILES_ACTIVE=prod
DB_URL=jdbc:postgresql://<HOST>:5432/botica_inteligente_db
DB_USERNAME=<USUARIO>
DB_PASSWORD=<PASSWORD>
JWT_SECRET=<SECRETO_BASE64_SEGURO>
CORS_ALLOWED_ORIGINS=https://tu-dominio-frontend.com
```

### 4.2 Build backend

```bash
cd backend
mvn clean package -DskipTests
java -jar target/*.jar
```

### 4.3 Build frontend

```bash
cd frontend
echo "VITE_API_URL=https://api.tu-dominio.com/api" > .env.production
npm run build
```

Servir la carpeta `dist/` con Nginx u otro servidor estático.

#### Ejemplo Nginx (SPA)

```nginx
server {
    listen 80;
    server_name tu-dominio-frontend.com;
    root /var/www/botica-frontend/dist;
    index index.html;

    location / {
        try_files $uri $uri/ /index.html;
    }
}
```

---

## 5. Puertos por defecto

| Servicio | Puerto |
|----------|--------|
| Backend API | 8080 |
| Frontend Vite | 5173 |
| PostgreSQL | 5432 |

---

## 6. Arquitectura de integración (resumen)

No se usa una API única de redirección. El frontend organiza las llamadas en:

- `api/client.ts` — cliente HTTP + JWT
- `services/*.service.ts` — un servicio por módulo de negocio

Esta decisión es la adecuada para un monolito REST. Un BFF o API Gateway solo tendría sentido al migrar a microservicios o al agregar agregaciones complejas multi-servicio.
