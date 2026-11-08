# Propuesta Técnica: Microservicio de Inventario y Reservas

## 1. Responsabilidad del Servicio
Es el microservicio responsable exclusivo de gestionar la disponibilidad de lugares (asientos numerados y aforos/cupos no numerados) y garantizar la prevención estricta de la doble venta[cite: 6, 8].

## 2. Alcance Funcional
- **Inicializar Inventario:** Crear la grilla de asientos numerados o el cupo total para un evento y sector al ser publicado[cite: 6, 8].
- **Reserva Temporal:** Bloquear temporalmente lugares (ej. 10 minutos) durante el proceso de compra[cite: 2, 6, 8].
- **Confirmación:** Confirmar de forma definitiva la ocupación de la reserva tras la aprobación del pago[cite: 2, 6, 8].
- **Liberación de Reserva:** Liberar lugares si el pago es rechazado, la compra se cancela o la reserva expira[cite: 2, 6, 8].
- **Control de Expiración (TTL):** Proceso automático que expira reservas pendientes cuyo tiempo expiró[cite: 8].
- **Gestión de Devoluciones:** Liberar lugares cuando Compras apruebe una devolución (hasta 72 hs antes del evento)[cite: 2, 6, 8].
- **Cancelación de Evento:** Liberar/invalidar todo el inventario de un evento si este se cancela[cite: 2, 6, 8].
- **Consultas de Disponibilidad:** Informar asientos libres o cupos disponibles por sector/evento[cite: 6, 8].

## 3. Arquitectura y Manejo de Concurrencia
- **Base de Datos:** Relacional independiente (ej. PostgreSQL) con esquema propio[cite: 2, 6].
- **Prevención de Doble Venta:**
  - *Sectores Numerados:* Restricción de unicidad (`UNIQUE (evento_id, sector_id, asiento_id)`) + `Pessimistic Locking` (`SELECT FOR UPDATE`) al intentar reservar[cite: 2, 6, 8].
  - *Sectores No Numerados:* Actualizaciones atómicas directas en la BD (`UPDATE sector_inventario SET disponibles = disponibles - N WHERE disponibles >= N`)[cite: 6, 8].
- **Resiliencia & Transacciones:** Operaciones aisladas y transaccionales (`@Transactional`) sin dependencia de sincronización en memoria de una sola instancia de Spring Boot[cite: 6, 8].