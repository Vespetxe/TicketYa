# Spec Delta

## Purpose

Define cómo TicketYa emite, consulta e invalida entradas digitales para compras confirmadas, manteniendo la identidad de cada entrada y evitando duplicados ante reintentos.

## ADDED Requirements

### Requirement: Emisión solo para órdenes confirmadas
El servicio SHALL emitir una entrada por cada unidad de compra confirmada y SHALL rechazar una orden inexistente o que no esté confirmada.

#### Scenario: Emisión para una orden confirmada
- **WHEN** Entradas recibe una solicitud para una orden existente y confirmada
- **THEN** crea una entrada por cada unidad confirmada de la orden

#### Scenario: Orden inexistente
- **WHEN** Entradas recibe una solicitud para un `orderId` inexistente
- **THEN** rechaza la emisión y no crea entradas

#### Scenario: Orden no confirmada
- **WHEN** Entradas recibe una solicitud para una orden existente que no está confirmada
- **THEN** rechaza la emisión y no crea entradas

### Requirement: Emisión idempotente por orden
La emisión SHALL ser idempotente por `orderId`: repetir una solicitud para la misma orden SHALL devolver el lote ya emitido sin crear entradas adicionales.

#### Scenario: Reintento de emisión de la misma orden
- **WHEN** Entradas recibe nuevamente una solicitud de emisión para un `orderId` ya emitido
- **THEN** devuelve las entradas previamente creadas para esa orden sin duplicar tickets ni códigos QR

#### Scenario: Emisiones concurrentes de la misma orden
- **WHEN** varias solicitudes de emisión para el mismo `orderId` se procesan concurrentemente
- **THEN** queda persistido un solo lote de entradas para la orden

### Requirement: Identidad y unicidad de cada entrada
Cada entrada SHALL tener `ticketId` y contenido QR únicos, y SHALL quedar asociada a `orderId`, `eventId`, `clientId` y `sectorId`; `seatId` SHALL estar presente cuando el sector sea numerado y SHALL omitirse cuando no lo sea.

#### Scenario: Entrada para asiento numerado
- **WHEN** se emite una unidad confirmada asociada a un asiento numerado
- **THEN** la entrada conserva `eventId`, `sectorId` y `seatId`, además de la orden y el cliente

#### Scenario: Entrada para sector no numerado
- **WHEN** se emite una unidad confirmada de un sector no numerado
- **THEN** la entrada conserva `eventId` y `sectorId` sin exigir ni inventar un `seatId`, y mantiene su propio `ticketId` y QR únicos

#### Scenario: Colisión de identificador o QR
- **WHEN** se intenta persistir una entrada cuyo `ticketId` o QR ya pertenece a otra entrada
- **THEN** el sistema rechaza la duplicación y conserva la entrada previamente registrada

### Requirement: Consulta de entradas emitidas
El servicio SHALL permitir consultar entradas por `orderId` o por `clientId` y SHALL devolver sus referencias de compra, evento y ubicación junto con su estado.

#### Scenario: Consulta por orden
- **WHEN** un consumidor consulta las entradas de un `orderId` existente
- **THEN** recibe las entradas emitidas para esa orden con sus estados y datos de ubicación

#### Scenario: Consulta por cliente sin entradas
- **WHEN** un consumidor consulta un `clientId` que no tiene entradas emitidas
- **THEN** recibe una colección vacía

### Requirement: Anulación y reintegro de entradas
El servicio SHALL permitir invalidar una entrada emitida por devolución aprobada o cancelación de evento, y SHALL reflejar si quedó anulada o reintegrada según el resultado comunicado por Compras.

#### Scenario: Anulación por devolución aprobada
- **WHEN** Compras comunica la aprobación de una devolución para una entrada emitida
- **THEN** Entradas invalida el ticket y registra su estado de anulación o reintegro según el resultado del proceso

#### Scenario: Anulación por cancelación de evento
- **WHEN** se solicita invalidar las entradas asociadas a una orden de un evento cancelado
- **THEN** Entradas invalida las entradas afectadas y registra el estado comunicado por el flujo de reintegro

#### Scenario: Anulación repetida
- **WHEN** Entradas recibe más de una vez la misma solicitud de anulación para una entrada ya invalidada
- **THEN** mantiene el estado final existente y no duplica ni altera el resultado de la anulación
