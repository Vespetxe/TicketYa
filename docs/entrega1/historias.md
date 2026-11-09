# Features e historias de Entrega 1

Convención: cada historia define un escenario principal, aceptación verificable y alternativas/errores. Cliente, organizador y administrador son actores de negocio; no implican autenticación implementada. Los IDs declarados y el encabezado administrativo se explican en [limitaciones](limitaciones.md).

## Grupo E1.1. Catálogo de lugares y participantes — Eventos

### HU01. Registrar un lugar
Como organizador quiero registrar un lugar para ubicar mis eventos.

Principal: enviar nombre, dirección, ciudad opcional y coordenadas; recibir su `venueId`.

- Aceptación: POST devuelve 201 e identidad; GET recupera el lugar con sus datos.
- Alternativas: listar lugares existentes; conservar la identidad devuelta para crear sectores.
- Errores: campos obligatorios inválidos producen 400; identidad inexistente produce 404. Una ciudad omitida no permite satisfacer luego un filtro por esa ciudad.

### HU02. Definir sectores y asientos
Como organizador quiero dividir un lugar en sectores numerados o generales.

Principal: crear un sector `NUMERADO` con filas y asientos por fila, o `NO_NUMERADO` con capacidad.

- Aceptación: se asigna `sectorId`, asociado al lugar; en numerados se generan asientos consultables con `seatId`, fila y número.
- Alternativas: un lugar admite varios sectores. Los asientos del catálogo se trasladan al inicializar inventario, conservando su referencia externa.
- Errores: lugar inexistente 404, geometría/capacidad inválida 400 o regla de negocio 422 según el contrato.

### HU03. Registrar participantes
Como organizador quiero registrar artistas o participantes para asociarlos a un evento.

- Principal y aceptación: POST de nombre/rol devuelve 201; GET permite recuperar/listar el participante y su ID.
- Alternativas: reutilizar un participante en varios eventos.
- Errores: JSON no convertible 400, participante inexistente 404. El DTO actual no tiene restricciones de campos; la entidad valida al persistir y ese fallo puede producir 500. No se promete 400 para un nombre vacío. Se puede actualizar o eliminar un participante mediante PUT/DELETE; eliminar uno referenciado puede fallar por integridad.

## Grupo E1.2. Administración de eventos — Eventos e Inventario

### HU04. Crear borrador y precios
Como organizador quiero configurar un evento antes de habilitar ventas.

- Principal: crear nombre, descripción, tipo, organizador, lugar, fecha futura y participantes; asignar precio a cada sector del lugar.
- Aceptación: estado inicial `BORRADOR`; cada precio corresponde al lugar del evento y es positivo; consultar el evento devuelve sus precios.
- Alternativas: completar precios antes de publicar.
- Errores: lugar inexistente 404; validación de fecha futura en DTO 400; participante inexistente, sector ajeno o regla inválida 422. No se acepta un precio proporcionado por el comprador como precio de venta.

### HU05. Publicar con inventario
Como organizador quiero publicar el evento con disponibilidad consistente.

- Principal: PATCH publicar inicializa por REST todos los sectores como `PREPARADO`, sin admitir reservas; después confirma `PUBLICADO` y habilita comercialmente el inventario en una transacción local.
- Aceptación: fecha futura, configuración válida y todos los sectores con precio; disponibilidad inicial coincide con la capacidad y los asientos del catálogo.
- Alternativas: si falla la preparación, devuelve 503 y conserva borrador/sectores preparados no reservables. Si falla la habilitación posterior al commit de PUBLICADO, conserva publicación con habilitación pendiente; repetir publicar habilita sin reinicializar ni duplicar stock. Suspensión/cancelación prevalecen frente a una activación tardía.
- Errores: inicialización incompatible rechazada por Inventario (400/409) se traduce a 422 en Eventos; requiere revisar la configuración y no reintentar ciegamente. Una transición inválida es 422; indisponibilidad temporal es 503. No se promete recuperación de configuraciones distintas concurrentes.

