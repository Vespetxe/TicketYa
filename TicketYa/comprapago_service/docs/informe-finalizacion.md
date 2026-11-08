# Informe de finalización — Compras y Pagos hasta 5.8

> Corte histórico del 2026-11-05, limitado a secciones 1–5. No describe el estado
> actual: las secciones 1–6 están implementadas con simuladores; la integración de
> sección 7 permanece pendiente. Consultar flujo-compra.md y tasks.md para el estado vigente.

Fecha: 2026-11-05. Alcance: secciones 1 a 5 de openspec/changes/compras-pagos.

## Resultado

Las 32 tareas del alcance están implementadas y verificadas. El cambio completo
conserva 18 tareas pendientes de las secciones 6 y 7. No se realizaron commits ni push.
No se inició la aplicación contra el datasource de desarrollo ni se ejecutaron
operaciones sobre esa base.

## Tareas completadas y evidencia

| Tareas | Resultado y evidencia principal |
|---|---|
| 1.1–1.2 | Compilación Maven con JDK 21 y resolución de dependencias existentes; verify y JAR ejecutable correctos. |
| 1.3 | Paquetes completos, incluido config con reloj UTC. |
| 1.4 | Arranque de contextos Spring y persistencia real sobre MySQL 8.4 aislado. |
| 1.5 | ErrorResponse uniforme; tests HTTP de validación, formato, inexistencia, conflicto, métodos, contenido, 406 y 500 sin detalles internos. |
| 2.1–2.4 | Entidades y repositorios conservados; pruebas JPA de relaciones y unicidad de pagos/reintegros; reserva única y trazabilidad de entradas. |
| 2.5 | Tests existentes de transiciones y nuevos tests de estados persistidos del circuito. |
| 2.6 | Modelo ER Mermaid actualizado, nueve tablas, restricciones y notas sobre datos históricos. |
| 3.1–3.3 | DTO, selección, precios internos, escala monetaria y HTTP sin precio del cliente; catálogo simulado distingue estados comerciales y rechaza sectores Long fuera de catálogo. |
| 3.4 | Límite configurable, serialización por cliente/evento y concurrencia con MySQL; pendientes vencidas dejan de consumir cupo. |
| 3.5–3.6 | Consultas, detalle/estado, DTO sin ciclos JPA, HTTP real y verificación de Swagger/OpenAPI. |
| 4.1–4.3 | Pasarela simulada, condiciones previas, resultados aprobados/rechazados/desconocidos y registro durable. |
| 4.4–4.5 | Unicidad de clave antes de cobrar, turno por compra y tests de concurrencia, incluso clave compartida entre compras. |
| 4.6–4.7 | Consulta/recuperación por clave original y endpoints documentados; recuperación devuelve el pago existente. |
| 5.1–5.3 | Clientes simulados conservados y ampliados con selección completa y diario durable. |
| 5.4–5.6 | Confirmación, emisión, IDs y COMPLETADA; cancelación pendiente con liberación recuperable y exclusión frente al pago simultáneo. |
| 5.7 | Recuperación por etapa, respuestas perdidas y compensación persistida; rechazo definitivo de reintegro separado de incertidumbre, con 409 y resolución manual sin reenvío automático. |
| 5.8 | Diagramas de éxito, rechazo, vencimiento, incertidumbre y compensación alineados con el código. |

## Verificaciones ejecutadas

- Maven -o clean compile: BUILD SUCCESS.
- Maven -o verify: BUILD SUCCESS, 192 tests, 0 failures, 0 errors, 0 skipped.
- El empaquetado generó target/compras-service-0.0.1-SNAPSHOT.jar.
- OpenSpec validate compras-pagos --strict --json: válido, sin incidencias.
- git diff --check: sin errores de espacios en los cambios finales.

Se utilizó Maven 3.10.0 de la caché local y JDK Microsoft 21. La batería final terminó
el 2026-11-05 a las 21:45:13 de America/Buenos_Aires. Los reportes detallados están en
target/surefire-reports y la salida final en target/verification-cierre.log, ambos ignorados por Git.

