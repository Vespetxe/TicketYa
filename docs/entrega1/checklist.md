# Checklist documental de Entrega 1

Referencia: lectura completa de las cinco páginas del enunciado `TPE-ArqWeb-TicketYa-2026.pdf`. La tabla de alcance y las tareas de Entrega 1 están en la página 4; el entregable documental y los datos/requests, en la página 5. La columna de cobertura identifica documentos entregados, no constituye una auditoría final de conformidad.

| Requisito de Entrega 1 | Cobertura |
|---|---|
| Features, historias y aceptación | [Historias](historias.md), cuatro dominios, Gateway e historias iniciales de F6/F8/F10/F11/F12 previstas para E2 |
| Arquitectura de microservicios independientes | [Arquitectura](arquitectura.md), REST y BD por servicio |
| Eventos, lugares, sectores, participantes y precios | Grupos E1.1–E1.3; OpenAPI Eventos |
| Búsqueda por filtros y ordenamiento | HU07–HU08; contrato de búsqueda; respuesta sin paginación |
| Publicación, suspensión y cancelación | HU05–HU06 y HU19 |
| Inventario numerado y no numerado, concurrencia | HU09, HU14; modelo Inventario |
| Reserva temporal y vencimiento | HU09, HU12; secuencia de expiración |
| Compra numerada con pago aprobado | HU10–HU11; secuencia aprobada |
| Compra con pago rechazado | HU11; secuencia rechazada |
| Pago simulado e idempotencia | HU11, HU13; arquitectura y pruebas |
| Emisión y consulta de entradas con QR | HU15; modelo Entradas y OpenAPI |
| Devolución y reintegro administrativo | HU16–HU18; contrato Compras |
| DER de cada microservicio | [Cuatro diagramas y restricciones](modelo-datos.md) |
| API REST documentada | [Cinco contratos OpenAPI](openapi/README.md), públicos e internos diferenciados |
| Gateway integrado | Arquitectura, rutas reales y OpenAPI Gateway |
| Ejecución y demostración | [README](../../README.md), Postman y esquemas nuevos |
| Pruebas y escenarios de integración | [Evidencia y procedimiento seguro](pruebas.md) |

El enunciado contempla una evolución posterior del proyecto. Este paquete desarrolla las historias y diseños de Entrega 1: no presenta autenticación JWT, validación de ingreso/uso único del QR, reportes, notificaciones, persistencia NoSQL ni infraestructura de Entrega 2 como implementados.

**Cobertura de historias de todo el sistema:** HU01–HU20 describen Entrega 1; HU21–HU25 cubren F6, F8, F10, F11 y F12 como previsiones explícitas de Entrega 2, sin implementación. Se subsana la omisión documental anterior. Las compensaciones existentes se documentan como parte del código actual, sin agregar prestaciones futuras.

La exigencia de base relacional independiente se representa por cuatro esquemas y cuatro propietarios de datos. El despliegue local puede compartir una instancia MySQL sin habilitar claves foráneas entre esquemas.

Trazabilidad del PDF: F1→HU03–HU06; F2→HU01–HU04; F3→HU05/HU06/HU19; F4→HU09–HU13/HU15; F5→HU11/HU14; F6→HU21 (E2); F7→HU16–HU19; F8→HU22 (E2); F9→HU07/HU08; F10→HU23 (E2); F11→HU24 (E2); F12→HU25 (E2).
