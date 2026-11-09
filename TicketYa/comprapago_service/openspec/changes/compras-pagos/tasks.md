# Tasks

> Antecedente OpenSpec conservado. Los cortes y planes anteriores se identifican como históricos: la versión actual tiene API Gateway y clientes REST para Eventos, Reservas y Entradas. Ver [arquitectura vigente](../../../../../docs/entrega1/arquitectura.md) y [pruebas registradas](../../../../../docs/entrega1/pruebas.md). Las marcas del plan histórico no deben interpretarse como diagnóstico del estado actual.

## 1. Creación y configuración del microservicio

- [x] 1.1 Crear el proyecto `compras-service` utilizando Spring Boot, Java y Maven. Verificar que el proyecto compile mediante `mvn clean compile`.
- [x] 1.2 Incorporar las dependencias de Spring Web, Spring Data JPA, MySQL Driver, Validation, OpenAPI, JUnit y Mockito. Verificar que Maven resuelva correctamente las dependencias.
- [x] 1.3 Organizar los paquetes `controller`, `service`, `repository`, `entity`, `dto`, `client`, `config` y `exception`. Verificar que la estructura esté presente en el proyecto.
- [x] 1.4 Configurar la conexión a una base de datos MySQL independiente. Verificar que Spring Boot inicie y establezca correctamente la conexión.
- [x] 1.5 Configurar el manejo centralizado de errores HTTP y las validaciones de solicitudes. Verificar mediante pruebas que los errores produzcan respuestas consistentes.

## 2. Modelo de datos y persistencia

- [x] 2.1 Implementar las entidades `Compra` y `DetalleCompra`, incluyendo sus relaciones locales y referencias externas mediante identificadores. Verificar mediante una prueba JPA que puedan persistirse y recuperarse.
- [x] 2.2 Implementar la entidad `Pago`, incluyendo estados, importe y clave de idempotencia única. Verificar la persistencia y el rechazo de claves duplicadas mediante pruebas.
- [x] 2.3 Implementar las entidades `Devolucion`, `DetalleDevolucion` y `Reintegro`, con sus relaciones locales. Verificar mediante pruebas JPA que los registros se almacenen correctamente.
- [x] 2.4 Crear los repositorios JPA para las entidades del microservicio. Verificar las operaciones de consulta y almacenamiento mediante pruebas de repositorio.
- [x] 2.5 Incorporar enumeraciones para los estados de compras, pagos, devoluciones y reintegros. Verificar mediante pruebas que las transiciones inválidas sean rechazadas.
- [x] 2.6 Documentar el modelo entidad-relación de Compras y Pagos. Verificar que el diagrama represente las tablas, relaciones y restricciones implementadas.

## 3. Gestión de compras

- [x] 3.1 Definir los DTO de creación, consulta y detalle de compras. Verificar que las validaciones detecten solicitudes incompletas o incorrectas.
- [x] 3.2 Implementar la creación de compras pendientes, utilizando inicialmente datos de eventos y reservas simulados. Verificar mediante pruebas unitarias la creación exitosa y los rechazos por datos inválidos.
- [x] 3.3 Implementar el cálculo del importe total utilizando precios obtenidos mediante una abstracción del servicio de Eventos. Verificar mediante pruebas que los importes sean correctos y que no se acepten precios enviados por el cliente.
- [x] 3.4 Implementar el control del límite de entradas por cliente y evento mediante una política configurable. Verificar los casos dentro y fuera del límite y las solicitudes concurrentes.
- [x] 3.5 Implementar las consultas de compras por identificador y por cliente. Verificar mediante pruebas que devuelvan los registros correspondientes y contemplen compras inexistentes.
- [x] 3.6 Implementar los endpoints REST de compras y documentarlos con OpenAPI. Verificar sus respuestas mediante pruebas HTTP y Swagger.

- [x] 3.7 Contar entradas vigentes y comprometidas para el limite por cliente/evento, descontando solo entradas con reintegro confirmado. Verificar devoluciones parciales/totales, rechazos, incertidumbre y concurrencia sin cambiar el stock externo.

## 4. Procesamiento de pagos simulados

- [x] 4.1 Definir una interfaz para la pasarela de pagos y desarrollar una implementación simulada que permita aprobar o rechazar operaciones. Verificar ambos resultados mediante pruebas unitarias.
- [x] 4.2 Implementar la validación de las condiciones previas al pago, incluyendo compra pendiente y reserva vigente. Verificar que las compras canceladas, completadas o con reservas vencidas no puedan generar nuevos cobros.
- [x] 4.3 Implementar el registro y procesamiento de pagos, conservando importes, fechas y resultados. Verificar mediante pruebas los pagos aprobados y rechazados.
- [x] 4.4 Incorporar claves de idempotencia y restricciones de unicidad para pagos. Verificar que una solicitud repetida no genere un segundo cobro.
- [x] 4.5 Implementar protección frente a solicitudes simultáneas de pago sobre una misma compra. Verificar mediante una prueba de concurrencia que no se registren cobros aprobados duplicados.
- [x] 4.6 Implementar el tratamiento de resultados de pago desconocidos y su consulta posterior. Verificar mediante pruebas que un error de comunicación no produzca automáticamente un nuevo cobro.
- [x] 4.7 Implementar los endpoints REST de procesamiento y consulta de pagos, documentándolos con OpenAPI. Verificar sus respuestas mediante pruebas HTTP y Swagger.