La base fue ticketya_compras_test en un contenedor mysql:8.4 sin volúmenes, publicado
solo en 127.0.0.1:33079. Un inicializador de tests bloquea URLs distintas antes de Hibernate.
La suite incluye una prueba de ese bloqueo. No utiliza DB_PASSWORD ni el puerto 3306.

Se conservaron los escenarios anteriores. Los tests se adaptaron al contrato DTO,
a UTC y a la política de rechazo terminal con liberación. Se añadieron 53 pruebas
respecto de las 139 iniciales: 14 corresponden al cierre de la segunda auditoría.
Los fallos encontrados durante el desarrollo se corrigieron; no se omitieron tests.

## Decisiones arquitectónicas

1. CompraRegistroService y PagoRegistroService concentran transacciones locales cortas.
   PagoService coordina efectos externos fuera de ellas. Un rollback local no borra
   la evidencia de un cobro previamente registrado.
2. Compra conserva etapa, IDs de entradas y un turno de dos minutos con token.
   El bloqueo de fila protege el reclamo; no permanece abierto durante llamadas externas.
   Un token reemplazado no puede actualizar el progreso.
3. Las operaciones inciertas se consultan antes de repetir. Solo el rechazo definitivo
   permite compensar. La emisión se valida por cantidad, titularidad, evento y selección.
4. La compensación anula y verifica entradas, reintegra y verifica liberación antes
   de finalizar FALLIDA. La anulación crea una barrera durable incluso sin entradas,
   impidiendo emisiones demoradas posteriores al reintegro.
5. Se reutiliza Reintegro para compensación técnica total de una compra fallida.
   No se implementan solicitudes, aprobación administrativa, devoluciones parciales,
   regla de 72 horas ni cancelaciones de eventos.
6. El diario operaciones_simuladas modela resultados externos persistentes con
   transacciones independientes y claves estables. No es integración REST real.
7. Repetir una creación con la misma reserva produce conflicto. Repetir el pago usa
   la clave original. Un rechazo libera la reserva y termina la compra: otro intento
   comercial requiere reserva y compra nuevas.
8. Los controladores devuelven DTO y un único advice activo define los errores.
   PagoExceptionHandler queda como compatibilidad para tests standalone.

## Problemas corregidos

- Pérdida del registro local por rollback después de cobrar o compensar.
- Reintegro inmediato ante timeout sin verificar confirmación/emisión.
- Compra que permanecía PENDIENTE después de emitir entradas.
- Respuesta de emisión ignorada y ausencia de IDs persistidos.
- Reserva sin correspondencia de sector/cantidad/asiento.
- Compras repetidas sobre una misma reserva.
- Pago rechazado sin liberación recuperable.
- Recursión/acoplamiento de entidades JPA en la API.
- Resultados simulados perdidos al reconstruir clientes.
- Pruebas que podían heredar el datasource de desarrollo.
- Comparaciones temporales con zonas inconsistentes.
- Pendientes vencidas consumiendo el límite indefinidamente.
- Emisión demorada posible después de una anulación sin entradas previas.

## Límites restantes

- Secciones 6 y 7 fuera del alcance y pendientes en OpenSpec. No hay Gateway ni clientes REST reales.
- Recuperación controlada mediante repetición de POST /pagos con la misma clave;
  no existe recuperación programada. Tras una caída se espera el vencimiento del turno.
- Un reintegro definitivamente rechazado requiere intervención manual: queda RECHAZADO,
  el pago sigue cobrado, la compra incompleta y la API devuelve 409 sin reenviarlo.
  Esta política resuelve la clasificación errónea; no automatiza un nuevo intento financiero.
- Los futuros clientes REST deben preservar idempotencia, consultas fiables y
  distinción entre rechazo definitivo e incertidumbre. La suite no certifica esos servicios.
- Antes de arrancar sobre datos históricos se requiere revisar y reconciliar las
  inconsistencias de la versión anterior e inicializar etapas coherentes. No se ejecutó
  una migración ni se inspeccionaron/modificaron datos de desarrollo.
