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
| 6 | Dashboard y escaneo EAN-13 | Hecha |
| 7 | Asistente de consulta de medicamentos | Hecha |

El asistente busca solo en el catalogo propio (sintoma, nombre o principio activo), sugiere alternativas con stock y precio, y funciona sin internet ni claves externas.

## Configuracion opcional

Se puede sobrescribir en `application.yml` o con variables de entorno:

| Propiedad | Por defecto | Uso |
|---|---|---|
| `botica.igv.tasa` | `0.18` | Tasa de IGV en compras y ventas |
| `botica.serie.boleta` | `B001` | Serie de las boletas |
| `botica.serie.nota-venta` | `NV01` | Serie de las notas de venta |
| `botica.empresa.nombre`, `.ruc`, `.direccion`, `.telefono` | `Botica Inteligente` y vacios | Datos del emisor en el comprobante PDF |

Plan completo y reglas de negocio: [MIGRACION.md](MIGRACION.md). El proyecto web anterior esta en la rama `main`.
