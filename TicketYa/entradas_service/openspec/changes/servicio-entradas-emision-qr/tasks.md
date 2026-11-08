# Tasks

## 1. Modelo y persistencia de entradas

- [x] 1.1 Definir el modelo de ticket con `ticketId`, QR, `orderId`, `eventId`, `clientId`, `sectorId`, `seatId` opcional y estados; verificar con pruebas unitarias la representación de entradas numeradas y no numeradas.
- [x] 1.2 Crear el esquema relacional y restricciones únicas para el lote por `orderId`, `ticketId` y QR; verificar que la aplicación inicializa el esquema y la base rechaza duplicados.

## 2. Emisión de entradas

- [x] 2.1 Definir el contrato REST interno con Compras para consultar una orden y sus unidades confirmadas; verificar el contrato con ejemplos de respuesta y errores para orden inexistente y no confirmada.
- [x] 2.2 Implementar la emisión transaccional de un ticket por cada unidad confirmada, con QR opaco único y `seatId` solo cuando corresponda; verificar casos numerados y no numerados con pruebas unitarias.
- [x] 2.3 Implementar idempotencia por `orderId` y manejo de solicitudes concurrentes; verificar con una prueba concurrente que una orden produce un único lote y los reintentos devuelven los tickets existentes.
- [x] 2.4 Documentar emisión, validación de estado e idempotencia en OpenAPI y ejemplos de requests/responses; verificar que la especificación cubre códigos de éxito, conflicto y dependencia no disponible.

## 3. Consulta, anulación e integración

- [x] 3.1 Implementar consultas de tickets por `orderId` y `clientId`; verificar colecciones con resultados y vacías mediante pruebas de API.
- [x] 3.2 Implementar anulación y registro de reintegro comunicados por Compras; verificar transición de estados, orden inexistente y anulación repetida sin efectos duplicados.
- [x] 3.3 Documentar consulta y anulación en OpenAPI y en la colección de requests del servicio; verificar que los ejemplos usan `orderId`, `eventId`, `clientId`, `sectorId` y `seatId` opcional.
- [x] 3.4 Ejecutar las pruebas del servicio y una prueba de integración del flujo Compras → Entradas para emisión confirmada y rechazos; verificar que ningún caso emite tickets para una orden no confirmada.