### HU06. Administrar estados
Como administrador quiero suspender ventas y cerrar estados del evento.

- Principal: con `X-User-Role: ADMIN` y `X-Admin-Id` positivo, suspender bloquea inventario y cambia a `SUSPENDIDO`.
- Aceptación: `AGOTADO` y `FINALIZADO` se asignan mediante PATCH status y bloquean nuevas reservas; reabrir `AGOTADO` como `PUBLICADO` exige cupo positivo.
- Alternativas: la cancelación integral corresponde a HU19; suspensión también se admite desde borrador.
- Errores: rol administrativo inválido y transición inválida 422; encabezado obligatorio omitido puede producir 400; dependencia pendiente 503. No hay reanudación pública de suspendidos ni detección automática de agotado.

## Grupo E1.3. Descubrimiento — Eventos

### HU07. Buscar eventos
Como cliente quiero filtrar eventos para elegir una función.

- Principal: buscar por ciudad, rango de fechas, tipo, organizador o participante.
- Aceptación: los filtros se combinan; ciudad no distingue mayúsculas; se muestran publicados/agotados; cada resultado expone precio mínimo, identidad del lugar, tipo, fecha y estado.
- Alternativas: búsqueda sin filtros y consulta de detalle por ID.
- Errores: rangos incoherentes 422; parámetros no convertibles 400. Un resultado vacío es una búsqueda válida.

### HU08. Ordenar resultados
Como cliente quiero un orden reproducible y relevante.

- Principal: elegir relevancia, fecha o precio y dirección según OpenAPI.
- Aceptación: relevancia prioriza `PUBLICADO` frente a `AGOTADO`, luego fecha próxima e ID; precio usa el mínimo por evento con precisión decimal; el resultado no duplica eventos por sus participantes.
- Alternativas: empates resueltos por identidad; no se ofrece un motor de relevancia textual.
- Errores: opciones inválidas se resuelven según validación del endpoint, sin inventar órdenes adicionales.

## Grupo E1.4. Disponibilidad y compras — Inventario y Compras

### HU09. Reservar disponibilidad temporal
Como cliente quiero retener cupos o asientos mientras pago.

- Principal: consultar disponibilidad, seleccionar `idAsiento` operativo y crear reserva.
- Aceptación: cantidad positiva, selección exacta y sin duplicados; reserva `PENDIENTE` con vencimiento (el adaptador de Compras la traduce como `TEMPORAL`); TTL normal 600 s. Una reserva impide vender simultáneamente esos lugares.
- Alternativas: sector general usa cantidad sin asientos; GET reserva e historial por `idCliente`.
- Errores: inventario inexistente 404, selección inválida 400, cupo/asiento ocupado o evento bloqueado 409. No intercambiar `seatId` del catálogo por `idAsiento` del inventario.

### HU10. Crear compra
Como cliente quiero convertir mi reserva en una orden con importe verificado.

- Principal: enviar cliente, evento, reserva y detalles; recibir compra `PENDIENTE` con precios históricos e importe calculado por servidor.
- Aceptación: evento publicado futuro; reserva temporal vigente del mismo cliente/evento; detalles coinciden con la selección; una compra por reserva; límite predeterminado de seis entradas por cliente/evento.
- Alternativas: varias líneas numeradas de cantidad 1 o una línea general de varias unidades.
- Errores: reserva incompatible, reutilizada, vencida o límite excedido impiden crear la compra; no se factura el precio enviado por el usuario.

### HU11. Pagar y completar
Como cliente quiero pagar una compra y obtener entradas.

- Principal: POST pago con `compraId`, `claveIdempotencia` estable y medio admitido. Se registra pago, confirma reserva y emite antes de marcar `COMPLETADA`.
- Aceptación: importes hasta 100000 son aprobados por la pasarela simulada; superiores se rechazan. Aprobación exitosa produce tickets y reserva `CONFIRMADA`; rechazo libera reserva, no emite y termina compra `FALLIDA`.
- Alternativas: repetir la misma clave conserva el efecto; no equivale a crear otro cobro. Consultar compra/pagos permite comprobar el resultado.
- Errores: clave incompatible, turno ocupado o estado no habilitado produce conflicto; dependencia incierta conserva recuperación. Ver HU13.

