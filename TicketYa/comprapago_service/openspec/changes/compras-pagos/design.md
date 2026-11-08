# Diseño técnico: Microservicio de Compras y Pagos

## Context

El microservicio de Compras y Pagos forma parte de la arquitectura distribuida de TicketYa, compuesta por los microservicios de Eventos, Reservas, Compras y Pagos, Entradas y un API Gateway.

Estado actual: secciones 1–6 implementadas con clientes simulados; contratos REST reales e integración de la sección 7 pendientes. Las responsabilidades funcionales del microservicio están definidas en `proposal.md` y en las especificaciones correspondientes a las capacidades `gestion-compras`, `procesamiento-pagos`, `coordinacion-reservas-entradas` y `gestion-devoluciones`.

Cada microservicio administra su propia base de datos y se comunica con los demás mediante APIs REST. Por lo tanto, Compras y Pagos debe conservar la información de sus operaciones y coordinar acciones externas sin depender de transacciones de base de datos compartidas.

La primera entrega requiere implementar el circuito de compra, pagos simulados, emisión de entradas, devoluciones y cancelaciones de eventos.

## Goals / Non-Goals

### Objetivos técnicos

- Desarrollar un microservicio Spring Boot independiente y ejecutable.
- Separar las responsabilidades de acceso REST, lógica de negocio, persistencia y comunicación con otros servicios.
- Persistir compras, detalles, pagos, devoluciones y estados de las operaciones.
- Garantizar idempotencia en pagos, reintegros y operaciones que puedan repetirse.
- Coordinar la confirmación de reservas y emisión de entradas sin generar operaciones duplicadas.
- Permitir recuperar operaciones interrumpidas por errores de comunicación.
- Mantener contratos REST documentados mediante OpenAPI.
- Facilitar pruebas unitarias e integración con los microservicios del grupo.

### Fuera del alcance

- Implementar una pasarela de pagos real.
- Administrar directamente la disponibilidad de asientos.
- Generar o validar códigos QR dentro de Compras y Pagos.
- Administrar el catálogo de eventos y sus precios.
- Compartir entidades JPA o tablas con otros microservicios.
- Implementar en esta etapa una infraestructura de mensajería distribuida.
- Incorporar las funcionalidades adicionales de la segunda entrega, como JWT, cola virtual, recomendaciones o notificaciones.

## Decisions

### 1. Organización del proyecto

**Decisión:** utilizar Java, Spring Boot y Maven, con una arquitectura monolítica modular dentro del propio microservicio.

Se organizará el código en los siguientes componentes:

- `controller`: recepción de solicitudes HTTP y devolución de respuestas.
- `service`: aplicación de reglas de negocio y coordinación de operaciones.
- `repository`: acceso a los datos persistidos.
- `entity`: representación de las entidades de la base de datos.
- `dto`: contratos de entrada y salida de la API.
- `client`: comunicación REST con otros microservicios.
- `config`: configuración técnica.
- `exception`: manejo centralizado de errores.

Esta separación permite mantener responsabilidades claras y facilita la implementación de pruebas.

**Alternativa considerada:** concentrar la lógica en controladores y repositorios. Se descarta porque dificultaría el mantenimiento, las pruebas y la coordinación con otros servicios.

### 2. Base de datos independiente

**Decisión:** utilizar MySQL como base de datos relacional exclusiva del microservicio, con Spring Data JPA e Hibernate.

Se propone el siguiente modelo de entidades:

**Compra**

- `id`: identificador de la compra.
- `clienteId`: identificador del cliente.
- `eventoId`: identificador del evento.
- `reservaId`: identificador de la reserva temporal.
- `fechaCreacion`: fecha y hora de creación.
- `importeTotal`: importe total registrado.
- `estado`: estado actual de la compra.
- `version`: campo para control de concurrencia optimista.

**DetalleCompra**

- `id`: identificador del detalle.
- `compra`: referencia local a Compra.
- `sectorId`: identificador del sector.
- `asientoId`: identificador del asiento, cuando corresponda.
- `cantidad`: cantidad de entradas representadas por el detalle.
- `precioUnitario`: precio aplicado.
- `subtotal`: importe del detalle.

**Pago**

