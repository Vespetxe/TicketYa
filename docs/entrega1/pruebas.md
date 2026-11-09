# Pruebas: evidencia y reproducción aislada

## Evidencia de la verificación integrada más reciente

Estas ejecuciones son **evidencia histórica conservada**, no pruebas ejecutadas nuevamente durante la limpieza del entregable. La fuente es [astra-results.json](../../TicketYa/integration-tests/astra-results.json), con fechas, suites/casos, identidad de HEAD y hashes de los artefactos utilizados.

| Suite registrada | Casos | Resultado |
|---|---:|---|
| Eventos | 34 | 0 fallos, errores u omitidos |
| Inventario | 38 | 0 fallos, errores u omitidos |
| Compras | 289 | 0 fallos, errores u omitidos |
| Entradas | 31 | 0 fallos, errores u omitidos |
| Gateway | 3 | 0 fallos, errores u omitidos |
| Total Java | **395** | Aprobadas |
| Aislamiento Python | **4** | Aprobadas; 22 variantes negativas de destinos |
| HTTP real | **14** | Aprobados; dos instancias de Inventario |
| Colección mediante runner Node | **77 solicitudes / 127 aserciones** | Aprobadas |

### Regresiones y escenarios comprobados

- Publicación interrumpida: stock PREPARADO no reservable mientras Eventos sigue BORRADOR; reservar devuelve 409. Reintentar reutiliza sectores compatibles sin duplicar cupo. La habilitación fallida se recupera sobre PUBLICADO sin reinicializar; una activación tardía no reabre suspensión/cancelación. Configuración incompatible es 422 permanente, separada del 503 temporal.
- Devolución parcial: GET `/compras/{id}/entradas` vincula entradaId numérico, UUID, sector y asiento; la prueba compra dos asientos y verifica el UUID exacto anulado, el otro vigente, el reintegro histórico y la reposición de un único asiento, incluso al repetir.
- Concurrencia: carrera de habilitación/cancelación, no doble venta del último cupo entre dos instancias y cancelación concurrente de un pago incierto. Diez competidores por el último cupo: una compra completada y nueve conflictos.
- Compras y pagos: aprobación, rechazo/liberación, vencimiento, pago incierto, recuperación posterior al vencimiento con compensación, emisión y reintentos sin duplicados.
- Cancelación y reintegros: barreras durables de orden/evento impiden emisión tardía, cancelación recupera pagos en procesamiento y comprueba dinero, tickets y stock sin reofrecer un evento cancelado.
- Contratos y administración: actor positivo X-Admin-Id, consulta administrativa con estado obligatorio, búsqueda/relevancia, estados de eventos y exclusión de rutas internas del Gateway. Se corrigió el 500 de lectura lazy de asientos con transacción de solo lectura y prueba HTTP con OpenInView desactivado.

Pruebas específicas: `PublicacionInventarioTest`, `PublicacionInterrumpidaTest`, `PublicacionRecuperacionTest`, `SeleccionEntradaYActorTest`, `AsientosHttpTest`, `AcreditacionDestinosTest` y los escenarios de `entrega1_test.py`. No se observaron fallos en las corridas finales registradas.

### Condiciones de ejecución registradas

Se ejecutó `mvn -o test` por proyecto con JDK 21. Eventos, Entradas e Inventario usaron H2 protegido y efímero; Gateway proveedores de prueba; Compras exclusivamente `ticketya_compras_test` en el MySQL desechable 33079.

Para HTTP se creó el contenedor nuevo `ticketya-astra-tests`, MySQL loopback 33489 y cuatro esquemas nuevos `ticketya_e1_*`. Se iniciaron seis procesos nuevos en loopback 28080–28085, perfil `integracion,pruebas-integrales`, offset 10000 y token aleatorio. Antes del primer POST pasaron las pruebas negativas y la acreditación GET de las bases efectivas, URLs datasource y todos los destinos REST salientes de Eventos, Compras, Entradas y Gateway. Una URL normal o ausente aborta antes de escribir. Hubo nueve pruebas Java de acreditación y 22 variantes negativas de destino en Python.

TTL de reservas: 15 s; scheduler: 1000 ms, solo en el entorno aislado. Se conservaron los procesos anteriores en 8080–8084 y 18080–18085 y no se modificaron bases de desarrollo. Solo se reinició el Eventos aislado creado para esa verificación al corregir su lectura de asientos.

