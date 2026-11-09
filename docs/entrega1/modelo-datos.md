# Modelo relacional por microservicio

Los diagramas representan tablas y claves físicas del mapeo JPA actual. Los metadatos se consultaron en modo lectura en el MySQL aislado de la verificación previa; [snapshot](modelos/esquema-referencia.json). Cada esquema es propiedad de un servicio. No hay FK entre bases. `PK`, `FK` y `UK` indican claves; un UK compuesto se detalla como conjunto debajo y no como unicidad individual. Los tipos enum se simplifican a `enum` para Mermaid; su dominio literal permanece en el snapshot.

## Eventos

```mermaid
erDiagram
    event {
        bigint event_id PK "required"
        text description "nullable"
        varchar event_type "required"
        varchar name "required"
        bigint organizer_id "required"
        datetime start_date_time "required"
        enum status "required"
        bigint venue_id "required"
        bigint version "required"
    }
    event_participant {
        bigint event_id PK,FK "required"
        bigint participant_id PK,FK "required"
    }
    event_sector_price {
        bigint event_sector_price_id PK "required"
        decimal price "required"
        bigint sector_id "required"
        bigint event_id FK "required"
    }
    participant {
        bigint participant_id PK "required"
        varchar name "required"
        varchar role "nullable"
    }
    seat {
        bigint seat_id PK "required"
        varchar row_label "required"
        int seat_number "required"
        bigint sector_id FK "required"
    }
    sector {
        bigint sector_id PK "required"
        int capacity "required"
        datetime created_at "required"
        varchar name "required"
        enum type "required"
        bigint venue_id FK "required"
    }
    venue {
        bigint venue_id PK "required"
        varchar address "required"
        varchar city "nullable"
        datetime created_at "required"
        decimal latitude "required"
        decimal longitude "required"
        varchar name "required"
    }
    participant ||--o{ event_participant : "participant_id"
    event ||--o{ event_participant : "event_id"
    event ||--o{ event_sector_price : "event_id"
    sector ||--o{ seat : "sector_id"
    venue ||--o{ sector : "venue_id"
```

[Fuente Mermaid](diagramas/er-eventos.mmd).

Restricciones únicas compuestas (además de las claves individuales del gráfico):

- `event_participant`: `event_id, participant_id` (PK).
- `event_sector_price`: `event_id, sector_id` (UK).
- `seat`: `sector_id, row_label, seat_number` (UK).
- `sector`: `venue_id, name` (UK).

## Inventario

```mermaid
erDiagram
    asiento {
        bigint id_asiento PK "required"
        bigint catalogo_asiento_id "nullable"
        enum estado "required"
        varchar fila "required"
        int numero "required"
        bigint id_inventario_sector FK "required"
    }
    control_evento {
        bigint id_evento PK "required"
        enum estado "nullable"
    }
    detalle_reserva {
        bigint id_detalle_reserva PK "required"
        bigint id_asiento FK "nullable"
        bigint id_reserva FK "required"
    }
    inventario_sector {
        bigint id_inventario_sector PK "required"
        int capacidad_total "required"
        int disponibles "required"
        bit es_numerado "required"
        enum estado "required"
        bigint id_evento "required"
        bigint id_sector "required"
    }
    lugares_devueltos {
        bigint id PK "required"
        bigint asiento_id "nullable"
        varchar entrada_id UK "required"
        bigint reserva_id_reserva FK "required"
    }
    reserva {
        bigint id_reserva PK "required"
        int cantidad "required"
        enum estado "required"
        datetime fecha_creacion "required"
        datetime fecha_expiracion "required"
        bigint id_cliente "required"
        bigint id_evento "required"
        bigint id_sector "required"
    }
    inventario_sector ||--o{ asiento : "id_inventario_sector"
    reserva ||--o{ detalle_reserva : "id_reserva"
    asiento |o--o{ detalle_reserva : "id_asiento"
    reserva ||--o{ lugares_devueltos : "reserva_id_reserva"
```

[Fuente Mermaid](diagramas/er-inventario.mmd).

Restricciones únicas compuestas (además de las claves individuales del gráfico):

- `inventario_sector`: `id_evento, id_sector` (UK).

## Compras

