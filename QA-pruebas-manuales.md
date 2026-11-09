# Registro de pruebas manuales — Botica Inteligente

Fecha: 8 de noviembre de 2026. Sesión: usuario `user`, perfil OWNER. Aplicación ya iniciada. No se modificó código ni se guardaron datos de prueba.

## Resultados observados

| Prueba | Resultado | Evidencia o alcance |
|---|---|---|
| Orden del menú | Aprobada | Inicio, Vender, Productos, Categorías, Laboratorios, Ventas, Inventario, Compras, Usuarios. No aparece Asistente. |
| Dashboard | Aprobada parcialmente | KPIs, gráfico y alertas cargan; no había ventas en el período, por lo que no se comprobó con datos de venta. |
| Nuevo producto: campos | Aprobada | No solicita código interno; muestra «Laboratorio / Droguería (opcional)» sin asterisco. |
| Búsqueda web sin nombre ni laboratorio | Aprobada | Muestra aviso de completar ambos y no abre diálogo de resultados. No se probó cada campo por separado. |
| Compra: proveedor y documento | Aprobada | RUC opcional, tipo y fecha; sin razón social ni número de documento. Tipos: Factura, Boleta y Recibo. El formulario también contiene IGV, observación y productos. |
| Inventario, lotes y kardex | Aprobada parcialmente | Paracetamol 500 con stock 0; detalle de lotes y kardex abre con tablas vacías. Vencimientos abre y muestra «Sin lotes por vencer». No se probó con movimientos. |
| Vistas en español | Aprobada parcialmente | Se abrieron Productos, Categorías, Laboratorios, Inventario, Compras y Ventas; no se observaron claves sin traducir ni null en lo visible. |
| Asistente en Vender | Diferencia | Consulta rápida «Fiebre»: responde que no encontró productos; el aviso legal aparece fijo arriba del panel, pero no al final de esa respuesta. Falta probar una respuesta con sugerencias y la ficha. |
| Productos frente a Inventario | Discrepancia por investigar | Inventario muestra Paracetamol 500; Productos muestra cero resultados incluso con filtro Todos. Se desconoce la causa o el estado del producto. En Vender la búsqueda por Paracetamol no ofreció resultados; tiene stock 0. |

## Pendiente

- Cambio ES/EN, persistencia tras reinicio y revisión de vistas y errores de negocio en ambos idiomas.
- Crear dos productos y comprobar códigos correlativos, edición deshabilitada y guardado sin laboratorio.
- Búsqueda web con nombre y laboratorio, selección, campos editables y prueba sin internet; búsqueda del producto en el asistente sin conexión.
- Compras con y sin RUC, proveedor en tabla, detalle y anulación con reversión de stock.
- Aviso legal al final de respuesta con sugerencia y en la ficha, también en inglés.
- Venta completa, cobro y PDF; inventario con lotes y movimientos; creación y baja de usuario; permisos de SELLER.

## Limitación de la ejecución

La herramienta de control no pudo escribir en el diálogo modal de producto: el foco se perdía al enviar texto y el campo quedaba vacío; el acceso por índice devolvió «element ... is not available in cached app state». Se solicitó ayuda para crear «Prueba QA 1» sin laboratorio; el formulario quedó abierto a la espera. No se considera fallo de la aplicación. El inicio de sesión debe realizarlo la persona usuaria.
