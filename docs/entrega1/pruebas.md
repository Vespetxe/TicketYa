# Pruebas de Entrega 1

Ejecutar desde la raíz de TicketYa con JDK 21, Maven y Node.js en PATH. Para la corrida integral en Windows se requiere PowerShell 7, Docker con contenedores Linux y la imagen `mysql:8.4`. Configurar `JAVA_HOME` y usar una terminal nueva, sin configuración de servicios o datasources de desarrollo.

## Controles locales

```powershell
mvn -f TicketYa/integration-tests/pom.xml test
```

Surefire ejecuta las pruebas de la guarda con transporte simulado. Comprueba las dos redes permitidas, el token, las bases, los once destinos REST y ambas instancias de Inventario. Un destino ausente o normal, una base incorrecta o un error en la última acreditación deben interrumpir el recorrido antes de cualquier operación de negocio.

El mismo comando valida rutas de los controladores, ejemplos, referencias, visibilidad y parámetros OpenAPI, variables, códigos HTTP, cuerpos y sintaxis JavaScript de Postman, enlaces Markdown y modelo relacional. Node verifica la sintaxis de los scripts. Las pruebas de los generadores escriben bajo `TicketYa/integration-tests/target/documentacion/` y comparan los archivos resultantes con los entregables. Una diferencia hace fallar la prueba y deja la copia generada para revisión.

- **Postman.** La definición de pasos reutilizables está en `src/main/resources/postman-entrega1.json` del módulo. El generador arma las tres carpetas en el orden definido, agrega cabeceras, scripts y códigos, y construye el entorno. La colección y el entorno importables siguen en [Postman](postman/).
- **Mermaid.** Las fuentes de arquitectura y secuencias están en `diagramas/*.mmd`. Los cuatro ER y las restricciones compuestas se derivan del [snapshot del esquema](modelos/esquema-referencia.json). Se vuelven a insertar los diagramas en las páginas Markdown y se compara el texto completo.
- **OpenAPI.** Los contratos por servicio conservan las correcciones documentales revisadas de Entrega 1. El generador aplica esas correcciones a copias, determina la visibilidad, prefija las referencias por servicio y reconstruye Gateway y el índice. Para contrastar exportaciones de Eventos, Inventario y Compras, usar `-Dticketya.openapi=<directorio>` con archivos llamados `eventos.json`, `inventario.json` y `compras.json`. Entradas usa la copia estática del servicio. [Inventario de endpoints](openapi/README.md).

## Corrida integral

```powershell
pwsh -NoProfile -File TicketYa/integration-tests/iniciar_entorno_integral.ps1
```

El lanzador admite `-MavenCommand` con la ruta de Maven. Con dependencias ya descargadas se puede usar `-Offline`. La opción `-BuildOnly` reconstruye los cinco JAR sin iniciar servicios. Para ejecutar únicamente las suites Java de los cinco componentes:

```powershell
pwsh -NoProfile -File TicketYa/integration-tests/probar_servicios.ps1
```

| Etapa | Recursos y controles |
|---|---|
| Suites por componente | Eventos, Inventario y Entradas usan sus H2 de prueba en memoria. Compras usa un contenedor nuevo, sin volumen de desarrollo, vinculado a `127.0.0.1:33079/ticketya_compras_test`. El lanzador comprueba la base dentro del contenedor y conserva las guardas previas a Hibernate. Gateway usa sus proveedores de prueba. |
| Preparación HTTP | Segundo MySQL desechable en `127.0.0.1:33489`, con contraseña aleatoria y cuatro esquemas nuevos `ticketya_e1_eventos`, `ticketya_e1_inventario`, `ticketya_e1_compras` y `ticketya_e1_entradas`. Un puerto ocupado interrumpe el arranque. |
| Procesos | Se reconstruyen los JAR y se lanzan Gateway, Eventos, Inventario, Entradas y Compras en loopback 28080–28084. Una segunda JVM independiente de Inventario usa 28085, el mismo JAR y el mismo esquema. El manifiesto registra puerto, PID y hash. Las JVM usan UTC, TTL de reserva de 15 segundos y scheduler de 1000 ms. |
| Acreditación | Antes de arrancar Inventario 2 se comprueba que la guarda aborta ante su ausencia tras seis GET y cero escrituras. Con ambas JVM activas se exige la acreditación completa de token, servicio, esquema efectivo y destinos salientes. La guarda también admite la red aislada original 18080–18085 con MySQL 33479. El lanzador automático crea siempre la red 28080–28085. |
| Ejecución y cierre | Maven Failsafe ejecuta los 14 escenarios con el perfil `e2e`. El runner Node acredita otra vez los seis procesos y recorre las tres carpetas de Postman. El lanzador libera sus propios procesos y contenedores al terminar o fallar. |

### Escenarios HTTP

| Área | Comprobaciones |
|---|---|
| Último cupo | Diez clientes salen de una barrera común. Cinco reservan por Gateway hacia Inventario 1 y cinco por Inventario 2. Se exige un único HTTP 201, nueve HTTP 409, pago aprobado, compra completada y stock final cero. La suite local de Inventario conserva su prueba de concurrencia propia. |
| Compra y pago | Compra numerada, QR y tickets distintos, repetición de pago sin duplicados, rechazo y liberación, timeout recuperable y compensación de un pago recuperado tras el vencimiento. |
| Devolución y cancelación | Devolución parcial repetida con identidad de asiento y ticket, conservación del otro ticket, reintegro por importe histórico, cancelación repetida y cancelaciones concurrentes de un cobro incierto. |
| Reserva y emisión | Suspensión, imposibilidad de pago después del vencimiento y barrera de anulación contra emisión tardía. |
| Catálogo y comunicación | Búsqueda por ciudad, ruta interna excluida del Gateway, relevancia y devolución en evento agotado, finalización y reanudación de publicación con un sector ya preparado. |

## Lectura de los resultados

Cada corrida completa tiene un identificador nuevo. En `TicketYa/integration-tests/target/runs/<id>/` se guardan los XML por servicio, los reportes Surefire y Failsafe, el manifiesto de procesos, los logs y `postman.json`. El consolidador Java comprueba los XML, la topología, los hashes de los JAR y el de la colección antes de emitir `resumen.json`. Si la ejecución se interrumpe, consultar el log de la etapa y sus XML. Los controles locales independientes guardan su XML bajo `target/runs/local/surefire-reports/`.

Los conteos de una corrida se consultan en sus informes locales. El runner Node implementa las API `pm` usadas por la colección. Para la demostración con el cliente Postman, seguir la [guía de la colección](demostracion.md).