```mermaid
erDiagram
    compra_entradas {
        bigint compra_id FK "required"
        bigint entrada_id PK,UK "required"
    }
    compras {
        bigint id PK "required"
        datetime bloqueo_hasta "nullable"
        bit cancelacion_solicitada "required"
        bigint cliente_id "required"
        varchar ejecutor "nullable"
        enum estado "required"
        enum etapa "required"
        bigint evento_id "required"
        datetime fecha_creacion "required"
        decimal importe_total "required"
        bigint reserva_id UK "required"
        datetime reserva_vencimiento "nullable"
        bigint version "required"
    }
    control_limite_entradas {
        bigint id PK "required"
        bigint cliente_id "required"
        bigint evento_id "required"
    }
    detalles_compra {
        bigint id PK "required"
        bigint asiento_id "nullable"
        int cantidad "required"
        decimal precio_unitario "required"
        bigint sector_id "required"
        decimal subtotal "required"
        bigint compra_id FK "required"
    }
    detalles_devolucion {
        bigint id PK "required"
        bigint entrada_id "required"
        decimal importe "required"
        bigint detalle_compra_id FK "required"
        bigint devolucion_id FK "required"
    }
    devoluciones {
        bigint id PK "required"
        bit cancelacion_evento "required"
        enum estado "required"
        enum etapa "nullable"
        datetime fecha_resolucion "nullable"
        datetime fecha_solicitud "required"
        decimal importe_reintegrado "required"
        decimal importe_solicitado "required"
        varchar motivo "required"
        varchar motivo_rechazo "nullable"
        varchar motivo_resolucion "nullable"
        bigint compra_id FK "required"
    }
    entrada_externa {
        bigint id PK "required"
        varchar uuid UK "required"
    }
    operaciones_simuladas {
        varchar clave PK "required"
        longtext datos "nullable"
    }
    pagos {
        bigint id PK "required"
        varchar clave_idempotencia UK "required"
        enum estado "required"
        datetime fecha_creacion "required"
        datetime fecha_procesamiento "nullable"
        decimal importe "required"
        decimal importe_reintegrado "required"
        enum medio_pago "nullable"
        varchar referencia_externa "nullable"
        varchar referencia_reintegro "nullable"
        bigint compra_id FK "required"
    }
    reintegros {
        bigint id PK "required"
        varchar clave_idempotencia UK "required"
        enum estado "required"
        datetime fecha_procesamiento "nullable"
        decimal importe "required"
        varchar referencia_externa "nullable"
        bigint devolucion_id FK "nullable"
        bigint pago_id FK "required"
    }
    compras ||--o{ compra_entradas : "compra_id"
    compras ||--o{ detalles_compra : "compra_id"
    devoluciones ||--o{ detalles_devolucion : "devolucion_id"
    detalles_compra ||--o{ detalles_devolucion : "detalle_compra_id"
    compras ||--o{ devoluciones : "compra_id"
    compras ||--o{ pagos : "compra_id"
    pagos ||--o{ reintegros : "pago_id"
    devoluciones |o--o{ reintegros : "devolucion_id"
```

[Fuente Mermaid](diagramas/er-compras.mmd).

Restricciones únicas compuestas (además de las claves individuales del gráfico):

- `control_limite_entradas`: `cliente_id, evento_id` (UK).

## Entradas

```mermaid
erDiagram
    emission_event_gate {
        varchar event_id PK "required"
        bit cancelled "required"
    }
    ticket_batches {
        bigint id PK "required"
        bit cancelled "required"
        datetime issued_at "required"
        varchar order_id UK "required"
    }
    tickets {
        varchar ticket_id PK "required"
        varchar client_id "required"
        varchar event_id "required"
        varchar qr_payload UK "required"
        varchar seat_id "nullable"
        varchar sector_id "required"
        enum status "required"
        bigint version "required"
        bigint batch_id FK "required"
    }
    ticket_batches ||--o{ tickets : "batch_id"
```

[Fuente Mermaid](diagramas/er-entradas.mmd).

Restricciones únicas compuestas (además de las claves individuales del gráfico):

- No hay índices únicos compuestos en el snapshot.

## Identidades y relaciones lógicas

| Referencia | Correspondencia real |
|---|---|
| Evento y lugar | `event.venue_id` referencia lógica a `venue.venue_id`; no FK física, aun dentro de la misma base. |
| Precio y sector | `event_sector_price.sector_id` es referencia lógica; solo su relación con event tiene FK. |
| Asiento catálogo e inventario | `seat.seat_id` viaja como `catalogoAsientoId` al inicializar. Inventario asigna otro `asiento.id_asiento`. Reservar y comprar usa ese ID operativo; fila/número ayudan a presentarlo. |
| Compra y reserva | `compras.reserva_id` único apunta a Inventario por REST; no FK entre esquemas. |
| Compra y entrada | `entrada_externa.id` numérico local corresponde a UUID externo único. `CompraDTO.entradasIds` contiene los numéricos; `Ticket.ticketId` contiene UUID. |
| Colección de entradas | `compra_entradas.compra_id` tiene FK; `entrada_id` es valor numérico único de la colección, sin FK física a entrada_externa. |
| Ticket y orden | `ticket_batches.order_id` es string con ID de compra; ticket referencia físicamente a su lote. Los IDs de evento/cliente/sector/asiento son strings externos. |
| Gate de emisión | Identidad del evento externa a Entradas, sin FK a Eventos. |
| Lugar devuelto | Registro idempotente por entrada externa; referencias a reserva son internas, asiento_id es escalar. |

## Invariantes adicionales

- `control_limite_entradas` serializa el cupo por cliente/evento; pagos y reintegros guardan claves y efectos; `operaciones_simuladas` es el diario de la pasarela simulada.
- Precios e importes son decimales; no se calcula el reintegro con precios actuales del catálogo.
- El modelo de devolución separa solicitud, detalles por entrada y reintegro; aprobar no ejecuta el movimiento financiero.
- Bloqueos, transiciones y validaciones de selección complementan las restricciones SQL. Un DER no expresa por sí solo la prohibición de emitir después de cancelar.
- No se presupone igualdad numérica entre asientos de distintos servicios ni correspondencia posicional entre listas de entradas. GET /compras/{id}/entradas expone por unidad entradaId, ticketId UUID, sector, asiento y estado para seleccionar exactamente la devolucion.

## Entidades con información solapada

`seat` representa la grilla del lugar y `asiento` la disponibilidad operativa. `entrada_externa` proyecta el UUID y datos del ticket para asociarlo con detalles y devoluciones locales. Los detalles de compra conservan precios históricos del catálogo. No hay un segundo propietario del catálogo ni relaciones SQL entre servicios: se coordinan por REST.
