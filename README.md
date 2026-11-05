# Asistente y Gestion de Botica Inteligente

Aplicacion de escritorio para administrar una botica: catalogo de medicamentos con codigo de barras EAN-13, inventario con lotes y vencimientos, ventas, dashboard y un asistente de IA para consultar informacion de productos (orientativa; no sustituye a un profesional de salud).

Perfiles: **OWNER** (propietario) y **SELLER** (vendedor).

## Stack

Java 17 · JavaFX 21 + AtlantaFX · Spring Boot 3.3 (sin servidor web) · SQLite + Flyway · Maven

## Ejecutar

Requisitos: JDK 17 y Maven.

```powershell
$env:JAVA_HOME="C:\Program Files\Microsoft\jdk-17.0.18.8-hotspot"
mvn javafx:run
```

En el primer arranque la app pide crear el usuario OWNER. La base de datos se crea en `~/.botica-inteligente/botica.db`.

## Pruebas

```powershell
mvn clean test
```

## Estado

| Fase | Contenido | Estado |
|---|---|---|
| 1 | Esqueleto, SQLite + Flyway, shell con tema | Hecha |
| 2 | Login local y roles | Hecha |
| 3 | Catalogo: categorias, laboratorios, productos | Hecha |
| 4 | Inventario, lotes, kardex, compras | Hecha |
| 5 | Ventas / punto de venta | Hecha |
| 6 | Dashboard y escaneo EAN-13 | Pendiente |
| 7 | Asistente IA | Pendiente |

Plan completo y reglas de negocio: [MIGRACION.md](MIGRACION.md). El proyecto web anterior esta en la rama `main`.