- `id`: identificador del pago.
- `compra`: referencia local a Compra.
- `claveIdempotencia`: identificador único de la solicitud de pago.
- `medioPago`: enum textual; obligatorio en intentos nuevos, null solo en históricos sin información.
- `importe`: importe procesado.
- `estado`: estado del pago.
- `fechaCreacion`: fecha de registro.
- `fechaProcesamiento`: fecha del resultado, cuando exista.
- `referenciaExterna`: identificador devuelto por la pasarela simulada.

**Devolucion**

- `id`: identificador de la solicitud.
- `compra`: referencia local a Compra.
- `fechaSolicitud`: fecha de presentación.
- `estado`: estado de la devolución.
- `motivo`: motivo informado.
- `motivoRechazo`: justificación administrativa, cuando corresponda.
- `importeSolicitado`: importe correspondiente a las entradas seleccionadas.
- `importeReintegrado`: importe efectivamente reintegrado.

**DetalleDevolucion**

- `id`: identificador del detalle.
- `devolucion`: referencia local a Devolucion.
- `detalleCompra`: referencia local al detalle original.
- `entradaId`: identificador de la entrada emitida.
- `importe`: importe correspondiente a la entrada.

**Reintegro**

- `id`: identificador del reintegro.
- `pago`: referencia local al pago original.
- `devolucion`: referencia local a la devolución, cuando corresponda.
- `claveIdempotencia`: identificador único de la operación.
- `importe`: importe a reintegrar.
- `estado`: estado del reintegro.
- `fechaProcesamiento`: fecha del resultado, cuando exista.

Se utilizará `BigDecimal` para los importes monetarios y tipos de fecha y hora adecuados para almacenar instantes de manera consistente.

Las referencias a Eventos, Reservas y Entradas se conservarán mediante identificadores. No se crearán relaciones JPA entre bases de datos de distintos microservicios.

**Alternativa considerada:** utilizar una base de datos compartida. Se descarta porque contradice la independencia de persistencia acordada para la arquitectura.

### 3. Estados de las operaciones

**Decisión:** representar los estados mediante enumeraciones y validar explícitamente sus transiciones.

Se proponen los siguientes estados:

| Operación | Estados |
|---|---|
| Compra | `PENDIENTE`, `PROCESANDO`, `COMPLETADA`, `FALLIDA`, `CANCELADA`, `DEVUELTA_PARCIAL`, `DEVUELTA_TOTAL` |
| Pago | `PENDIENTE`, `APROBADO`, `RECHAZADO`, `RESULTADO_DESCONOCIDO`, `REINTEGRADO_PARCIAL`, `REINTEGRADO_TOTAL` |
| Devolución | `PENDIENTE`, `APROBADA`, `RECHAZADA`, `PROCESANDO`, `COMPLETADA`, `ERROR_PENDIENTE` |
| Reintegro | `PENDIENTE`, `COMPLETADO`, `RECHAZADO`, `RESULTADO_DESCONOCIDO` |

Una compra solamente podrá pasar a `COMPLETADA` cuando se hayan confirmado el pago, la reserva y la emisión de las entradas.

Los estados intermedios permitirán identificar operaciones pendientes de recuperación.

Las transiciones serán controladas por la capa de servicios, evitando modificaciones arbitrarias.

**Alternativa considerada:** representar los estados mediante cadenas de texto sin restricciones. Se descarta por el riesgo de inconsistencias y transiciones inválidas.

### 4. API REST

**Decisión:** exponer una API REST documentada con OpenAPI/Swagger y accesible externamente mediante el API Gateway.

Se propone el siguiente conjunto inicial de endpoints:

