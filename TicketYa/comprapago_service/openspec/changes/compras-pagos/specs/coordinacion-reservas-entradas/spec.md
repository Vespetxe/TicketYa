# Especificación funcional: Coordinación de Reservas y Entradas

## Purpose

Esta capacidad permite coordinar el proceso de compra entre los microservicios de Compras y Pagos, Reservas y Entradas. Su objetivo es asegurar que las reservas se confirmen únicamente cuando corresponda, que las entradas se emitan para compras válidas y que los errores de comunicación no provoquen ventas o emisiones duplicadas.

## ADDED Requirements

### Requirement: Validación de reservas temporales

El sistema MUST verificar que la reserva asociada a una compra exista, esté vigente y corresponda al evento, sector y entradas solicitadas antes de continuar con el pago.

#### Scenario: Reserva válida

- **GIVEN** que existe una reserva temporal vigente asociada a la compra.
- **WHEN** el sistema consulta su validez.
- **THEN** permite continuar con el proceso de pago.

#### Scenario: Reserva inexistente

- **GIVEN** que el identificador de reserva no corresponde a una reserva existente.
- **WHEN** el sistema intenta validarla.
- **THEN** rechaza la operación e informa que la reserva no existe.

#### Scenario: Reserva vencida

- **GIVEN** que la reserva temporal superó su tiempo de vigencia.
- **WHEN** el sistema intenta utilizarla para una compra.
- **THEN** impide continuar con el pago.

#### Scenario: Reserva perteneciente a otra operación

- **GIVEN** que la reserva corresponde a otro cliente, evento o selección de entradas.
- **WHEN** se intenta asociarla a una compra.
- **THEN** el sistema rechaza la operación.

### Requirement: Confirmación de reservas

El sistema MUST solicitar al microservicio de Reservas la confirmación de los lugares asociados a una compra cuyo pago fue aprobado.

#### Scenario: Confirmación exitosa

- **GIVEN** que el pago de una compra fue aprobado y su reserva puede confirmarse.
- **WHEN** Compras solicita la confirmación al microservicio de Reservas.
- **THEN** los lugares quedan confirmados para esa compra.

#### Scenario: Confirmación rechazada

- **GIVEN** que el microservicio de Reservas rechaza la confirmación.
- **WHEN** Compras recibe la respuesta.
- **THEN** no considera la compra completada e inicia el tratamiento correspondiente para resolver el pago aprobado.

#### Scenario: Confirmación repetida

- **GIVEN** que una reserva ya fue confirmada para una compra.
- **WHEN** Compras repite la solicitud de confirmación.
- **THEN** la operación no genera una segunda confirmación ni modifica la titularidad de los lugares.

### Requirement: Liberación de reservas

El sistema MUST solicitar la liberación de las reservas temporales cuando una compra no pueda completarse y los lugares todavía no estén confirmados.

#### Scenario: Liberación por pago rechazado

- **GIVEN** que el pago de una compra fue rechazado.
- **WHEN** el sistema determina que la operación no continuará.
- **THEN** solicita la liberación de la reserva temporal correspondiente.

#### Scenario: Liberación por cancelación de compra

- **GIVEN** que una compra pendiente es cancelada.
- **WHEN** se procesa la cancelación.
- **THEN** el sistema solicita liberar la reserva temporal asociada.

#### Scenario: Reserva previamente liberada

- **GIVEN** que una reserva ya fue liberada.
- **WHEN** se repite la solicitud de liberación.
- **THEN** el sistema evita producir efectos adicionales sobre la disponibilidad.

#### Scenario: Recuperación de cancelación de compra pendiente

- **GIVEN** que la cancelación fue registrada y la liberación de reserva no pudo verificarse.
- **WHEN** se repite la cancelación de la misma compra.
- **THEN** consulta y recupera la liberación sin habilitar pagos ni duplicar sus efectos.

### Requirement: Emisión de entradas

El sistema MUST solicitar al microservicio de Entradas la emisión de los tickets correspondientes únicamente después de que el pago haya sido aprobado y la reserva confirmada.

#### Scenario: Emisión exitosa

- **GIVEN** que una compra tiene el pago aprobado y la reserva confirmada.
- **WHEN** Compras solicita la emisión de entradas.
- **THEN** el microservicio de Entradas genera los tickets correspondientes y Compras registra sus identificadores.

