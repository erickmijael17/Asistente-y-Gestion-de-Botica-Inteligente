# Botica Inteligente - Aplicacion de escritorio

Documento maestro (rama `escritorio`). Claude planifica y revisa; Codex implementa por fases. El proyecto vive en la **raiz del repo** (no hay subcarpetas `backend/`, `frontend/` ni `escritorio/`).

## Vision

Plataforma de gestion farmaceutica para una botica: inventario con lotes y vencimientos, catalogo maestro de medicamentos (EAN-13), ventas, dashboard, y un asistente IA de consulta de medicamentos (orientativo, no sustituye a un profesional de salud). Dos perfiles: `OWNER` (propietario) y `SELLER` (vendedor).

Funcionalidad diferenciadora: **escaneo EAN-13**. Escanear -> buscar en catalogo -> si existe, sumar stock/registrar lote; si no, alta rapida asociando el codigo. Asi el catalogo propio crece solo.

## Decisiones

| Tema | Decision |
|---|---|
| Destino | App de escritorio monolitica: JavaFX 21 + AtlantaFX + Spring Boot 3.3 (sin servidor web, `web-application-type=none`) |
| Java | 17 (JDK instalado) |
| Persistencia | SQLite embebida (`sqlite-jdbc` + `hibernate-community-dialects`), Hikari pool = 1. BD en `${user.home}/.botica-inteligente/botica.db`. Nunca versionar `*.db` |
| Esquema | Flyway (`V1__...sql`), `ddl-auto=validate` |
| Seguridad | Login local: tabla `usuarios`, BCrypt (`spring-security-crypto`), roles `OWNER`/`SELLER`. Sin Keycloak, JWT ni HTTP |
| Build | Maven, modulo unico en la raiz |
| Paquete base | `com.botica.inteligente` |
| Capas | `controller` (JavaFX) -> `service` -> `repository` (Spring Data JPA) -> `entity`. `record` como comandos/resultados en servicios; sin DTO de API |

## Referencia del proyecto web anterior

El codigo web (Spring REST + React + PostgreSQL + Keycloak) sigue en la rama `main` y en una copia local de solo lectura:
`C:\Users\USUARIO\AppData\Local\Temp\claude\20b7c692e3c0376c\ae5096e7-de98-4c43-82bc-4a97aee72f5b\scratchpad\referencia\backend\src\main\java\com\botica\inteligente`.
Portar entidades y reglas de `categoria`, `laboratorio`, `producto`, `venta`, `usuario`. **No** portar controllers REST, DTOs HTTP, MapStruct, Spring Security/OAuth/JWT ni Springdoc.

## Diferencias verificadas backend anterior vs informe `crud-persona-spring`

- Tablas en plural: `usuarios`, `categorias`, `laboratorios`, `productos`, `ventas`.
- `productos`: `codigo_interno`, `codigo_barras` (campo EAN-13; unico cuando no es nulo), `nombre_comercial`, `nombre_generico`, `tipo_producto`, `principio_activo`, `concentracion`, `presentacion`, `registro_sanitario`, `requiere_receta`, `precio_compra`, `precio_venta`, `stock_minimo`, `estado` (borrado logico), `categoria_id`, `laboratorio_id`, campos de informacion medica para el asistente.
- `productos` **no tiene stock**: el inventario se crea en la fase 4 (lotes + kardex); el stock se deriva de movimientos.
- `ventas` anterior: `usuario_id`, `fecha_venta`, `subtotal`, `impuestos`, `total`, `estado`, `@Version`. Serie, correlativo, comprobante e IGV del informe se agregan en la fase 5.

## Reglas de negocio (del informe)

- IGV 18 % con/sin precios incluidos; importe de linea = cantidad x precio, `HALF_UP` a 2 decimales (`CalculadoraIgv`).
- Serie `B001` (boleta), `NV01` (nota de venta); formato `SERIE-########`; unico `(serie, numero)`.
- Anular no borra: cambia estado y revierte stock.
- Nunca vender mas que el stock; el stock cambia solo via `InventarioService` generando `MovimientoInventario` con `stock_resultante`.
- Costo promedio ponderado a 4 decimales, sin IGV.
- No anular una compra si parte ya se vendio.
- No eliminar categoria/laboratorio/producto con dependencias (validar tambien en categorias).
- El detalle de venta guarda copia de codigo y nombre del producto.
- Mejoras sobre el original: tasa IGV y series en configuracion; consultas pesadas en `javafx.concurrent.Task`; `show-sql=false`; seeder desactivado por defecto; auditoria del usuario en ventas/anulaciones.

## Componentes UI a crear

`Refrescable`, `Notificador` (AtlantaFX), `Paginador`, `Autocompletar<T>`, `Exportador` (Task en segundo plano), `Tablas`, `Formularios` (validacion inline). Arranque: `Launcher` -> `JavaFxApplication` (init levanta Spring; start carga FXML con `controllerFactory(context::getBean)`).

## Estructura objetivo

```
pom.xml
src/main/java/com/botica/inteligente/
  Launcher.java  JavaFxApplication.java  BoticaApplication.java
  config/ security/ shared/{audit,exception,util} ui/
  usuario/ categoria/ laboratorio/ producto/ inventario/ venta/ compra/ dashboard/ asistente/
    (cada uno: controller, entity, repository, service, enums)
src/main/resources/
  application.yml  db/migration/V1__esquema_inicial.sql  css/app.css  view/*.fxml
src/test/java/...
```

## Fases

| Fase | Contenido | Criterio de salida |
|---|---|---|
| 1 | Esqueleto: pom, Launcher, JavaFxApplication, SQLite + Flyway V1 (usuarios, categorias, laboratorios, productos), shell con navegacion y tema | `mvn -q clean test` verde |
| 2 | Login local + roles + OWNER inicial (`BOTICA_OWNER_PASSWORD` o primer arranque) | Test de `AuthService`; vistas segun rol |
| 3 | Catalogo: categorias, laboratorios, productos (CRUD + paginacion + autocompletar) | Tests de servicio |
| 4 | Inventario: lotes con vencimiento, kardex, costo promedio, compras/proveedores, alertas | kardex == stock |
| 5 | Ventas / punto de venta + IGV + correlativo + anulacion + PDF | Test de integracion |
| 6 | Dashboard + escaneo EAN-13 | |
| 7 | Asistente IA sobre el catalogo | |

Cada fase termina con `mvn clean test` en verde y un commit propio.

## Reglas de trabajo

- Pruebas de integracion con SQLite temporal; no usar H2.
- Inyeccion por constructor; SLF4J; sin `System.out` ni `TODO`; `BigDecimal` para dinero; `LocalDateTime` en auditoria.
- Sin secretos en el repo; no versionar `*.db`, `.idea/`, `target/`.
