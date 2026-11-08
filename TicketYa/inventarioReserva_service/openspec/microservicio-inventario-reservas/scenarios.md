# Escenarios de Prueba y Criterios de Aceptación

## Escenario 1: Inicialización de Inventario
- **Dado** un evento publicado con un sector de capacidad 500 no numerado.
- **Cuando** el microservicio de Eventos notifica la inicialización de inventario.
- **Entonces** se crea un registro de disponibilidad con 500 cupos en estado `DISPONIBLE`.

## Escenario 2: Reserva exitosa (Sectores Numerados / No Numerados)
- **Dado** un sector con disponibilidad.
- **Cuando** se solicita una reserva temporal para 2 entradas.
- **Entonces** se retoma `201 Created` con un `reservaId`, estado `PENDIENTE` y tiempo de expiración (10 min)[cite: 2, 8].

## Escenario 3: Prevención de Doble Venta (Prueba de Concurrencia)
- **Dado** 1 solo asiento libre en un sector numerado.
- **Cuando** 50 peticiones simultáneas intentan reservar ese mismo asiento.
- **Entonces** exactamente 1 petición tiene éxito (`201 Created`) y 49 peticiones fallan con error `409 Conflict`[cite: 4, 6, 8].

## Escenario 4: Expiración Automática por TTL
- **Dado** una reserva en estado `PENDIENTE` que superó los 10 minutos sin confirmación.
- **Cuando** se ejecuta el proceso programado (`@Scheduled`).
- **Entonces** la reserva cambia a estado `EXPIRADA` y los lugares vuelven a estar disponibles[cite: 2, 8].

## Escenario 5: Confirmación y Liberación por Pago
- **Cuando** el servicio de Compras notifica pago exitoso, la reserva pasa a `CONFIRMADA`[cite: 6, 8].
- **Cuando** el servicio de Compras notifica pago rechazado, la reserva pasa a `LIBERADA` y los lugares se reintegran al inventario[cite: 2, 6, 8].

## Escenario 6: Devolución y Cancelación de Evento
- **Cuando** se procesa una devolución aprobada, la ruta `/ocupaciones/liberar` incrementa la disponibilidad[cite: 2, 8].
- **Cuando** se cancela un evento, la ruta `/eventos/{id}/cancelar-inventario` inhabilita todo el stock[cite: 2, 8].