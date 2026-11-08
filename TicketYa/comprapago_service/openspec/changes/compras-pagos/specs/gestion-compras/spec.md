# Especificación funcional: Gestión de Compras

## Purpose

Esta capacidad permite registrar, consultar y realizar el seguimiento de las compras de entradas de TicketYa. Su objetivo es garantizar que cada compra conserve la información del cliente, evento, reserva, entradas solicitadas, importes y estado, respetando las restricciones comerciales establecidas.

## ADDED Requirements

### Requirement: Creación de compras

El sistema MUST permitir que un cliente inicie una compra de entradas para un evento disponible, indicando el sector, la cantidad de entradas y los asientos seleccionados cuando corresponda.

#### Scenario: Creación exitosa de una compra

- **GIVEN** que el evento está publicado, los datos son válidos y existe una reserva temporal vigente.
- **WHEN** el cliente solicita iniciar una compra.
- **THEN** el sistema registra la compra en estado pendiente, asociándola al cliente, evento y reserva correspondiente.

#### Scenario: Compra de entradas para un evento no disponible

- **GIVEN** que el evento no está publicado, fue cancelado o ya finalizó.
- **WHEN** el cliente intenta iniciar una compra.
- **THEN** el sistema rechaza la operación e informa que el evento no se encuentra disponible.

#### Scenario: Solicitud de compra con datos inválidos

- **WHEN** el cliente solicita una compra con una cantidad de entradas inválida o sin identificar correctamente el sector.
- **THEN** el sistema rechaza la solicitud e informa los errores encontrados.

### Requirement: Registro del detalle de una compra

El sistema MUST registrar las entradas solicitadas en cada compra, conservando el evento, sector, cantidad, precio unitario y los asientos seleccionados cuando el sector sea numerado.

#### Scenario: Compra de entradas con asientos numerados

- **GIVEN** que el cliente seleccionó asientos válidos de un sector numerado.
- **WHEN** se registra la compra.
- **THEN** el sistema almacena el detalle de los asientos seleccionados y sus respectivos precios.

#### Scenario: Compra de entradas sin asientos numerados

- **GIVEN** que el cliente seleccionó un sector con capacidad general.
- **WHEN** se registra la compra.
- **THEN** el sistema almacena el sector, la cantidad de entradas y el precio unitario, sin requerir identificadores de asientos.

### Requirement: Cálculo del importe total

El sistema MUST calcular el importe total de cada compra utilizando los precios vigentes informados por el microservicio de Eventos.

#### Scenario: Cálculo correcto del importe

- **GIVEN** que el cliente seleccionó tres entradas cuyo precio unitario es de $10.000.
- **WHEN** el sistema calcula el importe de la compra.
- **THEN** el importe total registrado es de $30.000.

#### Scenario: Modificación no autorizada del precio

- **GIVEN** que el cliente envía un precio diferente del establecido para el evento.
- **WHEN** el sistema procesa la solicitud de compra.
- **THEN** utiliza el precio autorizado por el microservicio de Eventos y descarta el importe proporcionado por el cliente.

### Requirement: Control del límite de entradas por cliente

El sistema MUST verificar el límite por cliente/evento contando entradas vigentes y comprometidas, y descontando solo entradas con reintegro confirmado. Solicitudes o resultados financieros no confirmados no liberan cupo. Esta política no administra stock externo.

#### Scenario: Compra dentro del límite permitido

- **GIVEN** que la cantidad solicitada, sumada a las entradas previamente adquiridas por el cliente, no supera el límite establecido.
- **WHEN** el cliente solicita una nueva compra.
- **THEN** el sistema permite continuar con la operación.

#### Scenario: Compra que supera el límite permitido

- **GIVEN** que la cantidad solicitada supera el máximo de entradas permitido para el cliente.
- **WHEN** el cliente intenta iniciar la compra.
- **THEN** el sistema rechaza la operación e informa que se excedió el límite permitido.

#### Scenario: Estados que consumen cupo

- **GIVEN** que existen compras pendientes, en procesamiento, completadas o parcialmente devueltas.
- **WHEN** se registra otra compra para el mismo cliente/evento.
- **THEN** cuenta las cantidades originales de esos estados, descontando entradas distintas con reintegro confirmado.
- **AND** reconcilia pendientes cuyo vencimiento registrado ya pasó, según el comportamiento existente; FALLIDA, CANCELADA y DEVUELTA_TOTAL no consumen cupo.