### HU12. Vencer una reserva
Como plataforma quiero liberar retenciones impagas vencidas.

- Principal: scheduler detecta vencimiento y libera los lugares de una reserva temporal.
- Aceptación: cambia a `EXPIRADA`; no se confirma una reserva expirada ni se emiten tickets por esa confirmación fallida.
- Alternativas: un pago ya aprobado cuando falla la confirmación se reintegra como compensación y la compra queda fallida.
- Errores: concurrencia con confirmación se serializa mediante bloqueos en BD; no confiar solamente en el estado visto por el navegador.

### HU13. Recuperar pagos y compensaciones
Como plataforma quiero completar o compensar reintentos sin duplicar efectos.

- Principal: conservar etapas `PAGO`, `CONFIRMACION`, `EMISION`, `COMPENSACION`, `LIBERACION`, `FINALIZADA` y claves del pago/reintegro; repetir la solicitud original.
- Aceptación: un timeout posterior al efecto se consulta en el registro de la pasarela; no se cobra ni reintegra dos veces; una falla definitiva anula el lote, reintegra cobro confirmado y compensa reserva.
- Alternativas: clave que comienza con `simular-timeout` demuestra incertidumbre posterior al efecto. Turno persistido dura dos minutos; si está ocupado se reintenta después.
- Errores: resultado desconocido permanece recuperable, sin afirmar que no hubo cobro; no existe worker automático de recuperación.

Una compra todavía pendiente también puede cancelarse mediante POST `/compras/{id}/cancelar`: registra la intención, impide nuevos pagos y permite repetir para completar la liberación de reserva. No es una devolución de una venta completada. Comprar/pagar o cancelar en un estado incompatible produce el error indicado en el contrato.

### HU14. Evitar sobreventa concurrente
Como organizador quiero preservar capacidad ante solicitudes simultáneas.

La prueba `diezSolicitudesDosInstanciasUltimoCupo` envía diez reservas por un único cupo y comprueba la compra del ganador. [Ejecución aislada](pruebas.md).

- Principal: varias solicitudes compiten por el mismo lugar o límite de cliente.
- Aceptación: los bloqueos de inventario y control de límite en BD permiten una sola ocupación válida y no exceden el límite; dos instancias de Inventario comparten esa protección.
- Alternativas: perdedores reciben conflicto y pueden volver a consultar disponibilidad.
- Errores: un reintento de liberación/reintegro no incrementa nuevamente el stock; la identidad del ticket devuelto se registra una sola vez.

## Grupo E1.5. Entradas — Entradas y Compras

### HU15. Emitir y consultar entradas
Como cliente quiero recibir una entrada por unidad comprada y consultarlas.

- Principal: Compras solicita emisión interna; Entradas consulta la orden confirmada y persiste un lote único por orden.
- Aceptación: cada unidad tiene `ticketId` UUID y `qr` UUID únicos; estado inicial `EMITIDA`; GET `/tickets` acepta exactamente uno de `orderId` o `clientId`.
- Alternativas: reintento de emisión devuelve el mismo lote con `created=false`; una línea numerada exige cantidad 1.
- Errores: orden inexistente 404, no habilitada/cancelada 409, detalle incompatible 422, Compras inaccesible 503. Anulación previa de orden o barrera de evento impide emisión posterior.
- Límite: QR es un identificador opaco; no hay validación de ingreso en Entrega 1.

## Grupo E1.6. Devoluciones y cancelación — Compras, Entradas e Inventario

### HU16. Solicitar devolución parcial o total
Como cliente quiero devolver una selección de mis entradas.