- Se conserva ddl-auto=update de desarrollo y no se introdujo un framework de migraciones.
- Autenticación/JWT y operación pública no se incorporaron a este alcance.
- No se detectaron fallos críticos pendientes en los escenarios implementados y probados;
  no se afirma una garantía absoluta frente a toda infraestructura externa futura.

## Archivos creados

- compras-service/src/test/java/ar/edu/unicen/grupo8/comprasservice/cliente/EventoClientSimuladoTest.java
- compras-service/docs/flujo-compra.md
- compras-service/docs/informe-finalizacion.md
- compras-service/docs/pruebas.md
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/client/AlmacenSimulado.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/client/AlmacenSimuladoJpa.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/config/AplicacionConfig.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/dto/PagoDTO.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/entity/OperacionSimulada.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/entity/enums/EtapaCompra.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/exception/OperacionExternaRechazadaException.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/service/CompraRegistroService.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/service/PagoRegistroService.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/service/SeleccionReserva.java
- compras-service/src/test/java/ar/edu/unicen/grupo8/comprasservice/EntornoPruebasSeguro.java
- compras-service/src/test/java/ar/edu/unicen/grupo8/comprasservice/EntornoPruebasSeguroTest.java
- compras-service/src/test/java/ar/edu/unicen/grupo8/comprasservice/controller/ApiIntegracionTest.java
- compras-service/src/test/java/ar/edu/unicen/grupo8/comprasservice/service/FlujoCompraIntegracionTest.java
- compras-service/src/test/java/ar/edu/unicen/grupo8/comprasservice/service/SeleccionReservaTest.java
- compras-service/src/test/resources/META-INF/spring.factories
- compras-service/src/test/resources/application.properties

## Archivos modificados

- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/client/EventoClientSimulado.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/entity/enums/EstadoPago.java
- compras-service/src/test/java/ar/edu/unicen/grupo8/comprasservice/entity/enums/EstadoPagoTest.java
- openspec/changes/compras-pagos/specs/coordinacion-reservas-entradas/spec.md

- compras-service/docs/modelo-entidades.md
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/client/EntradaClientSimulado.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/client/ReservaClient.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/client/ReservaClientSimulado.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/controller/CompraController.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/controller/PagoController.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/controller/PagoExceptionHandler.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/dto/CompraDTO.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/dto/CrearCompraDTO.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/dto/DetalleCompraDTO.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/dto/ProcesarPagoDTO.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/dto/ReservaDTO.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/entity/Compra.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/entity/Pago.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/exception/GlobalExceptionHandler.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/repository/CompraRepository.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/service/CompraService.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/service/PagoService.java
- compras-service/src/main/java/ar/edu/unicen/grupo8/comprasservice/service/PasarelaPagoSimuladaService.java
- compras-service/src/main/resources/application-test.properties
- compras-service/src/main/resources/application.properties
- compras-service/src/test/java/ar/edu/unicen/grupo8/comprasservice/cliente/EntradaClientSimuladoTest.java
- compras-service/src/test/java/ar/edu/unicen/grupo8/comprasservice/cliente/ReservaClientSimuladoTest.java
- compras-service/src/test/java/ar/edu/unicen/grupo8/comprasservice/controller/PagoControllerTest.java
- compras-service/src/test/java/ar/edu/unicen/grupo8/comprasservice/dto/CrearCompraDTOTest.java
- compras-service/src/test/java/ar/edu/unicen/grupo8/comprasservice/repository/ReintegroRepositoryTest.java
- compras-service/src/test/java/ar/edu/unicen/grupo8/comprasservice/service/CompraIntegracionTest.java
- compras-service/src/test/java/ar/edu/unicen/grupo8/comprasservice/service/CompraServiceTest.java
- compras-service/src/test/java/ar/edu/unicen/grupo8/comprasservice/service/PagoServiceTest.java
- compras-service/src/test/java/ar/edu/unicen/grupo8/comprasservice/service/PasarelaPagoSimuladaServiceTest.java
- openspec/changes/compras-pagos/design.md
- openspec/changes/compras-pagos/tasks.md