| Método | Endpoint | Funcionalidad |
|---|---|---|
| POST | `/compras` | Crear una compra |
| GET | `/compras/{id}` | Consultar una compra |
| GET | `/compras?clienteId={id}` | Consultar compras de un cliente |
| POST | `/pagos` | Procesar o recuperar un pago por compraId y claveIdempotencia |
| GET | `/compras/{id}/estado` | Consultar el estado |
| GET | `/compras/{id}/detalle` | Consultar el detalle |
| POST | `/compras/{id}/cancelar` | Cancelar o recuperar la liberación de una compra pendiente |
| GET | `/pagos/{id}` | Consultar un pago |
| POST | `/admin/devoluciones/{id}/procesar` | Procesar o recuperar la devolución y su reintegro autorizado |
| GET | `/admin/devoluciones/{id}/procesamiento` | Consultar avance y reintegro |
| GET | `/admin/reintegros/{id}` | Consultar un reintegro |
| POST | `/devoluciones` | Solicitar una devolución |
| GET | `/devoluciones/{id}` | Consultar una devolución |
| GET | `/admin/devoluciones?estado={estado}` | Filtrar devoluciones |
| POST | `/admin/devoluciones/{id}/aprobar` | Aprobar una devolución |
| POST | `/admin/devoluciones/{id}/rechazar` | Rechazar una devolución |
| POST | `/admin/eventos/{id}/cancelacion` | Procesar una cancelación |

Los cuerpos de las solicitudes y respuestas se definirán mediante DTO independientes de las entidades de persistencia.

Se utilizarán códigos HTTP coherentes con cada resultado, incluyendo errores de validación, recursos inexistentes y conflictos de estado.

Las operaciones administrativas deberán estar diferenciadas de las operaciones disponibles para clientes. La autenticación y autorización definitiva mediante JWT se incorporará en la segunda entrega; durante la primera se deberá restringir el acceso administrativo mediante un mecanismo acordado por el grupo y no exponerlo libremente en un despliegue público.

**Alternativa considerada:** exponer directamente entidades JPA. Se descarta para evitar acoplamiento entre el modelo interno y los contratos externos.

### 5. Comunicación entre microservicios

**Decisión:** utilizar comunicación REST sincrónica mediante clientes HTTP encapsulados en componentes específicos.

Compras y Pagos consumirá las funcionalidades necesarias de:

- **Eventos:** consulta del evento, su disponibilidad comercial, fecha de inicio, sectores, precios y restricciones.
- **Reservas:** consulta de reservas, confirmación, liberación y verificación de resultados.
- **Entradas:** emisión de entradas, consulta de entradas emitidas y anulación.

Los clientes REST tendrán tiempos máximos de espera definidos y distinguirán entre respuestas de negocio y fallas de comunicación.

Los contratos concretos deberán acordarse con los responsables de cada microservicio antes de implementar la integración.

**Alternativa considerada:** acceso directo a las bases de datos de otros servicios. Se descarta porque rompe el aislamiento y el encapsulamiento de responsabilidades.

### 6. Coordinación del proceso de compra

**Decisión:** utilizar una coordinación de operaciones con estados persistidos y acciones de recuperación o compensación.

El flujo principal será:

1. El cliente dispone de una reserva temporal vigente.
2. Compras valida la información comercial y la reserva.
3. Compras registra la operación pendiente.
4. Se procesa el pago simulado.
5. Si el pago es aprobado, se solicita confirmar la reserva.
6. Una vez confirmada, se solicita emitir las entradas.
7. Cuando todas las operaciones finalizan correctamente, la compra pasa a `COMPLETADA`.

Ante un pago rechazado, la compra no se completa y se solicita liberar la reserva.

Si el pago fue aprobado pero la reserva no puede confirmarse definitivamente, se inicia el reintegro correspondiente.

Si la emisión de entradas falla o su resultado es desconocido, se conserva el estado intermedio y se intenta verificar el resultado antes de repetir o compensar la operación.

Las acciones externas utilizarán identificadores de operación estables para facilitar la idempotencia.

No se mantendrá una transacción de base de datos abierta mientras se espera la respuesta de otro microservicio.

**Alternativa considerada:** utilizar una transacción distribuida entre las bases de datos. Se descarta por su complejidad y porque no corresponde al modelo de independencia adoptado.

### 7. Idempotencia y concurrencia

**Decisión:** utilizar claves de idempotencia persistidas, restricciones de unicidad y control de concurrencia en las operaciones críticas.

Para pagos y reintegros:

- Cada solicitud tendrá una clave de idempotencia.
- La clave se almacenará con una restricción única.
- Una solicitud repetida devolverá el resultado de la operación original.
- Una clave reutilizada con datos incompatibles producirá un error de conflicto.
- Los resultados desconocidos deberán verificarse antes de iniciar una nueva operación.