#### Scenario: Recompra después de devolución parcial o total

- **GIVEN** que algunas o todas las entradas fueron reintegradas con resultado confirmado.
- **WHEN** el titular solicita otra compra del mismo evento.
- **THEN** solo contabiliza las entradas restantes y los compromisos activos, sin descontar dos veces una devolución repetida ni su historial de rechazos.

#### Scenario: Devolución sin confirmación financiera

- **GIVEN** que existe una solicitud pendiente o aprobada, o un reintegro rechazado o incierto.
- **WHEN** se evalúa el límite.
- **THEN** mantiene esas entradas en el conteo hasta confirmar el reintegro.

#### Scenario: Reintegro confirmado y coordinación pendiente

- **GIVEN** que el reintegro fue confirmado pero la anulación o liberación sigue en recuperación.
- **WHEN** se evalúa el límite.
- **THEN** descuenta las entradas reintegradas sin considerar finalizada su coordinación externa.

#### Scenario: Compras concurrentes consumen cupo devuelto

- **GIVEN** que un reintegro confirmado dejó un único lugar disponible dentro del límite del cliente.
- **WHEN** dos solicitudes distintas intentan consumir ese cupo simultáneamente.
- **THEN** solo acepta una mediante el control transaccional por cliente/evento.
- **AND** esta prueba del límite no reemplaza la prueba global de no doble venta del inventario.

### Requirement: Consulta de compras

El sistema MUST permitir consultar una compra mediante su identificador y obtener el historial de compras asociado a un cliente.

#### Scenario: Consulta de una compra existente

- **GIVEN** que existe una compra registrada.
- **WHEN** se consulta utilizando su identificador.
- **THEN** el sistema devuelve la información de la compra, incluyendo su detalle, importe y estado actual.

#### Scenario: Consulta de una compra inexistente

- **WHEN** se consulta un identificador de compra que no existe.
- **THEN** el sistema informa que no se encontró la compra solicitada.

#### Scenario: Consulta del historial de un cliente

- **GIVEN** que un cliente realizó varias compras.
- **WHEN** se solicita su historial de compras.
- **THEN** el sistema devuelve las compras asociadas al cliente.

#### Scenario: Cliente sin compras registradas

- **GIVEN** que un cliente no tiene compras registradas.
- **WHEN** se solicita su historial.
- **THEN** el sistema devuelve una colección vacía.

### Requirement: Seguimiento del estado de las compras

El sistema MUST registrar y permitir consultar el estado de cada compra, garantizando que los cambios de estado sean consistentes con el resultado del proceso de compra.

#### Scenario: Compra pendiente de pago

- **GIVEN** que existe una compra registrada y todavía no se confirmó su pago.
- **WHEN** se consulta el estado de la compra.
- **THEN** el sistema informa que se encuentra pendiente.

#### Scenario: Compra completada correctamente

- **GIVEN** que el pago fue aprobado, la reserva fue confirmada y las entradas fueron emitidas.
- **WHEN** finaliza correctamente el proceso de compra.
- **THEN** el sistema registra la compra como completada.

#### Scenario: Compra con pago rechazado

- **GIVEN** que el pago de una compra fue rechazado.
- **WHEN** el sistema recibe el resultado del procesamiento.
- **THEN** registra el estado correspondiente y evita marcar la compra como completada.

#### Scenario: Compra con reserva vencida

- **GIVEN** que la reserva temporal asociada a una compra venció antes de su confirmación.
- **WHEN** se intenta continuar con el proceso de compra.
- **THEN** el sistema impide completar la compra utilizando esa reserva.

### Requirement: Conservación del historial de compras

El sistema MUST conservar el registro de las compras realizadas, incluso cuando hayan sido rechazadas, canceladas o posteriormente devueltas, para permitir su seguimiento.

#### Scenario: Consulta de una compra cancelada

- **GIVEN** que una compra registrada fue cancelada.
- **WHEN** se consulta su información.
- **THEN** el sistema devuelve sus datos históricos y el estado de cancelación correspondiente.

#### Scenario: Consulta de una compra con devolución

- **GIVEN** que una compra tiene entradas devueltas.
- **WHEN** se consulta su información.
- **THEN** el sistema conserva el registro original e informa el estado actualizado de la operación.