El empaquetado convencional encontró JAR bloqueados en Windows. Las compilaciones utilizadas se generaron en salidas separadas `target/astra-build` y Eventos final en `target/astra-build-next`. Los primeros intentos de Mockito dentro del sandbox se interrumpieron; los resultados aprobados corresponden a las ejecuciones posteriores que finalizaron correctamente. Esas rutas son procedencia histórica, no requisitos del script actual: `iniciar_entorno_integral.ps1` siempre reconstruye desde fuentes en salidas nuevas.

Evidencia conservada: [HTTP](../../TicketYa/integration-tests/astra-run.log), [aislamiento Python](../../TicketYa/integration-tests/astra-isolation-python.log), [colección](../../TicketYa/integration-tests/astra-postman.log) y [resultados por solicitud y hash de la colección](../../TicketYa/integration-tests/astra-postman-results.json). El runner ejecutó las API pm utilizadas por esta colección; no se usó Postman/Newman.

### Alcance y limitaciones de la evidencia

La comprobación estática de rutas/métodos contra controladores, referencias y ejemplos OpenAPI, parámetros obligatorios, sintaxis JavaScript, enlaces y claves ER se registra separadamente en [verificacion-documental.json](verificacion-documental.json). No equivale a las ejecuciones Java/HTTP.

No se ejecutaron renderizado Mermaid ni validadores normativos externos OpenAPI/Postman. Las corridas no certifican toda combinación de fallas ni un clon de un commit futuro. No hay transacción distribuida: PUBLICADO puede quedar temporalmente sin habilitación comercial y requiere repetir publicar. No hay worker general de recuperación automática. Las bases existentes no fueron migradas y los procesos normales conservan su versión hasta un despliegue autorizado. Las funcionalidades previstas de E2 no se implementaron.

En la preparación Git posterior se reconstruyeron los cinco proyectos en BuildOnly y sus 233 clases de producción coincidieron byte por byte con los artefactos acreditados anteriores. Esto no constituye otra ejecución de las 395 pruebas.

## Evidencia de la verificación funcional anterior

Fuente conservada: [close-results.json](../../TicketYa/integration-tests/close-results.json), registrado el 8 de noviembre de 2026 a las 21:53 UTC, y [close-run.log](../../TicketYa/integration-tests/close-run.log). Estos resultados **no son nuevas ejecuciones de esta preparación documental**.

| Suite anterior | Pruebas | Fallos / errores / omitidas |
|---|---:|---|
| Compras | 284 | 0 / 0 / 0 |
| Entradas | 28 | 0 / 0 / 0 |
| Eventos | 28 | 0 / 0 / 0 |
| Inventario | 35 | 0 / 0 / 0 |
| Gateway | 3 | 0 / 0 / 0 |
| Total Java | 378 | 0 / 0 / 0 |
| Aislamiento Python | 2 | 0 fallos |
| Escenarios HTTP reales | 14 | 0 fallos; dos instancias de Inventario |

La suite HTTP registrada cubrió compra numerada/reintentos, rechazo/liberación, pago incierto, recuperación tras vencimiento, devolución parcial idempotente, cancelación y verificación financiera, cancelación concurrente de pago incierto, último cupo concurrente entre dos instancias, suspensión, anulación previa a emisión, reserva vencida, búsqueda y exclusión de rutas internas, estados/relevancia/devolución de agotado y publicación parcialmente inicializada. No equivale a una prueba exhaustiva de toda combinación de fallas.

## Pruebas Java en entorno separado

Estos comandos son instrucciones para quien reproduce, no acciones ejecutadas durante esta tarea documental. Usar terminales nuevas, sin variables heredadas del arranque normal. Las pruebas de persistencia pueden crear y borrar **sus tablas de prueba**; nunca apuntarlas a bases de desarrollo.

Compras exige MySQL en `127.0.0.1:33079/ticketya_compras_test`. Para crear una instancia desechable con nombre nuevo y contraseña de ejemplo exclusivamente de prueba:

```powershell
docker run --name ticketya-doc-unit-mysql -e MYSQL_ROOT_PASSWORD=ticketya-test-only -e MYSQL_DATABASE=ticketya_compras_test -p 127.0.0.1:33079:3306 -d mysql:8.4
docker exec ticketya-doc-unit-mysql mysqladmin ping -uroot -pticketya-test-only
```

Esperar hasta que el segundo comando indique disponible; no reutilizar un nombre/puerto ocupado ni quitar contenedores previos. Desde raíz, ejecutar cada bloque en una terminal nueva:

```powershell
$env:DB_TEST_PASSWORD = 'ticketya-test-only'
Set-Location TicketYa/comprapago_service
mvn test
```

```powershell
Set-Location TicketYa/EventoCatalogo_Service
mvn test
```