Para compras:

- Se impedirá confirmar o pagar dos veces una misma compra.
- Se utilizará control de concurrencia sobre los registros involucrados.
- La validación del límite de entradas por cliente deberá considerar solicitudes simultáneas.

La prevención definitiva de doble venta de lugares corresponde al microservicio de Reservas.

**Alternativa considerada:** realizar únicamente verificaciones previas mediante consultas. Se descarta porque dos solicitudes simultáneas podrían superar las validaciones antes de guardar sus resultados.

#### Correcciones propias antes de integración (2026-11-07)

MedioPago se representa como enum persistido por nombre. Se eligió frente a texto libre
porque valida un catálogo pequeño y frente a una entidad de catálogo porque no se
necesita administración dinámica. Valores acordados: TARJETA_CREDITO, TARJETA_DEBITO,
TRANSFERENCIA y BILLETERA_VIRTUAL. No representa cuentas, tokens ni credenciales.
POST /pagos agrega medioPago, obligatorio para nuevos intentos; PagoDTO lo devuelve.
La validación es condicional en el servicio y se repite bajo bloqueo al registrar:
el DTO permite ausencia exclusivamente para recuperar históricos sin dato. Una clave
con otro medio produce 409 antes de cobrar; omitir un medio existente produce 400.
Los históricos conservan null, incluso al recuperarse; informarles un medio produce
conflicto. No se usa un valor por defecto ni se infiere de una operación externa.
La pasarela simulada conserva su contrato de clave e importe y su comportamiento;
la clasificación comercial se conserva en Compras. El mapeo a datos de un futuro
proveedor es PENDIENTE DE INTEGRACIÓN, no un contrato bancario inventado.

La política de límite fue aprobada explícitamente: contar entradas vigentes y
comprometidas, descontando solo entradas con Reintegro COMPLETADO. Se mantiene
el bloqueo por cliente/evento y la unicidad por reserva. Una consulta adicional
cuenta entradas reintegradas distintas de las compras activas, con filtro por
titular/evento y resultado confirmado; no duplica filas por historial de rechazos.
Una devolución no financiera no reduce el conteo, y la repetición idempotente no
libera cupo nuevamente. Un reintegro confirmado sí lo reduce aunque la coordinación
externa siga pendiente. Las compras DEVUELTA_TOTAL ya quedan fuera de los estados
activos. El historial de cantidades de DetalleCompra no se modifica ni elimina.
No se duplica inventario de Reservas ni se afirma no doble venta global.

### 8. Gestión de devoluciones

**Decisión:** administrar las devoluciones mediante solicitudes persistidas y operaciones de reintegro independientes.

El flujo de devolución voluntaria será:

1. El cliente identifica la compra y las entradas que desea devolver.
2. Compras verifica la titularidad, el estado de las entradas y el plazo mínimo de 72 horas.
3. Se registra la solicitud de devolución.
4. Un administrador aprueba o rechaza la solicitud.
5. Si se aprueba, se procesa el reintegro simulado.
6. Se solicita la anulación de las entradas correspondientes.
7. Se coordina la liberación de lugares cuando corresponda.
8. La devolución se considera completada al confirmarse las operaciones necesarias.

Se permitirán devoluciones parciales, conservando la relación entre cada entrada devuelta y su detalle de compra original.

Las cancelaciones de eventos utilizarán un proceso específico que identificará las compras afectadas y permitirá reintegros sin aplicar la restricción de 72 horas.

Los reintegros, anulaciones y liberaciones deberán ser idempotentes.

**Alternativa considerada:** eliminar los registros de compras y entradas devueltas. Se descarta porque impediría conservar el historial y controlar reintegros duplicados.

#### Implementación acotada 6.1–6.4

Se reutilizan las entidades y repositorios de devoluciones, los IDs de entradas
persistidos en Compra y el bloqueo por compra. Los clientes solo se consultan fuera
de la transacción de registro. EventoClient incorpora una consulta de inicio UTC;
el simulador conserva la programación en su diario para no extender el plazo al reiniciar.

