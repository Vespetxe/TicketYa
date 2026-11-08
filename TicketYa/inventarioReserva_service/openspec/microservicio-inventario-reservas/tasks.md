# Checklist de Tareas para Desarrollo

- [x] **1. Configuración de Proyecto:**
    - [x] Proyecto Spring Boot independiente con Java 17/21[cite: 2].
    - [x] Configurar conexión a la BD PostgreSQL exclusiva de Reservas[cite: 2, 6].
    - [x] Configurar Swagger / OpenAPI UI[cite: 4, 6].

- [x] **2. Entidades y Persistencia JPA:**
    - [x] Crear entidad `InventarioSector` (eventoId, sectorId, capacidadTotal, disponibles)[cite: 6].
    - [x] Crear entidad `Asiento` (fila, numero, estado) con índice único `(evento_id, sector_id, asiento_id)`[cite: 6].
    - [x] Crear entidad `Reserva` y `DetalleReserva` (clienteId, fechaCreacion, fechaExpiracion, estado)[cite: 6, 8].

- [x] **3. Desarrollo de Endpoints y Lógica (las 9 rutas):**
    - [x] Implementar `POST /eventos/{eventoId}/inventario`[cite: 8].
    - [x] Implementar `POST /reservas` (con Bloqueo Pesimista / Queries Atómicas)[cite: 6, 8].
    - [x] Implementar `GET /reservas/{id}` y `GET /reservas?clienteId={clienteId}`[cite: 8].
    - [x] Implementar `POST /reservas/{id}/confirmar` y `POST /reservas/{id}/liberar`[cite: 8].
    - [x] Implementar `GET /disponibilidad/eventos/{eventoId}/sectores/{sectorId}`[cite: 8].
    - [x] Implementar `POST /ocupaciones/liberar` (Devoluciones)[cite: 8].
    - [x] Implementar `POST /eventos/{eventoId}/cancelar-inventario`[cite: 8].

- [x] **4. Automatización de Expiración:**
    - [x] Activar `@EnableScheduling` y crear `@Scheduled` cada N segundos para marcar reservas vencidas y restablecer stock[cite: 8].

- [x] **5. Testing Obligatorio:**
    - [x] Unit tests de lógica de expiración y cambios de estado[cite: 6].
    - [x] Integration test de concurrencia simulando múltiples hilos intentando comprar el mismo lugar para validar la no doble venta[cite: 4, 6].