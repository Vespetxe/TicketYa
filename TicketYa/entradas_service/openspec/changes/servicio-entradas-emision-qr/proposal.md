# Proposal

## Why

El servicio de Entradas necesita emitir y administrar las entradas digitales que resultan de compras confirmadas. Una emisión repetida por reintentos no debe crear tickets duplicados, y cada entrada debe poder identificarse de forma única también cuando corresponde a un sector sin asiento numerado.

## What Changes

- Incorporar emisión idempotente de una entrada por cada lugar o cupo confirmado, vinculada a la orden, el evento y la ubicación correspondiente.
- Generar un identificador y contenido QR únicos por entrada, y permitir consultar entradas por orden o cliente.
- Permitir anular entradas y reflejar estados de emisión, anulación y reintegro para coordinar devoluciones o cancelaciones de eventos.
- Rechazar solicitudes de emisión cuando la orden no existe o no está confirmada, según el contrato acordado con Compras.

## Capabilities

### New Capabilities
- `ticket-issuance`: emisión, consulta, unicidad, idempotencia y anulación de entradas digitales.

### Modified Capabilities

Ninguna.

## Impact

Afecta al microservicio Spring Boot `servicio-entradas`, su persistencia relacional propia y sus endpoints REST internos para emisión, consulta y anulación. Se integra con Compras mediante `orderId` y con Eventos mediante `eventId`; usa `sectorId` y `seatId` cuando corresponda, sin exigir `seatId` para sectores no numerados. No incluye autenticación JWT ni validación de uso del QR en el acceso, previstas para la entrega 2.
