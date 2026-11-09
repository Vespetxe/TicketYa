# Demostración reproducible por Gateway

Preparar primero un entorno con cuatro esquemas **nuevos** siguiendo el [README](../../README.md). La colección escribe datos; no apuntarla a un entorno de desarrollo existente.

Importar [colección Postman](postman/TicketYa-Entrega1.postman_collection.json) y [entorno local de ejemplo](postman/local.postman_environment.json), seleccionar el entorno y revisar `base_url` (predeterminado `http://localhost:8080`). `cliente_id`, `organizador_id` y `administrador_id` son actores declarados, no IDs de tablas de usuarios ni credenciales.

Ejecutar cada carpeta completa y en orden, preferentemente con Collection Runner sin demoras. Cada carpeta crea su propio lugar, participante, sector y evento; captura sus IDs en el entorno y genera un sufijo único y fecha a ocho días. `precio` es 15000 para aprobación y 150000 para rechazo. Los datos JSON de cada solicitud son los datos de ejemplo; no se necesita insertar registros con SQL ni conocer IDs previos.

1. **Compra numerada y devolución**: crear catálogo, precio y publicar; consultar catálogo/asientos y disponibilidad; reservar dos asientos operativos; crear compra; pagar y repetir pago; consultar tickets/compra/pagos; consultar GET `/compras/{id}/entradas`, elegir un asiento y solicitar su devolución, aprobar y procesar con su clave; repetir procesamiento y comprobar reintegro, anulación del UUID elegido, vigencia del otro y reposición solo del asiento elegido; suspender y cancelar el evento.
2. **Compra general y cancelación**: crear otro evento con sector no numerado; reservar una unidad, comprar y pagar; cancelar dos veces; verificar ticket anulado, reintegro total y disponibilidad bloqueada.
3. **Pago rechazado**: crear otro evento con precio 150000; reservar y comprar; pago rechazado y reintento con la misma clave; comprobar ausencia de tickets y stock recuperado.

La reserva dura 600 s en el entorno normal; ejecutar desde disponibilidad hasta pago antes de su vencimiento. Si vence durante una demostración manual, comenzar la carpeta de nuevo para generar datos independientes. No reutilizar una reserva ya asociada a una compra.

Los scripts generan `clave_pago` al crear la compra y `clave_devolucion` al crear la solicitud; los pasos de repetición reutilizan esos valores. No cambian claves al recibir una respuesta incierta. Los fallos se presentan como aserciones Postman; no hay limpieza ni cancelación automática ajena a las solicitudes visibles.

La primera carpeta compra dos asientos. GET `/compras/{id}/entradas` entrega por unidad el `entradaId` numérico aceptado por devolución y su `ticketId` UUID, sector, asiento y estado. El script selecciona el asiento capturado de Inventario, conserva ambas identidades y verifica después que ese UUID esté ANULADA y el otro siga EMITIDA. Las otras carpetas compran una unidad general. No se infiere correspondencia por orden de listas.

El asiento reservado se captura de `asientosLibres[].idAsiento` en Inventario. El catálogo tiene otro `seatId`, que puede consultarse para exhibir la grilla pero no debe enviarse como asiento operativo. Las búsquedas pueden incluir datos de carpetas previas; el test comprueba que aparezca el evento recién publicado.

El vencimiento temporal y las carreras concurrentes están representados en [secuencias](secuencias.md) y cubiertos por la [suite aislada](pruebas.md); no se añaden esperas largas o comandos internos a la colección pública. La colección se ejecutó contra la red desechable acreditada mediante el runner Node del repositorio: 77 solicitudes y 127 aserciones aprobadas. El runner implementa las API pm que utiliza esta colección; no se ejecutó el cliente Postman ni Newman. Ver la evidencia histórica consolidada en [pruebas.md](pruebas.md).

Las operaciones administrativas envían `X-Admin-Id` positivo y Eventos también `X-User-Role: ADMIN`. El paso 22 consulta `/admin/devoluciones?estado=PENDIENTE`; `estado` es obligatorio. El validador documental comprueba ahora parámetros de consulta y encabezados requeridos de todas las solicitudes.