- Principal: enviar `compraId`, `clienteId`, IDs numéricos locales de entradas y motivo.
- Aceptación: entrada vigente propia, compra completada y al menos 72 horas hasta el evento; igualdad exacta admitida; no hay otra solicitud activa/completada para la misma entrada.
- Alternativas: seleccionar una parte de una compra es devolución parcial; cada unidad seleccionada recupera su precio histórico completo.
- Errores: ajenidad, entrada anulada, duplicados o plazo insuficiente rechazan la solicitud. GET `/compras/{id}/entradas` asocia por unidad `entradaId` numérico, `ticketId` UUID, sector, asiento y estado. El cliente elige el asiento en esa respuesta y envía su `entradaId`; no usa el orden de listas para inferir correspondencias.

### HU17. Decidir una devolución
Como administrador quiero revisar y aprobar o rechazar solicitudes.

- Principal: identificarse con `X-Admin-Id` positivo, listar solicitudes con el parámetro obligatorio `estado` y aprobar con motivo; estado `APROBADA`.
- Aceptación: aprobar no reintegra ni libera inventario; rechazar registra decisión y motivo; consultar preserva la trazabilidad.
- Alternativas: filtrar solicitudes según contrato.
- Errores: transiciones incompatibles o identidad inexistente se rechazan; aprobación y procesamiento son acciones separadas.

### HU18. Procesar reintegro
Como administrador quiero ejecutar una devolución aprobada sin duplicar dinero ni stock.

- Principal: procesar con clave estable e importe exactamente igual a la suma de entradas elegidas; reintegrar, anular esas entradas y liberar esos lugares.
- Aceptación: la consulta de procesamiento devuelve etapas y reintegro; repetición con la misma identidad no reintegra ni libera dos veces; entradas no elegidas siguen vigentes.
- Alternativas: evento cancelado no reofrece stock; evento pasado tampoco; agotado/suspendido puede recuperar lugares sin habilitar automáticamente ventas.
- Errores: importe incorrecto, clave incompatible o estado inválido se rechazan; fallo externo conserva progreso para reintentar.

### HU19. Cancelar integralmente un evento
Como administrador quiero cerrar ventas, emisión y reintegrar compras del evento.

- Principal: PATCH cancelar con encabezados ADMIN e identidad administrativa positiva registra primero barrera durable de emisión en Entradas, luego `CANCELADO`, bloquea Inventario y notifica Compras.
- Aceptación: ninguna emisión nueva puede quedar confirmada después de la barrera; se anulan entradas y se reintegran ventas; compras en procesamiento conservan intención de cancelación y recuperan pagos inciertos con su clave original.
- Alternativas: evento sin tickets también registra barrera; repetir completa los pendientes; Compras devuelve 202 mientras hay trabajo pendiente y Eventos comunica 503 recuperable.
- Errores: evento finalizado no admite cancelar; dependencia caída exige repetir, sin afirmar rollback distribuido. No se vuelven a ofrecer lugares cancelados.

## Grupo E1.7. Integración y entrega

### HU20. Usar y reproducir el Gateway
Como docente quiero ejecutar los cinco proyectos y demostrar los circuitos desde una entrada pública.

- Principal: crear esquemas nuevos, arrancar con perfil `integracion` y ejecutar la colección a través de 8080.
- Aceptación: rutas públicas se reenvían a su propietario; operaciones `/internal/**` no se publican; la colección captura IDs reales y genera fechas futuras; documentación describe dependencias reales y pago simulado.
- Alternativas: Swagger por servicio o importación de OpenAPI estático; pruebas en esquemas/puertos acreditados y separados.
- Errores: proveedor inaccesible 503, ruta no publicada 404; no reutilizar credenciales o BD de desarrollo para una prueba integral.

## Funcionalidades previstas para Entrega 2

Las siguientes features corresponden a los números de FUNCIONALIDAD del PDF. Se documentan ahora porque la página 4 exige historias de todo el sistema. Sus criterios son iniciales, sujetos a refinamiento para Entrega 2; **ninguna de estas prestaciones se implementa ni se declara disponible en esta entrega**. Los grupos E1 anteriores son agrupaciones del documento, no números de funcionalidades del PDF.

### F6. Fila virtual — HU21
Como cliente de un evento de alta demanda quiero conocer mi posición y recibir un turno para comprar, respetando el orden de ingreso.

