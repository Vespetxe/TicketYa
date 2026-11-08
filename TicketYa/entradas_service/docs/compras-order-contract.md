# Contrato interno de órdenes para Entradas

Entradas consulta a Compras antes de emitir tickets. Compras es la fuente de verdad del estado y del detalle confirmado de la orden.

## Consulta de orden

`GET /internal/orders/{orderId}`

### Respuesta confirmada

```json
{
  "orderId": "ord-1042",
  "clientId": "cli-17",
  "status": "CONFIRMADA",
  "items": [
    {
      "eventId": "evt-80",
      "sectorId": "platea",
      "seatId": "fila-a-asiento-12",
      "quantity": 1
    },
    {
      "eventId": "evt-80",
      "sectorId": "campo",
      "seatId": null,
      "quantity": 2
    }
  ]
}
```

Compras devuelve una línea por asiento numerado y puede agrupar cupos no numerados usando `quantity`. La cantidad debe ser positiva; una línea con `seatId` informado representa exactamente un asiento.

### Errores y estados

- `404 Not Found`: no existe `orderId`; Entradas responde que la orden no existe y no emite.
- `200 OK` con `status` distinto de `CONFIRMADA`: la orden existe pero no habilita emisión; Entradas rechaza la solicitud.
- `5xx` o error de conexión: Entradas responde dependencia no disponible y no emite; Compras puede reintentar.
- `200 OK` debe incluir `orderId`, `clientId` y al menos una unidad en `items`.

El endpoint es interno y se debe acordar con el responsable de Compras antes de integrar ambos servicios.
