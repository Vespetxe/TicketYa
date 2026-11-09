# Inventario de endpoints de Entrega 1

Contrato publico: [gateway.json](gateway.json). Los contratos por servicio incluyen coordinacion interna.

| Servicio | Metodo | Ruta | Visibilidad | Respuestas |
|---|---|---|---|---|
| eventos | POST | `/api/v1/events` | publica | 201, 400, 404, 422, 500 |
| eventos | GET | `/api/v1/events/search` | publica | 200, 400, 422, 500 |
| eventos | GET | `/api/v1/events/{eventId}` | publica | 200, 400, 404, 500 |
| eventos | PATCH | `/api/v1/events/{eventId}/cancel` | publica | 200, 400, 404, 422, 500, 503 |
| eventos | POST | `/api/v1/events/{eventId}/prices` | publica | 201, 400, 404, 422, 500 |
| eventos | PATCH | `/api/v1/events/{eventId}/publish` | publica | 200, 400, 404, 422, 500, 503 |
| eventos | PATCH | `/api/v1/events/{eventId}/status` | publica | 200, 400, 404, 422, 500, 503 |
| eventos | PATCH | `/api/v1/events/{eventId}/suspend` | publica | 200, 400, 404, 422, 500, 503 |
| eventos | GET | `/api/v1/participants` | publica | 200, 400, 500 |
| eventos | POST | `/api/v1/participants` | publica | 201, 400, 422, 500 |
| eventos | GET | `/api/v1/participants/{id}` | publica | 200, 400, 404, 500 |
| eventos | PUT | `/api/v1/participants/{id}` | publica | 200, 400, 404, 422, 500 |
| eventos | DELETE | `/api/v1/participants/{id}` | publica | 204, 400, 404, 422, 500 |
| eventos | GET | `/api/v1/sectors/{sectorId}/seats` | publica | 200, 400, 404, 500 |
| eventos | GET | `/api/v1/venues` | publica | 200, 400, 500 |
| eventos | POST | `/api/v1/venues` | publica | 201, 400, 422, 500 |
| eventos | GET | `/api/v1/venues/{venueId}` | publica | 200, 400, 404, 500 |
| eventos | GET | `/api/v1/venues/{venueId}/sectors` | publica | 200, 400, 404, 500 |
| eventos | POST | `/api/v1/venues/{venueId}/sectors` | publica | 201, 400, 404, 409, 422, 500 |
| inventario | GET | `/disponibilidad/eventos/{idEvento}/sectores/{idSector}` | publica | 200, 404, 500 |
| inventario | POST | `/eventos/{idEvento}/cancelar-inventario` | interna | 200, 409, 500 |
| inventario | POST | `/eventos/{idEvento}/inventario` | interna | 201, 400, 409, 500 |
| inventario | POST | `/internal/eventos/{id}/habilitar` | interna | 200, 409, 500 |
| inventario | POST | `/internal/eventos/{id}/publicar` | interna | 200, 409, 500 |
| inventario | POST | `/internal/eventos/{id}/suspender` | interna | 200, 409, 500 |
| inventario | POST | `/internal/reservas/{id}/compensar` | interna | 200, 404, 409, 500 |
| inventario | GET | `/internal/reservas/{id}/devoluciones` | interna | 200, 404, 500 |
| inventario | POST | `/internal/reservas/{id}/devoluciones` | interna | 200, 400, 404, 409, 500 |
| inventario | POST | `/ocupaciones/liberar` | interna | 200, 400, 404, 409, 500 |
| inventario | GET | `/reservas` | publica | 200, 500 |
| inventario | POST | `/reservas` | publica | 201, 400, 404, 409, 500 |
| inventario | GET | `/reservas/{id}` | publica | 200, 404, 500 |
| inventario | POST | `/reservas/{id}/confirmar` | interna | 200, 404, 409, 500 |
| inventario | POST | `/reservas/{id}/liberar` | interna | 200, 404, 409, 500 |
| compras | GET | `/admin/devoluciones` | administrativa | 200, 400, 404, 406, 409, 500, 503 |
| compras | POST | `/admin/devoluciones/{id}/aprobar` | administrativa | 200, 400, 404, 406, 409, 415, 500, 503 |
| compras | GET | `/admin/devoluciones/{id}/procesamiento` | administrativa | 200, 400, 404, 406, 409, 500, 503 |
| compras | POST | `/admin/devoluciones/{id}/procesar` | administrativa | 200, 400, 404, 406, 409, 415, 500, 503 |
| compras | POST | `/admin/devoluciones/{id}/rechazar` | administrativa | 200, 400, 404, 406, 409, 415, 500, 503 |
| compras | POST | `/admin/eventos/{id}/cancelacion` | administrativa | 200, 202, 400, 404, 406, 409, 500, 503 |
| compras | GET | `/admin/reintegros/{id}` | administrativa | 200, 400, 404, 406, 409, 500, 503 |
| compras | GET | `/compras` | publica | 200, 400, 404, 406, 409, 500 |
| compras | POST | `/compras` | publica | 201, 400, 404, 406, 409, 415, 500 |
| compras | GET | `/compras/{id}` | publica | 200, 400, 404, 406, 409, 500 |
| compras | POST | `/compras/{id}/cancelar` | publica | 200, 400, 404, 406, 409, 500 |
| compras | GET | `/compras/{id}/detalle` | publica | 200, 400, 404, 406, 409, 500 |
| compras | GET | `/compras/{id}/entradas` | publica | 200, 400, 404, 406, 409, 500, 503 |
| compras | GET | `/compras/{id}/estado` | publica | 200, 400, 404, 406, 409, 500 |
| compras | POST | `/devoluciones` | publica | 201, 400, 404, 406, 409, 415, 500 |
| compras | GET | `/devoluciones/{id}` | publica | 200, 400, 404, 406, 409, 500 |
| compras | GET | `/internal/orders/{id}` | interna | 200, 400, 404, 406, 409, 500 |
| compras | GET | `/pagos` | publica | 200, 400, 404, 406, 409, 500 |
| compras | POST | `/pagos` | publica | 200, 201, 400, 404, 406, 409, 415, 500 |
| compras | GET | `/pagos/{id}` | publica | 200, 400, 404, 406, 409, 500 |
| entradas | POST | `/internal/events/{id}/cancel-emission` | interna | 200, 500 |
| entradas | POST | `/internal/tickets/issue` | interna | 200, 201, 400, 404, 409, 422, 500, 503 |
| entradas | POST | `/internal/tickets/orders/{id}/annul` | interna | 200, 500 |
| entradas | PATCH | `/internal/tickets/status` | interna | 200, 400, 500 |
| entradas | PATCH | `/internal/tickets/{ticketId}/status` | interna | 200, 400, 404, 500 |
| entradas | GET | `/tickets` | publica | 200, 400, 500 |

Los ejemplos de IDs son ilustrativos; para ejecutar, importar la coleccion Postman que captura identidades reales.
Los codigos 500 y los errores de transporte no autorizan a repetir con una nueva clave de pago/reintegro.
El endpoint de acreditacion `/internal/test-environment` existe solo en el perfil `pruebas-integrales`; no integra la API de negocio normal.
El Gateway tambien devuelve 404 para rutas sin coincidencia. Los contratos no uniforman artificialmente los formatos de error de los servicios.

Los esquemas de Entradas usan alias documentales: `Ticket` corresponde a `TicketResponse`, `TicketStatusRequest` a `TicketStatusUpdateRequest` y `OrderEventStatusRequest` a `EventTicketsStatusUpdateRequest`; sus propiedades JSON son las del codigo. El archivo del servicio `docs/openapi.yaml` contiene esta misma especificacion en sintaxis JSON, que tambien es YAML 1.2 valida.

Las fechas de reserva/pago/devolucion son LocalDateTime sin offset; el inicio del evento es OffsetDateTime. Los ejemplos de solicitud no son seeds ni autorizan a usar bases de desarrollo.
