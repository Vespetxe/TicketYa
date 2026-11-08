# Design

## Context

El proyecto `servicio-entradas` es actualmente un esqueleto Spring Boot sin entidades ni endpoints de negocio. Ya incluye Spring Data JPA, validación, Web MVC y el conector MySQL. La matriz de entrega asigna a Compras la coordinación de la orden y a Entradas la emisión, consulta y anulación de tickets; cada servicio mantiene su propia base relacional y comparte identificadores, no claves foráneas.

Ver `proposal.md` para la motivación y `specs/ticket-issuance/spec.md` para el contrato de comportamiento.

## Goals / Non-Goals

**Goals:**
- Definir un contrato interno entre Compras y Entradas que permita emitir tickets solo para una orden confirmada y consultar por `orderId` o `clientId`.
- Hacer que reintentar la emisión de una orden produzca el mismo lote, incluso si hay solicitudes concurrentes.
- Conservar `orderId`, `eventId`, `clientId`, `sectorId` y `seatId` cuando aplique, y soportar cupos sin numeración.
- Permitir reflejar la anulación y el reintegro que comunica Compras.

**Non-Goals:**
- Validar el QR en el ingreso al evento o impedir un segundo ingreso; eso corresponde a la entrega 2.
- Añadir JWT o roles; la entrega 1 identifica usuarios por parámetro o encabezado.
- Implementar la pasarela de pagos, las reservas o el proceso de aprobación de devoluciones dentro de Entradas.

## Decisions

1. **Compras es la fuente de verdad de la orden.** Antes de emitir, Entradas consulta el contrato interno de Compras para confirmar que el `orderId` existe y está confirmado, y obtiene los datos inmutables de cada unidad comprada: `clientId`, `eventId`, `sectorId` y `seatId` opcional. Se rechazan órdenes inexistentes o no confirmadas; si Compras no responde, no se emite y el llamador puede reintentar. Se elige esta consulta síncrona porque la entrega 1 prioriza REST síncrono y la validación no debe depender de campos de estado enviados por el cliente.

2. **La idempotencia se aplica al lote completo por `orderId`.** Una orden se emite en una sola operación. Entradas persiste todos sus tickets en una transacción y aplica una restricción única a `orderId` para representar un solo lote; una restricción de unicidad en cada `ticketId` y valor QR protege también la identidad individual. Ante reintentos, Entradas devuelve los tickets persistidos. Las restricciones de la base de datos, no un bloqueo en memoria, resuelven solicitudes concurrentes entre instancias. Se prefiere esta estrategia a una clave basada en `seatId`, porque una compra puede incluir varias unidades de un sector no numerado.

3. **El QR contiene un valor opaco y aleatorio.** Cada ticket recibe un `ticketId` y un valor QR no predecibles, sin datos personales ni información mutable dentro del código. El QR identifica la entrada; la consulta del ticket recupera sus datos. La validación de acceso queda fuera de este cambio.

4. **Entradas guarda referencias externas como identificadores simples.** Las relaciones con órdenes, clientes, eventos, sectores y asientos no se modelan con claves foráneas hacia otras bases. `seatId` puede ser nulo o estar ausente para sectores no numerados.

5. **Compras inicia cambios de estado de tickets.** Compras solicita la anulación tras aprobar una devolución o al procesar una cancelación de evento, y comunica si corresponde registrar el estado `ANULADA` o `REINTEGRADA`. Repetir la solicitud devuelve el estado existente sin repetir efectos. El pago y su reintegro permanecen bajo responsabilidad de Compras.

## Risks / Trade-offs

- [Compras no disponible durante una emisión] → Entradas falla sin crear tickets; Compras conserva la operación como pendiente/reintentable. La coordinación completa de fallos y compensaciones queda para la entrega 2.
- [Dos solicitudes simultáneas compiten por la misma orden] → La restricción única y la transacción de base de datos permiten una sola emisión; la solicitud perdedora relee y devuelve el lote existente.
- [La forma exacta del endpoint interno de Compras aún debe coordinarse con su responsable] → Acordar request, respuesta y códigos de error con la matriz de APIs antes de integrar; mantener los nombres compartidos `orderId`, `eventId`, `sectorId`, `seatId` y `clientId`.
- [El valor QR se filtra] → Mantenerlo opaco, evitar incluir PII y tratarlo como credencial; el control de acceso se definirá en la entrega 2.

## Migration Plan

No hay tablas ni datos de negocio previos en el esqueleto actual. Crear la estructura relacional de tickets con sus restricciones de unicidad al implementar el servicio y configurar la conexión MySQL por ambiente. Si se revierte antes de guardar tickets reales, retirar la tabla nueva y la configuración añadida; si ya hay datos, conservarlos y acordar una migración explícita antes de eliminar el esquema.