Las decisiones confirmadas para esta etapa son:
- Una solicitud RECHAZADA permite volver a solicitar esas entradas si todavía se
  cumplen las 72 horas. Cualquier estado distinto de RECHAZADA bloquea duplicaciones.
- Aprobación y rechazo exigen motivo administrativo separado del motivo original,
  con fecha de resolución. Repeticiones compatibles conservan motivo y fecha;
  una decisión opuesta o un motivo diferente produce conflicto.
- Se mantiene EntradaDTO. El importe usa el precio histórico del sector/asiento;
  asociaciones con precios distintos se rechazan. Detalles equivalentes se asignan
  por IDs ordenados y capacidad para mantener estabilidad entre solicitudes parciales.

DevolucionService y DevolucionRegistroService implementan solicitud, consulta y
resolución local; aprobar no invoca reintegros ni modifica entradas. Los endpoints
se incorporan en 6.9; la autorización real queda pendiente de la integración de seguridad.
El plazo se evalúa al solicitar (incluyendo exactamente 72 horas), no al resolver.
La documentación operativa está en docs/devoluciones-6-1-6-4.md.

#### Implementación 6.5–6.9

Cada devolución reintegra todo su importe autorizado; lo parcial se expresa mediante
selecciones disjuntas de entradas, con importes históricos. Una compra DEVUELTA_PARCIAL
admite nuevas solicitudes sobre sus entradas restantes. Se preserva la asociación
histórica y la prohibición de solicitudes activas superpuestas.

ReintegroService orquesta fuera de transacciones. ReintegroRegistroService utiliza
transacciones cortas READ_COMMITTED, bloqueo de compra y el turno temporal existente
para serializar sus devoluciones. Persiste el reintegro antes de enviarlo y reserva
saldo incluyendo resultados desconocidos. Cada devolución mantiene una clave única
estable; reutilizarla con otros datos u otra devolución produce conflicto.

Devolucion agrega etapa (REINTEGRO, ANULACION, LIBERACION, FINALIZADA) y origen por
cancelación; Reintegro agrega referenciaExterna. Los efectos confirmados actualizan
Pago.importeReintegrado y Compra.DEVUELTA_PARCIAL/TOTAL, mientras la solicitud solo
queda COMPLETADA al confirmar toda la coordinación aplicable. Las solicitudes de
6.1–6.4 con etapa null se inicializan en REINTEGRO al procesarlas.

La pasarela simulada mantiene múltiples operaciones por pago, un vínculo de clave
con su pago y tope agregado, incluyendo el diario de compensaciones anterior. Entradas
anula por IDs seleccionados y Reservas registra lugares liberados por entrada;
consultar antes de reintentar evita repetir efectos de respuestas perdidas. Cada
simulador utiliza el diario persistente existente, independiente del dominio.
ERROR_PENDIENTE conserva etapa y resultados, sin revertir efectos externos. Un rechazo
financiero definitivo conserva el rechazo y exige resolución manual, como las
compensaciones técnicas existentes. No se agrega infraestructura ni recuperación automática.

La cancelación es una notificación administrativa autoritativa simulada: marca el
evento simulado cancelado, reutiliza solicitudes vigentes, aprueba pendientes y crea
solo lo restante, conservando los rechazos como historial. Omite las 72 horas y no
reofrece lugares. Una devolución voluntaria tampoco reofrece lugares si el evento
ya comenzó o dejó de estar disponible. Cada compra se procesa independientemente.
Las compras sin pago reutilizan cancelarCompra; un pago PROCESANDO se reporta pendiente
hasta recuperar su resultado mediante la API existente y repetir la notificación.
200 informa finalización y 202 contiene comprasPendientes para recuperación controlada.

Las rutas /admin separan operaciones administrativas, pero no implementan seguridad.
Antes de integrar deben autorizarse esas rutas y comprobar identidad/titularidad en
las del cliente. Eventos deberá garantizar la entrega del aviso ante ventas concurrentes,
Entradas acordar anulación selectiva y Reservas liberación parcial idempotente condicionada
al estado comercial. Los ejemplos y contratos están en docs/flujo-compra.md.

### 9. Manejo de errores y recuperación

**Decisión:** conservar el resultado conocido de cada operación y registrar estados intermedios cuando una respuesta externa sea incierta.