- [x] 4.8 Registrar el medio de pago en solicitudes nuevas, persistencia y respuestas; comprobar su identidad en reintentos y preservar historicos sin dato. Verificar validaciones, HTTP, persistencia y ausencia de regresiones; actualizar OpenAPI, DER y especificaciones relacionadas.

## 5. Coordinación con Reservas y Entradas

- [x] 5.1 Definir interfaces y DTO internos para consultar, confirmar y liberar reservas, sin depender inicialmente de endpoints definitivos. Verificar su funcionamiento mediante clientes simulados.
- [x] 5.2 Definir interfaces y DTO internos para solicitar, consultar y anular entradas. Verificar su funcionamiento mediante clientes simulados.
- [x] 5.3 Implementar la validación de reservas temporales, comprobando vigencia y correspondencia con la compra. Verificar mediante pruebas los casos válidos, vencidos e incompatibles.
- [x] 5.4 Implementar la confirmación de reservas después de un pago aprobado. Verificar mediante pruebas que no se confirme una reserva cuando el pago fue rechazado.
- [x] 5.5 Implementar la solicitud de emisión de entradas después de confirmar la reserva. Verificar mediante pruebas que no se emitan entradas sin pago aprobado y reserva confirmada.
- [x] 5.6 Implementar la coordinación del circuito completo de compra utilizando estados persistidos. Verificar mediante pruebas que una compra solo quede completada después de confirmar pago, reserva y entradas.
- [x] 5.7 Implementar el tratamiento de errores y operaciones con resultado desconocido, incluyendo recuperación y compensación. Verificar mediante pruebas que los reintentos no dupliquen confirmaciones, emisiones ni cobros.
- [x] 5.8 Documentar el flujo de compra y sus alternativas mediante un diagrama de secuencia. Verificar que contemple el caso exitoso, el rechazo del pago y el vencimiento de la reserva.

Verificación de las secciones 1–5 (2026-11-05): `mvn clean compile` y `mvn verify`
correctos; batería final de 192 tests sin fallos, errores ni omisiones, utilizando
MySQL 8.4 aislado en el puerto 33079. Incluye HTTP real, Swagger/OpenAPI, concurrencia,
respuestas perdidas, recuperación y compensación persistida. Evidencia y límites en
`docs/informe-finalizacion.md`; diagramas en `docs/flujo-compra.md`.
Nota histórica del corte 1–5: en esa fecha 6 y 7 estaban pendientes. El estado actual incluye sección 6 completa y mantiene sección 7 pendiente.

Cierre de observaciones de la segunda auditoría: 1.5 cubre 406/500 con ErrorResponse;
3.2 verifica catálogo simulado publicado/no publicado/cancelado/finalizado y sectores
Long sin truncamiento; 5.7 distingue reintegro rechazado de desconocido y bloquea
reenvíos automáticos del rechazo definitivo con 409 para resolución manual.
El escenario de cancelación pendiente se verifica por HTTP, recuperación de
liberación y concurrencia con pago. Los diagramas responden después de liberar el turno.

## 6. Gestión de devoluciones

- [x] 6.1 Definir los DTO de solicitud, consulta, aprobación y rechazo de devoluciones. Verificar las validaciones mediante pruebas unitarias.
- [x] 6.2 Implementar el registro de solicitudes de devolución, verificando compra, titularidad y entradas seleccionadas. Verificar mediante pruebas las solicitudes válidas, inexistentes y duplicadas.
- [x] 6.3 Implementar la validación del plazo mínimo de 72 horas antes del evento. Verificar mediante pruebas los casos de más de 72 horas, exactamente 72 horas y menos de 72 horas.
- [x] 6.4 Implementar la aprobación y el rechazo administrativo de solicitudes, incluyendo el registro de motivos. Verificar mediante pruebas las transiciones permitidas y prohibidas.
- [x] 6.5 Implementar reintegros simulados parciales y totales, con claves de idempotencia. Verificar mediante pruebas que no se reintegren importes superiores a los pagados ni se dupliquen reintegros.
- [x] 6.6 Implementar la coordinación de anulación de entradas y liberación de lugares para devoluciones aprobadas. Verificar mediante pruebas que las operaciones repetidas no generen efectos duplicados.
- [x] 6.7 Implementar el procesamiento de cancelaciones de eventos, identificando compras afectadas y omitiendo la restricción de 72 horas. Verificar mediante pruebas los reintegros y el procesamiento repetido de una cancelación.
- [x] 6.8 Implementar el tratamiento de errores durante reintegros, anulaciones y liberaciones. Verificar mediante pruebas que las operaciones incompletas puedan recuperarse sin duplicaciones.
- [x] 6.9 Implementar los endpoints REST de devoluciones y reintegros, documentándolos con OpenAPI. Verificar mediante pruebas HTTP las respuestas de éxito, rechazo y conflicto.