```powershell
Set-Location TicketYa/entradas_service
mvn test
```

```powershell
Set-Location TicketYa/inventarioReserva_service
mvn test
```

```powershell
Set-Location TicketYa/api-gateway
mvn test
```

Eventos usa H2 `ticketya_eventos_test`; Entradas H2 `ticketya`; Inventario H2 `ticketya_inventario_test` por defecto. Las guardas se ejecutan antes de Hibernate y rechazan destinos incompatibles. Para repetir Inventario contra MySQL, crear `ticketya_inventario_test` en la instancia desechable 33079 y configurar `TEST_DB_URL=jdbc:mysql://127.0.0.1:33079/ticketya_inventario_test?allowPublicKeyRetrieval=true&useSSL=false`, `TEST_DB_DRIVER=com.mysql.cj.jdbc.Driver`, `TEST_DB_USERNAME=root`, `TEST_DB_PASSWORD=ticketya-test-only` y `TEST_DB_DIALECT=org.hibernate.dialect.MySQLDialect` antes de `mvn test`.

## Integrales HTTP reales

### Preparar una instancia nueva

La prueba integral no admite apuntar simplemente a una URL elegida: exige puertos, esquemas y token acreditados. Crear otra instancia nueva, independiente de 33079 y del entorno normal:

```powershell
docker run --name ticketya-doc-http-mysql -e MYSQL_ROOT_PASSWORD=ticketya-test-only -p 127.0.0.1:33479:3306 -d mysql:8.4
docker exec ticketya-doc-http-mysql mysqladmin ping -uroot -pticketya-test-only
```

Cuando MySQL esté disponible, ejecutar en su cliente las sentencias siguientes (o abrir cliente interactivo con `docker exec -it ticketya-doc-http-mysql mysql -uroot -p`):

```sql
CREATE DATABASE ticketya_e1_eventos CHARACTER SET utf8mb4;
CREATE DATABASE ticketya_e1_inventario CHARACTER SET utf8mb4;
CREATE DATABASE ticketya_e1_entradas CHARACTER SET utf8mb4;
CREATE DATABASE ticketya_e1_compras CHARACTER SET utf8mb4;
```

Los nombres exactos son parte de las guardas. Si ya existen recursos en 33479/18080–18085, no detenerlos sin revisar su propiedad; este procedimiento requiere recursos disponibles y expresamente aislados.

### Arrancar procesos aislados

Generar una vez un token: `[guid]::NewGuid().ToString('N') + [guid]::NewGuid().ToString('N')`. Copiar el mismo valor a seis terminales nuevas y a la terminal de Python. No guardarlo en archivos compartidos. En cada terminal de servicio establecer:

```powershell
$env:TICKETYA_TEST_TOKEN = Read-Host 'Token aleatorio común de esta ejecución'
$env:DB_USERNAME = 'root'
$env:DB_PASSWORD = 'ticketya-test-only'
$env:SPRING_PROFILES_ACTIVE = 'integracion,pruebas-integrales'
$env:EVENTOS_BASE_URL = 'http://127.0.0.1:18081'
$env:INVENTARIO_BASE_URL = 'http://127.0.0.1:18082'
$env:ENTRADAS_BASE_URL = 'http://127.0.0.1:18083'
$env:COMPRAS_BASE_URL = 'http://127.0.0.1:18084'
```

En cada terminal, desde la raíz, elegir una fila y establecer `DB_URL` con su esquema y el sufijo de conexión indicado. Después ir al directorio y ejecutar el comando correspondiente:

| Proyecto | DB_URL | Comando |
|---|---|---|
| Inventario | `jdbc:mysql://127.0.0.1:33479/ticketya_e1_inventario?allowPublicKeyRetrieval=true&useSSL=false` | `mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=18082 --reservas.ttl-seconds=15 --reservas.expiration-interval-ms=1000"` |
| Segunda instancia Inventario | La misma base de Inventario | `mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=18085 --reservas.ttl-seconds=15 --reservas.expiration-interval-ms=1000"` |
| Entradas | `jdbc:mysql://127.0.0.1:33479/ticketya_e1_entradas?allowPublicKeyRetrieval=true&useSSL=false` | `mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=18083"` |
| Compras | `jdbc:mysql://127.0.0.1:33479/ticketya_e1_compras?allowPublicKeyRetrieval=true&useSSL=false` | `mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=18084"` |
| Eventos | `jdbc:mysql://127.0.0.1:33479/ticketya_e1_eventos?allowPublicKeyRetrieval=true&useSSL=false` | `mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=18081"` |
| Gateway | Sin BD; omitir DB_URL | `mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=18080"` |