Los errores se clasificarán en:

- **Validación:** datos incorrectos o reglas comerciales incumplidas.
- **Conflicto:** estado incompatible, reserva vencida o solicitud duplicada incompatible.
- **Falla externa conocida:** un servicio responde explícitamente que la operación fue rechazada.
- **Resultado desconocido:** se pierde la comunicación sin poder determinar si la operación se ejecutó.

Ante resultados desconocidos no se asumirán éxitos ni fracasos.

Se prevé un mecanismo de recuperación que consulte el estado de la operación externa y continúe el proceso sin repetir acciones ya confirmadas.

Para la primera entrega, este mecanismo podrá ejecutarse mediante operaciones de recuperación controladas y pruebas de integración. Su automatización completa dependerá de los contratos y del alcance acordado por el grupo.

**Alternativa considerada:** repetir inmediatamente todas las solicitudes fallidas. Se descarta porque podría provocar cobros, emisiones o reintegros duplicados.

### 10. Pruebas y documentación

**Decisión:** implementar pruebas unitarias con JUnit y Mockito, complementadas con pruebas de integración y escenarios de concurrencia.

Se priorizarán pruebas sobre:

- Validación y creación de compras.
- Cálculo de importes.
- Límites de entradas por cliente.
- Pagos aprobados, rechazados y duplicados.
- Solicitudes simultáneas de pago.
- Reservas vencidas y errores de comunicación.
- Confirmación de reservas y emisión de entradas.
- Devoluciones dentro y fuera del plazo.
- Reintegros parciales y totales.
- Cancelaciones de eventos.
- Recuperación de operaciones interrumpidas.

Se documentarán los endpoints mediante OpenAPI/Swagger y se prepararán ejemplos de solicitudes para probarlos desde Postman.

**Alternativa considerada:** validar únicamente mediante pruebas manuales. Se descarta porque no permite comprobar de forma repetible la lógica de negocio ni los escenarios de concurrencia.

## Risks / Trade-offs

- **[Diferencias entre los contratos REST de los microservicios]** → Acordar previamente endpoints, DTO, estados y códigos de respuesta con el grupo.
- **[Pago aprobado con reserva no confirmada]** → Conservar el estado de la operación y ejecutar un reintegro idempotente cuando se confirme que la compra no puede completarse.
- **[Emisión de entradas con resultado desconocido]** → Consultar el resultado antes de repetir la solicitud de emisión.
- **[Solicitudes simultáneas de pago]** → Aplicar restricciones de unicidad y control de concurrencia.
- **[Superación del límite de entradas por cliente]** → Validar el límite mediante una operación consistente frente a solicitudes concurrentes.
- **[Reintegros duplicados]** → Persistir identificadores únicos y verificar los importes previamente reintegrados.
- **[Fallas durante una devolución]** → Mantener estados intermedios y permitir la recuperación sin duplicar anulaciones o liberaciones.
- **[Dependencia de otros microservicios para probar Compras]** → Utilizar respuestas simuladas durante las pruebas unitarias y realizar pruebas integradas cuando los servicios estén disponibles.
- **[Mayor complejidad por las operaciones distribuidas]** → Mantener la coordinación en una capa de servicios específica, con estados y responsabilidades explícitas.

## Migration Plan

Antes de iniciar sobre datos existentes se deben revisar los cambios de esquema y los estados históricos. La migración manual `db/migrations/V001__agregar_medio_pago.sql` agrega medio_pago VARCHAR(32) NULL solo cuando la columna no existe en la base seleccionada, sin backfill ni modificación de pagos. No se ejecuta automáticamente ni se ejecutó durante su preparación. El usuario informó que desarrollo ya fue actualizado manualmente con respaldo e históricos conservados. Procedimiento, verificación y consideración del tipo ENUM que puede preferir Hibernate: docs/modelo-entidades.md, sección Migración manual de medio_pago. No se cambia el mapeo ni la configuración.

La incorporación al proyecto se realizará progresivamente:

