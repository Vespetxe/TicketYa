# Contrato interno de órdenes — Entrega 1 integrada

Compras implementa GET `/internal/orders/{id}`; Entradas lo consulta mediante REST antes de emitir. No se publica a través del Gateway. Contratos vigentes: [Compras](../../../docs/entrega1/openapi/compras.json) y [Entradas](../../../docs/entrega1/openapi/entradas.json).

```json
{
  "orderId": "1",
  "clientId": "1",
  "status": "CONFIRMADA",
  "items": [
    {"eventId": "1", "sectorId": "1", "seatId": "1", "quantity": 1},
    {"eventId": "1", "sectorId": "2", "seatId": null, "quantity": 2}
  ]
}
```

Los IDs son ejemplos de números transportados como cadenas; deben provenir de operaciones reales. `seatId` es el ID operativo de Inventario. Cada línea numerada representa una unidad; una general permite cantidad positiva mayor a uno. Los UUID se asignan al emitir: no son IDs de compra, cliente ni asiento.

`CONFIRMADA` es una proyección para autorizar emisión: compra en `PROCESANDO` y etapa `EMISION`, o `COMPLETADA`, en ambos casos sin `cancelacionSolicitada`. No es un estado persistido de Compra. Los demás casos devuelven el nombre del estado de Compra y no habilitan emisión.

- Compra inexistente: Compras devuelve 404; Entradas informa `ORDER_NOT_FOUND` y no emite.
- Orden no habilitada: Entradas devuelve 409 `ORDER_NOT_CONFIRMED`.
- Dependencia inaccesible: Entradas devuelve 503 `ORDERS_UNAVAILABLE`.
- Detalle incompatible: 422 `INVALID_ORDER`.
- Repetir una emisión válida devuelve el lote existente sin duplicación (200 y `created=false`); primera emisión 201 y `created=true`.

Consultar la orden no sustituye las barreras locales: anular la orden persiste un lote cancelado aunque no haya tickets; cancelar el evento registra un gate durable que serializa la emisión. Ambos impiden la emisión tardía. Ver [arquitectura y consistencia](../../../docs/entrega1/arquitectura.md).