#### Scenario: Intento de emisión sin pago aprobado

- **GIVEN** que una compra no tiene un pago aprobado.
- **WHEN** se intenta solicitar la emisión de entradas.
- **THEN** el sistema impide la operación.

#### Scenario: Intento de emisión sin reserva confirmada

- **GIVEN** que la reserva de una compra no fue confirmada.
- **WHEN** se intenta emitir las entradas.
- **THEN** el sistema impide la emisión.

### Requirement: Prevención de emisiones duplicadas

El sistema MUST garantizar que las solicitudes repetidas de emisión correspondientes a una misma compra no generen entradas adicionales.

#### Scenario: Solicitud de emisión repetida

- **GIVEN** que las entradas de una compra ya fueron emitidas.
- **WHEN** se repite la solicitud de emisión.
- **THEN** el sistema recupera las entradas existentes sin solicitar nuevas emisiones.

#### Scenario: Solicitudes simultáneas de emisión

- **GIVEN** que se reciben solicitudes simultáneas para completar una misma compra.
- **WHEN** se procesa la emisión de entradas.
- **THEN** se genera únicamente la cantidad de entradas originalmente adquirida.

### Requirement: Manejo de errores entre microservicios

El sistema MUST conservar el estado de las operaciones cuando se produzcan errores de comunicación, evitando asumir que una solicitud fallida no fue procesada por el servicio destinatario.

#### Scenario: Error al confirmar una reserva

- **GIVEN** que Compras solicita confirmar una reserva.
- **WHEN** ocurre un error de comunicación y no se conoce el resultado.
- **THEN** el sistema conserva la operación pendiente de verificación y no la considera completada.

#### Scenario: Error al emitir entradas

- **GIVEN** que Compras solicita emitir entradas.
- **WHEN** ocurre un error de comunicación y no se conoce el resultado.
- **THEN** el sistema conserva la operación pendiente de verificación y evita solicitar una emisión duplicada.

#### Scenario: Recuperación después de un error

- **GIVEN** que una operación quedó pendiente por un error de comunicación.
- **WHEN** se restablece la comunicación y se consulta el resultado.
- **THEN** el sistema continúa desde el estado confirmado sin repetir operaciones ya completadas.

### Requirement: Compensación de operaciones incompletas

El sistema MUST gestionar las acciones necesarias para resolver compras que no puedan completarse después de haberse aprobado un pago.

#### Scenario: Pago aprobado y reserva no confirmada

- **GIVEN** que el pago fue aprobado, pero la reserva no pudo confirmarse definitivamente.
- **WHEN** el sistema determina que la compra no puede completarse.
- **THEN** inicia el reintegro correspondiente y evita marcar la compra como completada.

#### Scenario: Pago aprobado y error en emisión

- **GIVEN** que el pago fue aprobado y la reserva fue confirmada, pero la emisión de entradas falló.
- **WHEN** el sistema detecta que la compra permanece incompleta.
- **THEN** conserva el estado de la operación y gestiona su recuperación o compensación, sin generar entradas duplicadas.

#### Scenario: Reintegro de compensación explícitamente rechazado

- **GIVEN** que la pasarela informa un rechazo definitivo del reintegro de una compra fallida.
- **WHEN** se registra el resultado de la compensación.
- **THEN** conserva el pago como cobrado, registra el reintegro como rechazado y mantiene la compra incompleta.
- **AND** informa un conflicto que requiere resolución manual, sin clasificarlo como resultado desconocido ni iniciar otro cobro o reintegro automático.

### Requirement: Consistencia del resultado de la compra

El sistema MUST considerar una compra completada únicamente cuando el pago esté aprobado, la reserva confirmada y las entradas emitidas correctamente.

#### Scenario: Proceso completo exitoso

- **GIVEN** que el pago está aprobado, la reserva confirmada y las entradas emitidas.
- **WHEN** Compras verifica el resultado de todas las operaciones.
- **THEN** registra la compra como completada.

#### Scenario: Proceso parcialmente completado

- **GIVEN** que alguna de las operaciones necesarias continúa pendiente o falló.
- **WHEN** se consulta el estado de la compra.
- **THEN** el sistema informa un estado que refleja la operación incompleta, sin presentarla como completada.