1. Crear el proyecto Spring Boot con las dependencias necesarias.
2. Configurar la conexión a su base de datos MySQL independiente.
3. Implementar el modelo de persistencia y los repositorios.
4. Implementar las reglas de negocio de compras, pagos y devoluciones.
5. Incorporar los clientes REST para Eventos, Reservas y Entradas.
6. Integrar los mecanismos de idempotencia y recuperación.
7. Ejecutar las pruebas unitarias y de integración.
8. Documentar la API con OpenAPI/Swagger.
9. Incorporar el microservicio al entorno de ejecución del grupo y al API Gateway.

Ante una falla durante la integración, el microservicio podrá detenerse o retirarse del Gateway sin modificar las bases de datos de los demás servicios. Las operaciones comerciales ya registradas deberán conservarse para su posterior resolución.

## Open Questions

### Implementación verificada hasta 5.8

El circuito de esta entrega conserva clientes simulados. La coordinación persiste
`etapa` (`PAGO`, `CONFIRMACION`, `EMISION`, `COMPENSACION`, `LIBERACION`, `FINALIZADA`),
identificadores de entradas y un turno temporal por compra. Las llamadas externas
ocurren fuera de transacciones locales; cada avance se guarda en una transacción corta.

La reserva incluye la selección completa y se asocia a una única compra. El rechazo
del pago libera la reserva y termina la compra como `FALLIDA`; repetir la misma clave
recupera ese rechazo. Para intentar una operación comercial nueva se requiere otra
reserva y otra compra. Los pendientes vencidos dejan de consumir cupo al evaluar
una nueva compra para el mismo cliente/evento.

Un error de comunicación conserva el estado intermedio. Solo un rechazo externo
definitivo inicia compensación; esta verifica anulación, reintegro total y liberación.
El reintegro de compensación usa la entidad existente `Reintegro`, sin habilitar
casos de uso de devoluciones de la sección 6. `REINTEGRO_DESCONOCIDO` permite recuperar
la respuesta de esa compensación. Los simuladores conservan resultados en un diario
auxiliar independiente de las transacciones del dominio, para modelar reinicios.

La recuperación es controlada repitiendo `POST /pagos` con la misma clave. Devuelve
201 solo al crear el pago y 200 en repeticiones/recuperación. El turno vence a los
dos minutos y se renueva en cada avance; una solicitud simultánea recibe 409.
No se implementan scheduler, clientes REST reales ni Gateway en este alcance.

Los diagramas y contratos concretos están en `docs/flujo-compra.md`;
el modelo en `docs/modelo-entidades.md` y el aislamiento de pruebas
en `docs/pruebas.md`. Esta subsección conserva el corte histórico hasta 5.8; el estado actual incluye toda la sección 6 y mantiene pendiente la integración de la sección 7.

### Preguntas para la integración futura

Las observaciones de cierre se resuelven manteniendo los mismos componentes:
el catálogo simulado distingue publicado/no publicado/cancelado/finalizado y rechaza
IDs desconocidos; los sectores se buscan por Long sin conversión a int. El advice
uniforma también 406 y errores internos (500 sin detalles sensibles).

La cancelación registra CANCELADA/LIBERACION bajo bloqueo de compra antes de llamar
a Reservas fuera de transacción; la repetición recupera la liberación y solo termina
en FINALIZADA cuando se confirma LIBERADA. Compite con el pago usando el mismo bloqueo.

El rechazo definitivo de un reintegro técnico se registra como Reintegro RECHAZADO,
con fecha, manteniendo el pago cobrado y la compra en COMPENSACION. Devuelve 409
solicitando resolución manual y no reenvía ese reintegro. Esto se diferencia de un
resultado desconocido, que sigue recuperándose por consulta. No se automatiza la
resolución de rechazos financieros definitivos ni se habilitan casos de uso de sección 6.

Las siguientes cuestiones operativas deberán confirmarse con el grupo antes de cerrar los contratos de integración:

- Direcciones y rutas definitivas de los endpoints internos de Eventos, Reservas y Entradas.
- Formato común de identificadores, fechas, importes y errores HTTP.
- Tiempo de vigencia de las reservas temporales.
- Valor configurable del límite de entradas por cliente y evento.
- Mecanismo de acceso administrativo que se utilizará durante la primera entrega.
- Forma en que los servicios externos permitirán consultar el resultado de una operación cuyo procesamiento haya quedado incierto.
