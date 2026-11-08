# Especificación funcional: Gestión de Devoluciones

## Purpose

Esta capacidad permite administrar las solicitudes de devolución de entradas de TicketYa, verificando las condiciones comerciales, gestionando su aprobación o rechazo y coordinando los reintegros y anulaciones correspondientes. También contempla las devoluciones derivadas de la cancelación de eventos.

## ADDED Requirements

### Requirement: Solicitud de devolución de entradas

El sistema MUST permitir que un cliente solicite la devolución de entradas adquiridas mediante una compra completada, identificando las entradas que desea devolver.

#### Scenario: Solicitud de devolución válida

- **GIVEN** que el cliente posee entradas vigentes correspondientes a una compra completada.
- **WHEN** solicita la devolución de una o varias de esas entradas.
- **THEN** el sistema registra una solicitud de devolución en estado pendiente de evaluación.

#### Scenario: Solicitud sobre una compra inexistente

- **WHEN** el cliente solicita devolver entradas de una compra inexistente.
- **THEN** el sistema rechaza la solicitud e informa que no se encontró la compra.

#### Scenario: Solicitud sobre entradas ajenas

- **GIVEN** que las entradas no pertenecen a la compra del cliente.
- **WHEN** el cliente solicita su devolución.
- **THEN** el sistema rechaza la operación.

#### Scenario: Asociación de precio histórico ambigua

- **GIVEN** que una entrada corresponde a varios detalles del mismo sector y asiento con precios históricos diferentes.
- **WHEN** se solicita su devolución sin una referencia inequívoca al detalle original.
- **THEN** el sistema rechaza la solicitud sin escoger arbitrariamente un precio ni registrar una devolución parcial.

### Requirement: Validación del plazo de devolución

El sistema MUST permitir las devoluciones voluntarias únicamente cuando se soliciten con una anticipación mínima de 72 horas respecto del inicio del evento.

#### Scenario: Solicitud dentro del plazo permitido

- **GIVEN** que faltan más de 72 horas para el inicio del evento.
- **WHEN** el cliente solicita devolver sus entradas.
- **THEN** el sistema permite registrar la solicitud si se cumplen las demás condiciones.

#### Scenario: Solicitud exactamente 72 horas antes del evento

- **GIVEN** que faltan exactamente 72 horas para el inicio del evento.
- **WHEN** el cliente solicita la devolución.
- **THEN** el sistema permite registrar la solicitud si se cumplen las demás condiciones.

#### Scenario: Solicitud fuera del plazo permitido

- **GIVEN** que faltan menos de 72 horas para el inicio del evento.
- **WHEN** el cliente solicita una devolución voluntaria.
- **THEN** el sistema rechaza la solicitud e informa que el plazo permitido ha vencido.

### Requirement: Prevención de devoluciones duplicadas

El sistema MUST impedir que una misma entrada sea reintegrada más de una vez o que existan solicitudes activas incompatibles sobre ella.

#### Scenario: Entrada con devolución pendiente

- **GIVEN** que una entrada ya tiene una solicitud de devolución pendiente.
- **WHEN** se intenta registrar otra solicitud sobre la misma entrada.
- **THEN** el sistema rechaza la nueva solicitud.

#### Scenario: Entrada ya devuelta

- **GIVEN** que una entrada ya fue devuelta y reintegrada.
- **WHEN** se solicita nuevamente su devolución.
- **THEN** el sistema rechaza la operación sin generar otro reintegro.

#### Scenario: Nueva solicitud después de rechazo

- **GIVEN** que todas las solicitudes anteriores sobre una entrada fueron rechazadas.
- **WHEN** el titular vuelve a solicitar la devolución cumpliendo el plazo y las demás condiciones.
- **THEN** el sistema permite una nueva solicitud y conserva el historial de rechazos.

### Requirement: Consulta de solicitudes de devolución

El sistema MUST permitir consultar una devolución por su identificador y obtener solicitudes filtradas por estado.

#### Scenario: Consulta de devolución existente

- **GIVEN** que existe una solicitud de devolución registrada.
- **WHEN** se consulta mediante su identificador.
- **THEN** el sistema devuelve la compra asociada, las entradas involucradas, el importe y el estado de la solicitud.

#### Scenario: Consulta de devolución inexistente