Ejemplo de selección de una fila desde raíz: `$env:DB_URL='jdbc:mysql://127.0.0.1:33479/ticketya_e1_inventario?allowPublicKeyRetrieval=true&useSSL=false'`, luego `Set-Location TicketYa/inventarioReserva_service`, luego su comando Maven. Arrancar la segunda instancia después de que la primera cree las tablas.

### Acreditar y ejecutar

En una terminal de Python, desde raíz:

```powershell
$env:TICKETYA_TEST_TOKEN = Read-Host 'El mismo token de los seis procesos'
$env:TICKETYA_SECOND_INVENTORY = '1'
Set-Location TicketYa/integration-tests
python -B -m unittest test_isolation.py -v
Set-Location ..
python -B integration-tests/entrega1_test.py
```

La primera suite prueba el verificador con respuestas controladas. La segunda, antes de crear datos, consulta `/internal/test-environment` con `X-TicketYa-Test-Token`: los servicios verifican el esquema mediante `SELECT DATABASE()` y la URL loopback 33479; Gateway verifica sus destinos exactos 18081–18084. También se acredita la instancia 18085. Token menor a 32 caracteres o acreditación incorrecta impiden iniciar los escenarios.

Las pruebas crean datos nuevos en esas bases y no limpian automáticamente al terminar. Los escenarios de expiración requieren TTL menor a 40 s: no usar el valor normal de 600. El perfil y las conexiones de prueba no deben trasladarse al entorno normal. La eliminación posterior de contenedores/volúmenes queda fuera de esta guía de ejecución no destructiva.

## Verificaciones documentales y evidencia de ejecución

Se verificaron estáticamente enlaces, JSON, referencias locales OpenAPI, correspondencia de rutas y ejemplos Postman, claves ER y separación de rutas internas. El resultado se registra en [verificacion-documental.json](verificacion-documental.json). En la preparación documental anterior no se ejecutaron suites con escrituras. En el cierre de Astra sí se ejecutaron Java, Python, HTTP y la colección mediante runner Node, exclusivamente en entornos desechables; ver la evidencia histórica consolidada al comienzo de este documento. No se utilizó el cliente Postman/Newman. Los diagramas se entregan en texto Mermaid; la validación estructural no sustituye su renderizado ni una auditoría independiente de conformidad.

## Segunda red aislada utilizada en el cierre

Los puertos 18080–18085 estaban ocupados. Se conservaron y se agregó una segunda red cerrada de pruebas: `TICKETYA_TEST_PORT_OFFSET=10000`, puertos 28080–28085 y MySQL nuevo en 33489, con los mismos nombres exactos `ticketya_e1_*`. Solo se aceptan offset 0 o 10000. Los servicios y verificadores comprueban la URL exacta de cada base y todos los destinos REST correspondientes; no admiten mezclar las dos redes ni usar 8080–8084.

Para reproducir esa red, aplicar el procedimiento anterior cambiando los puertos de aplicaciones por 2808x, MySQL por 33489 y agregando el offset a **todos los procesos y al runner**. Usar un contenedor nuevo, token aleatorio común y credenciales propias de prueba. No reemplazar ni detener recursos que ya estén ocupados. El script `integration-tests/iniciar_entorno_integral.ps1` conserva el procedimiento usado en Windows y ahora reconstruye siempre desde fuentes actuales en salidas únicas; requiere JDK 21 y Maven instalado. La contraseña del MySQL desechable se toma de DB_PASSWORD o de un archivo privado bajo target; nunca se versionan ese archivo ni el token. Con -BuildOnly -Offline se compilan los cinco proyectos desde fuentes sin arrancar servicios, usando dependencias ya disponibles.

Antes del primer POST se ejecutaron y aprobaron las pruebas negativas con transporte/JDBC simulados y la acreditación GET de los seis procesos. Cada destino saliente de Eventos, Compras y Entradas se acredita además de los destinos del Gateway y la base efectiva; una URL normal o ausente aborta. La prueba de devolución comprueba UUID y asiento exacto, vigencia del otro ticket, dinero y reposición de un solo asiento. La publicación parcial comprueba 409 al intentar reservar el sector preparado de un BORRADOR.

Para ejecutar la colección con el runner incluido, desde raíz y en la terminal con token/offset correctos: `node TicketYa/integration-tests/run_postman_collection.cjs`. El runner acredita primero los seis procesos y ejecuta los scripts pm usados por esta colección. No es una implementación general de Postman/Newman.