Verificación 6.1–6.4 (2026-11-05): `mvn -o verify` correcto, 227 pruebas sin fallos,
errores ni omisiones sobre MySQL aislado en 127.0.0.1:33079/ticketya_compras_test.
Incluye DTO, titularidad, selección, importes históricos, solicitudes simultáneas,
72 horas con reloj UTC controlado, decisiones concurrentes e idempotentes y ausencia
de efectos financieros. Decisiones confirmadas y alcance en
una nota interna del corte 6.1–6.4 (no incluida en el repositorio). No se implementaron 6.5–6.9 ni sección 7.

Verificación 6.5–6.9 (2026-11-07): compilación y `mvn -o verify` correctos,
268 pruebas sin fallos, errores ni omisiones: las 227 anteriores y 41 nuevas
(25 de integración, 9 unitarias de simuladores y 7 HTTP/OpenAPI).
Solo se utilizó ticketya_compras_test en el contenedor aislado existente.
Se verificaron selección parcial/total, saldo, idempotencia, concurrencia, respuestas
perdidas, turnos vencidos, anulación/liberación selectivas y cancelaciones repetidas,
múltiples y recuperables. Las decisiones de reintegro completo por solicitud y
cancelación sin reofrecer lugares están documentadas en design.md y flujo-compra.md.
La nota anterior corresponde al corte histórico 6.1–6.4; esta entrega completa 6.5–6.9.
En aquella ejecución aún no se habían integrado los clientes reales ni Gateway. La integración actual posterior los incluye; la autenticación JWT continúa prevista para E2.

Preparación previa a integración (2026-11-07): se agregan las correcciones 3.7 y 4.8,
sin reabrir tareas anteriores ni completar sección 7. MedioPago registra el catálogo
acordado, protege la identidad de reintentos y conserva null en históricos sin dato.
El límite cuenta vigentes/comprometidas y descuenta únicamente reintegros confirmados,
según aprobación explícita del usuario. Compilación y `mvn -o verify`: 283 pruebas,
sin fallos, errores ni omisiones (268 escenarios anteriores adaptados y 15 nuevos).
Verificados HTTP/OpenAPI del medio, persistencia, recuperación histórica y concurrencia
del cupo devuelto sobre ticketya_compras_test. Contratos e inventario documental en
docs/flujo-compra.md. No se ejecutó migración sobre desarrollo.

## 7. Integración entre microservicios

- [ ] 7.1 Revisar con los integrantes del grupo los contratos REST reales de Eventos, Reservas y Entradas. Verificar que exista un acuerdo documentado sobre endpoints, DTO, identificadores y estados.
- [ ] 7.2 Reemplazar las implementaciones simuladas de los clientes externos por clientes REST compatibles con los contratos acordados. Verificar la comunicación con cada microservicio mediante pruebas de integración.
- [ ] 7.3 Configurar las direcciones de los microservicios, los tiempos de espera y el manejo de errores de comunicación. Verificar el comportamiento cuando un servicio no esté disponible.
- [ ] 7.4 Integrar Compras y Pagos con el API Gateway del grupo. Verificar que los endpoints sean accesibles mediante las rutas acordadas.
- [ ] 7.5 Ejecutar una prueba integral de compra exitosa desde la reserva hasta la emisión de entradas. Verificar que todos los servicios registren resultados consistentes.
- [ ] 7.6 Ejecutar pruebas integrales de pago rechazado, reserva vencida y errores de comunicación. Verificar que no se produzcan cobros, confirmaciones o emisiones duplicadas.
- [ ] 7.7 Ejecutar una prueba integral de devolución y otra de cancelación de evento. Verificar que los reintegros, anulaciones y liberaciones sean consistentes.
- [ ] 7.8 Ejecutar pruebas de concurrencia junto con Reservas para comprobar que dos clientes no puedan adquirir el mismo asiento. Verificar que solamente una operación pueda confirmar su adquisición.
- [ ] 7.9 Preparar una colección Postman y documentar los pasos para ejecutar Compras y Pagos. Verificar que otra persona pueda reproducir los escenarios principales siguiendo la documentación.

## Seguimiento posterior

- Revisar que las funcionalidades implementadas coincidan con las especificaciones de OpenSpec.
- Actualizar los documentos cuando se acuerden contratos o decisiones de integración que modifiquen el diseño.
- Archivar el cambio de OpenSpec después de completar las tareas, realizar las verificaciones y cumplir los requisitos de revisión del proyecto.