- **WHEN** se consulta un identificador de devolución inexistente.
- **THEN** el sistema informa que no se encontró la solicitud.

#### Scenario: Consulta de devoluciones por estado

- **GIVEN** que existen solicitudes de devolución con diferentes estados.
- **WHEN** se consultan las solicitudes filtrando por un estado determinado.
- **THEN** el sistema devuelve únicamente las solicitudes que cumplen el filtro.

### Requirement: Aprobación administrativa de devoluciones

El sistema MUST permitir que una solicitud de devolución pendiente sea aprobada mediante una operación administrativa autorizada.

#### Scenario: Aprobación de una solicitud pendiente

- **GIVEN** que existe una devolución pendiente que cumple las condiciones establecidas.
- **WHEN** un administrador autoriza la devolución.
- **THEN** el sistema registra su aprobación y habilita el procesamiento del reintegro correspondiente.
- **AND** conserva el motivo administrativo obligatorio y la fecha de resolución, separados del motivo del cliente.
- **AND** en el alcance 6.1–6.4 no ejecuta el reintegro ni la anulación.

#### Scenario: Aprobación repetida

- **GIVEN** que una devolución ya fue aprobada.
- **WHEN** se repite la operación de aprobación.
- **THEN** el sistema no genera una nueva autorización de reintegro ni duplica sus efectos.
- **AND** conserva motivo y fecha originales si la repetición es compatible; un motivo distinto produce conflicto.

#### Scenario: Aprobación de una devolución rechazada

- **GIVEN** que una solicitud de devolución fue rechazada definitivamente.
- **WHEN** se intenta aprobar mediante la operación habitual.
- **THEN** el sistema rechaza el cambio de estado.

### Requirement: Rechazo administrativo de devoluciones

El sistema MUST permitir rechazar solicitudes pendientes, registrando el motivo de la decisión.

#### Scenario: Rechazo de una solicitud pendiente

- **GIVEN** que existe una solicitud de devolución pendiente.
- **WHEN** un administrador la rechaza e informa un motivo.
- **THEN** el sistema registra el rechazo y conserva el motivo indicado.

#### Scenario: Rechazo de una devolución ya reintegrada

- **GIVEN** que una devolución ya fue reintegrada.
- **WHEN** se intenta rechazarla.
- **THEN** el sistema impide la operación y conserva su estado.

### Requirement: Reintegro de devoluciones aprobadas

El sistema MUST coordinar el reintegro completo del importe histórico de las entradas de cada devolución aprobada, sin superar el importe autorizado ni el efectivamente pagado y disponible. Las devoluciones parciales de una compra se expresan seleccionando menos entradas, no mediante varios reintegros monetarios de una misma solicitud.

#### Scenario: Reintegro exitoso

- **GIVEN** que una devolución fue aprobada y existe un pago aprobado con saldo suficiente.
- **WHEN** se procesa el reintegro simulado.
- **THEN** el sistema registra el importe reintegrado y actualiza el estado de la devolución.

#### Scenario: Error durante el reintegro

- **GIVEN** que una devolución fue aprobada.
- **WHEN** el procesamiento del reintegro falla o su resultado es desconocido.
- **THEN** el sistema conserva la operación pendiente de resolución y evita generar un segundo reintegro.

### Requirement: Anulación de entradas devueltas

El sistema MUST coordinar con el microservicio de Entradas la anulación de los tickets correspondientes a una devolución aprobada y reintegrada.

#### Scenario: Anulación exitosa

- **GIVEN** que una devolución fue aprobada y su importe fue reintegrado.
- **WHEN** el sistema solicita anular las entradas correspondientes.
- **THEN** las entradas quedan invalidadas para su utilización.

#### Scenario: Error al anular entradas

- **GIVEN** que el reintegro fue realizado.
- **WHEN** falla la comunicación con el microservicio de Entradas durante la anulación.
- **THEN** el sistema conserva la operación pendiente de resolución y evita considerar finalizada la devolución hasta confirmar la anulación.

### Requirement: Liberación de lugares por devolución

El sistema MUST coordinar con Reservas la liberación de los lugares asociados a entradas devueltas cuando las condiciones del evento permitan volver a ofrecerlos.

#### Scenario: Liberación exitosa de un asiento

- **GIVEN** que una entrada numerada fue devuelta y anulada correctamente.
- **WHEN** el sistema solicita liberar el asiento correspondiente.
- **THEN** el microservicio de Reservas vuelve a habilitar su disponibilidad, cuando corresponda.