Actor principal: cliente. Actor operativo: organizador que marca el evento de alta demanda.

- Principal previsto: al abrir ventas se registra el ingreso a la fila; se informa posición y se habilitan tandas en orden.
- Aceptación inicial: un cliente pendiente de turno no puede iniciar una compra de ese evento; un turno habilitado permite hacerlo dentro del plazo que se defina; refrescar/reintentar no crea varias posiciones para el mismo cliente/evento.
- Alternativas: fuera de alta demanda se mantiene el circuito normal; un turno vencido libera su lugar en la tanda según una política explícita.
- Excepciones: desconexión no debe perder una posición todavía vigente; saturación y suspensión/cancelación deben comunicar un estado sin habilitar ventas indebidas. Duración de turno y tamaño de tanda se definirán en E2.

### F8. Control de acceso QR — HU22
Como personal de acceso quiero escanear una entrada para admitir una sola vez a su titular en el evento correcto.

- Principal previsto: escanear QR, comprobar existencia, evento y vigencia, y registrar el uso.
- Aceptación inicial: un QR válido para ese evento admite una entrada y queda utilizado; un segundo escaneo se rechaza; dos escaneos concurrentes solo producen una admisión.
- Alternativas: QR inexistente, de otro evento, anulado o reintegrado se rechaza con motivo verificable.
- Excepciones: ante indisponibilidad no se confirma un acceso inexistente; se definirá la política de recuperación en E2. Entrega 1 solo emite el valor QR opaco.

### F10. Preferencias y recomendaciones — HU23
Como cliente quiero registrar tipos de evento, artistas favoritos y ciudad para recibir sugerencias acordes con mis intereses e historial.

- Principal previsto: guardar preferencias, consultar sugerencias y usar compras anteriores para relacionar eventos.
- Aceptación inicial: consultar preferencias recupera la selección guardada; cambiarla modifica los criterios usados; las sugerencias identifican eventos y se basan en preferencias o historial disponible.
- Alternativas: un cliente sin historial recibe una alternativa de catálogo explícita; no se muestran cancelados como comprables.
- Excepciones: no se atribuye historial de otro cliente; referencias a artistas o ciudades inválidas se informan. Algoritmo y persistencia NoSQL se definirán para E2, sin presentarlos como componentes actuales.

### F11. Notificaciones — HU24
Como cliente quiero recibir avisos de compra confirmada, cancelación/cambio de evento y publicación de un artista favorito en mi ciudad.

Actor emisor: plataforma; destinatario: cliente; proveedor externo: correo o push por definir.

- Principal previsto: un hecho de negocio confirmado genera una notificación asociada al comprador/destinatario correcto.
- Aceptación inicial: compra completada produce confirmación; cancelación/cambio identifica el evento afectado; favorito más ciudad determina los destinatarios de una nueva publicación.
- Alternativas: reintentos conservan la identidad del aviso para evitar duplicados; se distingue entrega pendiente de entrega confirmada.
- Excepciones: caída del proveedor conserva el trabajo pendiente sin revertir una compra o reintegro confirmado. Canal, política de reintentos y consentimiento se refinan en E2; hoy no se envían avisos.

### F12. Reportes de organizadores — HU25
Como organizador quiero consultar ventas, devoluciones, ocupación y recaudación de mis eventos por sector.

- Principal previsto: seleccionar evento propio y obtener cantidades vendidas/devueltas, capacidad y porcentaje de ocupación, importes cobrados y reintegrados.
- Aceptación inicial: totales por sector concilian con sus operaciones y el total del evento; el porcentaje indica su denominador; se distingue importe cobrado, reintegrado y neto; un reintento financiero no duplica ingresos en el reporte.
- Alternativas: evento sin ventas devuelve ceros y capacidad conocida; devoluciones parciales afectan solo las unidades seleccionadas.
- Excepciones: no se consulta un evento ajeno como propio; un dato todavía no consolidado se identifica expresamente. Corte temporal y reglas de acceso se definirán en E2. No hay endpoints de reportes en E1.
