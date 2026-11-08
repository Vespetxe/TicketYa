# Especificación funcional: Procesamiento de Pagos

## Purpose

Esta capacidad permite procesar y registrar los pagos asociados a las compras de entradas de TicketYa mediante una pasarela de pagos simulada. Su objetivo es garantizar que cada operación tenga un resultado identificable, que no se produzcan cobros duplicados y que las compras reflejen correctamente el resultado del pago.

## ADDED Requirements

### Requirement: Procesamiento de pagos de compras

El sistema MUST permitir procesar el pago de una compra pendiente, utilizando el importe registrado para esa operación y una reserva temporal vigente.

#### Scenario: Pago aprobado

- **GIVEN** que existe una compra pendiente con una reserva vigente.
- **WHEN** el cliente solicita pagar y la pasarela simulada aprueba la operación.
- **THEN** el sistema registra el pago como aprobado y permite continuar con la confirmación de la compra.

#### Scenario: Pago rechazado

- **GIVEN** que existe una compra pendiente con una reserva vigente.
- **WHEN** el cliente solicita pagar y la pasarela simulada rechaza la operación.
- **THEN** el sistema registra el pago como rechazado y no confirma la compra.

#### Scenario: Compra inexistente

- **WHEN** se solicita procesar el pago de una compra inexistente.
- **THEN** el sistema rechaza la solicitud e informa que no se encontró la compra.

### Requirement: Validación previa al pago

El sistema MUST verificar que la compra se encuentre en condiciones de ser pagada antes de iniciar una operación de pago.

#### Scenario: Compra con reserva vencida

- **GIVEN** que la reserva asociada a una compra está vencida.
- **WHEN** el cliente solicita procesar el pago.
- **THEN** el sistema rechaza la operación sin generar un cobro.

#### Scenario: Compra ya completada

- **GIVEN** que una compra ya fue completada y pagada.
- **WHEN** se solicita procesar nuevamente su pago.
- **THEN** el sistema no genera un nuevo cobro e informa que la compra ya fue pagada.

#### Scenario: Compra cancelada

- **GIVEN** que una compra se encuentra cancelada.
- **WHEN** el cliente intenta pagarla.
- **THEN** el sistema rechaza la operación sin generar un cobro.

### Requirement: Idempotencia de los pagos

El sistema MUST evitar que una misma solicitud de pago produzca múltiples cobros, incluso cuando el cliente repita la solicitud por un error de comunicación.

#### Scenario: Repetición de una solicitud aprobada

- **GIVEN** que una solicitud de pago ya fue procesada y aprobada.
- **WHEN** el cliente repite la solicitud utilizando la misma clave de idempotencia.
- **THEN** el sistema devuelve el resultado previamente registrado sin generar otro cobro.

#### Scenario: Repetición de una solicitud rechazada

- **GIVEN** que una solicitud de pago ya fue procesada y rechazada.
- **WHEN** el cliente repite la solicitud utilizando la misma clave de idempotencia.
- **THEN** el sistema devuelve el resultado previamente registrado sin procesar un nuevo pago.

#### Scenario: Solicitudes simultáneas de pago

- **GIVEN** que una compra pendiente recibe solicitudes simultáneas para procesar su pago.
- **WHEN** el sistema procesa las solicitudes.
- **THEN** garantiza que no se genere más de un cobro aprobado para la misma compra.

### Requirement: Registro de operaciones de pago

El sistema MUST conservar un registro de cada intento de pago, incluyendo su identificador, compra asociada, importe, resultado y fecha de procesamiento.

#### Scenario: Registro de pago aprobado

- **GIVEN** que la pasarela simulada aprueba una operación.
- **WHEN** el sistema registra el resultado.
- **THEN** conserva los datos del pago y su estado aprobado.

#### Scenario: Registro de pago rechazado

- **GIVEN** que la pasarela simulada rechaza una operación.
- **WHEN** el sistema registra el resultado.
- **THEN** conserva los datos del intento y su estado rechazado.

### Requirement: Medio de pago e identidad de la operación