#### Scenario: Liberación repetida

- **GIVEN** que un lugar ya fue liberado por una devolución.
- **WHEN** se repite la solicitud de liberación.
- **THEN** no se incrementa nuevamente la disponibilidad ni se produce una liberación duplicada.

### Requirement: Devoluciones por cancelación de eventos

El sistema MUST permitir procesar las devoluciones de entradas correspondientes a eventos cancelados, sin aplicar la restricción de 72 horas prevista para las devoluciones voluntarias.

#### Scenario: Cancelación de un evento con entradas vendidas

- **GIVEN** que un evento fue cancelado y existen compras completadas con entradas vigentes.
- **WHEN** se solicita procesar la cancelación del evento.
- **THEN** el sistema identifica las entradas afectadas e inicia las operaciones de devolución y reintegro correspondientes.

#### Scenario: Cancelación dentro de las 72 horas previas

- **GIVEN** que un evento fue cancelado cuando faltaban menos de 72 horas para su inicio.
- **WHEN** se procesan las devoluciones por cancelación.
- **THEN** el sistema permite los reintegros sin aplicar el plazo de devolución voluntaria.

#### Scenario: Procesamiento repetido de una cancelación

- **GIVEN** que las devoluciones de un evento cancelado ya fueron procesadas.
- **WHEN** se repite la solicitud de procesamiento.
- **THEN** el sistema no genera nuevas devoluciones, reintegros ni anulaciones de entradas para operaciones ya completadas.

### Requirement: Recuperación e identidad del reintegro

El sistema MUST persistir la identidad, el importe y la etapa antes de efectos externos, conservar resultados conocidos y recuperar operaciones incompletas sin asumir atomicidad distribuida.

#### Scenario: Respuesta de reintegro perdida

- **GIVEN** que la pasarela realizó el reintegro pero su respuesta no llegó.
- **WHEN** se repite el procesamiento con la misma clave e importe.
- **THEN** consulta la identidad original, registra su resultado y continúa sin otro reintegro.
- **AND** una consulta fallida no habilita enviar otra operación.

#### Scenario: Datos incompatibles o saldo comprometido

- **GIVEN** que existe una clave registrada o un reintegro de resultado desconocido.
- **WHEN** se reutiliza la clave con otros datos o se intenta superar el saldo incluyendo operaciones pendientes.
- **THEN** rechaza la operación sin nuevos efectos financieros.

#### Scenario: Anulación o liberación parcialmente completada

- **GIVEN** que el reintegro fue confirmado y una operación posterior falló o perdió su respuesta.
- **WHEN** se reintenta con la identidad original.
- **THEN** conserva el importe, consulta los efectos y retoma la etapa pendiente sobre las entradas seleccionadas.
- **AND** solo declara COMPLETADA después de confirmar los efectos aplicables.

#### Scenario: Rechazo financiero definitivo

- **GIVEN** que la pasarela rechazó el reintegro de manera definitiva.
- **WHEN** se repite el procesamiento.
- **THEN** conserva el rechazo sin reenviar ni anular entradas y solicita resolución administrativa.

#### Scenario: Evento iniciado o cancelado

- **GIVEN** que las entradas fueron reintegradas y anuladas, pero el evento ya comenzó o fue cancelado.
- **WHEN** se completa la coordinación.
- **THEN** no vuelve a ofrecer sus lugares.

#### Scenario: Cancelación con solicitudes existentes

- **GIVEN** que existen solicitudes pendientes, aprobadas, incompletas, completadas o rechazadas.
- **WHEN** se recibe la notificación administrativa simulada de cancelación.
- **THEN** aprueba las pendientes, reutiliza las vigentes y sus claves y crea solicitudes solo para las entradas restantes.
- **AND** conserva rechazos y operaciones completadas como historial sin duplicar importes.

#### Scenario: Cancelación con compras en curso

- **GIVEN** que existen compras sin pago o con un pago aún PROCESANDO.
- **WHEN** se recibe la cancelación.
- **THEN** reutiliza la cancelación existente para las compras sin pago y reporta las de pago en curso como pendientes.
- **AND** repetir la notificación después de recuperar el pago recoge su resultado sin considerar cerrada prematuramente la cancelación.