El sistema MUST registrar el medio de cada pago nuevo como TARJETA_CREDITO, TARJETA_DEBITO, TRANSFERENCIA o BILLETERA_VIRTUAL, persistido mediante su nombre textual y expuesto en los DTO de respuesta. No almacena credenciales ni datos bancarios. El medio MUST permanecer inmutable al repetir o recuperar una operación.

#### Scenario: Pago nuevo con medio válido

- **GIVEN** que existe una compra pagable y el cliente informa un medio admitido.
- **WHEN** se registra el intento.
- **THEN** persiste el medio antes de cobrar y lo devuelve al procesar y consultar, tanto si el pago es aprobado como si es rechazado o queda incierto.

#### Scenario: Medio ausente o inválido en un pago nuevo

- **WHEN** se intenta crear un pago sin medio o con un valor ajeno al catálogo, vacío, numérico u objeto.
- **THEN** devuelve 400 sin registrar un pago ni cobrar.

#### Scenario: Repetición con otro medio

- **GIVEN** que una clave ya identifica un pago con un medio registrado.
- **WHEN** se repite la operación con otro medio.
- **THEN** devuelve 409 sin cambiar el dato original ni repetir efectos externos, incluso con resultado desconocido.
- **AND** omitir el medio registrado produce 400.

#### Scenario: Compatibilidad con pagos históricos sin medio

- **GIVEN** que un pago anterior no contiene el dato.
- **WHEN** se consulta o recupera con su compra y clave originales, omitiendo el medio.
- **THEN** conserva y devuelve null, sin atribuirle un medio ni crear otra operación.
- **AND** informar un medio en ese reintento produce 409: no completa retroactivamente datos desconocidos.
- **AND** incorporar la columna no elimina, reinicializa ni transforma los registros existentes; admite null para esos históricos.

### Requirement: Consulta de pagos

El sistema MUST permitir consultar una operación de pago mediante su identificador.

#### Scenario: Consulta de pago existente

- **GIVEN** que existe un pago registrado.
- **WHEN** se consulta utilizando su identificador.
- **THEN** el sistema devuelve la compra asociada, el importe, la fecha y el estado de la operación.

#### Scenario: Consulta de pago inexistente

- **WHEN** se consulta un identificador de pago inexistente.
- **THEN** el sistema informa que no se encontró la operación solicitada.

### Requirement: Procesamiento de reintegros

El sistema MUST permitir reintegrar pagos aprobados cuando exista una devolución autorizada o una cancelación de evento que justifique el reintegro.

#### Scenario: Reintegro aprobado

- **GIVEN** que existe un pago aprobado con un importe pendiente de reintegrar y una devolución autorizada.
- **WHEN** se solicita procesar el reintegro.
- **THEN** el sistema registra el reintegro simulado y actualiza el importe reintegrado.

#### Scenario: Reintegro de un pago rechazado

- **GIVEN** que un pago fue rechazado y no generó un cobro.
- **WHEN** se solicita reintegrar ese pago.
- **THEN** el sistema rechaza la operación porque no existe un importe cobrado que pueda reintegrarse.

#### Scenario: Reintegro duplicado

- **GIVEN** que un reintegro ya fue procesado correctamente.
- **WHEN** se repite la misma solicitud de reintegro.
- **THEN** el sistema no reintegra nuevamente el importe y devuelve el resultado previamente registrado.

#### Scenario: Reintegro superior al importe disponible

- **GIVEN** que el importe solicitado supera el saldo del pago que todavía puede reintegrarse.
- **WHEN** se intenta procesar el reintegro.
- **THEN** el sistema rechaza la operación sin modificar los importes registrados.

### Requirement: Manejo de errores en la pasarela simulada

El sistema MUST distinguir entre un pago rechazado y una operación cuyo resultado no pudo determinarse, evitando asumir que un error de comunicación equivale a un rechazo.

#### Scenario: Resultado de pago desconocido

- **GIVEN** que se inició una operación de pago.
- **WHEN** ocurre un error que impide conocer su resultado.
- **THEN** el sistema registra la operación como pendiente de verificación y no inicia automáticamente un segundo cobro.

#### Scenario: Recuperación del resultado de un pago

- **GIVEN** que una operación quedó pendiente de verificación.
- **WHEN** se obtiene posteriormente su resultado definitivo.
- **THEN** el sistema actualiza el estado del pago sin duplicar la operación original.