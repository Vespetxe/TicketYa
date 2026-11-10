# TicketYa — Entrega 1

Plataforma de eventos, inventario y reservas, compras y pagos simulados, y emisión de entradas. Cuatro microservicios independientes se integran por REST detrás de un API Gateway. Este README describe la versión integrada del código fuente; reconstruirla evita utilizar JAR anteriores.

La [documentación de Entrega 1](docs/entrega1/README.md) reúne historias, aceptación, arquitectura, secuencias, cuatro modelos relacionales, OpenAPI, evidencia de pruebas y límites reales. El [diagrama global](docs/entrega1/arquitectura.md) explica las llamadas entre servicios y la pasarela simulada, que forma parte de Compras.

## Requisitos

- JDK 21 (sirve para los cinco proyectos) y `JAVA_HOME` configurado.
- Maven 3.9 o posterior disponible como `mvn`; Gateway requiere Maven instalado. Los otros proyectos también incluyen wrapper.
- MySQL 8.4 recomendado; usuario con permisos sobre cuatro esquemas nuevos. Hibernate crea/actualiza tablas al arrancar.
- PowerShell para copiar los comandos de esta guía; equivalentes de variables de entorno en otra shell.
- Node.js para validar los scripts de la colección. Docker con MySQL 8.4 y PowerShell 7 para la corrida aislada completa. Postman es opcional para la demostración.
- Acceso al repositorio Maven al descargar dependencias por primera vez. El arranque de los microservicios requiere Java y MySQL.

Comprobar `java -version` y `mvn -version` antes de arrancar. Cada proyecto tiene su propio `pom.xml`: no hay un reactor Maven en la raíz.

## Estructura y puertos

| Directorio dentro de TicketYa | Aplicación | Puerto      | Esquema normal |
|---|---|-------------|---|
| `EventoCatalogo_Service` | Eventos y Catálogo | 8081        | eventos_catalogo_DB |
| `inventarioReserva_service` | Inventario y Reservas | 8082        | db_inventario_reservas |
| `entradas_service` | Entradas | 8083        | ticketya_entradas |
| `comprapago_service` | Compras y Pagos | 8084        | ticketya_compras |
| `api-gateway` | Gateway público | 8080        | Sin BD |
| `integration-tests` | Escenarios HTTP y acreditación de aislamiento | 28080–28085 | Solo esquemas de prueba |

`docs/entrega1` contiene los documentos vigentes. Los antecedentes junto a cada servicio se identifican en el [inventario de documentos históricos](docs/entrega1/limitaciones.md).

## Crear un entorno nuevo

Usar una instancia/esquemas nuevos para la demostración; no ejecutar ejemplos sobre datos de desarrollo existentes. El archivo [crear-esquemas.sql](docs/entrega1/ejemplos/crear-esquemas.sql) crea los cuatro esquemas predeterminados sin borrar tablas. En el cliente MySQL, conectado al servidor elegido con credenciales propias, ejecutar:

```sql
SOURCE docs/entrega1/ejemplos/crear-esquemas.sql;
```

La ruta se interpreta desde el directorio donde se abrió el cliente. También se puede abrir el archivo y ejecutar sus cuatro sentencias en Workbench. `IF NOT EXISTS` no garantiza que una base esté vacía: comprobar que los nombres no correspondan a datos previos. Para usar otros esquemas, crearlos y cambiar cada `DB_URL`.

No se incluyen usuarios ni contraseñas reales. Proporcionar credenciales de la instancia elegida. La conexión admite por ejemplo `jdbc:mysql://127.0.0.1:3306/<esquema>?allowPublicKeyRetrieval=true&useSSL=false` para un MySQL local de demostración. Configurar TLS apropiado si el servidor lo requiere.

## Configurar y arrancar los cinco proyectos

Abrir cinco terminales **desde la raíz del repositorio**. Las variables pertenecen a cada terminal y no se comparten automáticamente. En cada una establecer:

```powershell
$env:DB_USERNAME = Read-Host 'Usuario de MySQL para esta demostración'
$env:DB_PASSWORD = Read-Host 'Contraseña de MySQL' -MaskInput
$env:SPRING_PROFILES_ACTIVE = 'integracion'
$env:EVENTOS_BASE_URL = 'http://localhost:8081'
$env:INVENTARIO_BASE_URL = 'http://localhost:8082'
$env:ENTRADAS_BASE_URL = 'http://localhost:8083'
$env:COMPRAS_BASE_URL = 'http://localhost:8084'
```

`-MaskInput` requiere PowerShell 7; en PowerShell 5 usar el mecanismo de secretos del entorno o asignar la variable privadamente. No guardar credenciales en el repositorio. Gateway no usa `DB_*`, por lo que allí se pueden omitir esas dos variables.

Arrancar primero Inventario y Entradas, luego Compras, Eventos y Gateway. Las dependencias REST deben estar disponibles al ejecutar los flujos, especialmente publicación y pago. Elegir **un bloque por terminal**, después de las variables comunes:

```powershell
# Terminal Inventario
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/db_inventario_reservas?allowPublicKeyRetrieval=true&useSSL=false'
Set-Location TicketYa/inventarioReserva_service
mvn spring-boot:run
```

```powershell
# Terminal Entradas
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/ticketya_entradas?allowPublicKeyRetrieval=true&useSSL=false'
Set-Location TicketYa/entradas_service
mvn spring-boot:run
```

```powershell
# Terminal Compras
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/ticketya_compras?allowPublicKeyRetrieval=true&useSSL=false'
Set-Location TicketYa/comprapago_service
mvn spring-boot:run
```

```powershell
# Terminal Eventos
$env:DB_URL = 'jdbc:mysql://127.0.0.1:3306/eventos_catalogo_DB?allowPublicKeyRetrieval=true&useSSL=false'
Set-Location TicketYa/EventoCatalogo_Service
mvn spring-boot:run
```

```powershell
# Terminal Gateway
Set-Location TicketYa/api-gateway
mvn spring-boot:run
```

Usar `mvn spring-boot:run` compila el código actual sin ejecutar las pruebas de base de datos. Para generar JAR sin lanzar tests, ejecutar `mvn package -DskipTests` en cada proyecto. Ejecutar los tests es un paso separado con las condiciones de aislamiento indicadas abajo.

El perfil `integracion` de Compras usa los tres servicios reales. La pasarela financiera sigue simulada: aprueba importes hasta 100000 inclusive. No activar perfiles de prueba sobre estos esquemas. Para cambiar puertos de aplicaciones que los fijan en properties, usar `mvn spring-boot:run "-Dspring-boot.run.arguments=--server.port=<puerto>"` y actualizar las cuatro URLs en los procesos que las consumen.

## Acceso y Swagger

Entrada pública: `http://localhost:8080`. Una consulta inicial sin escrituras es GET `http://localhost:8080/api/v1/events/search?sort=RELEVANCE`; en base nueva devuelve `[]`. No existe página principal de frontend.

| Servicio | OpenAPI dinámico | Swagger UI |
|---|---|---|
| Eventos | http://localhost:8081/api-docs | http://localhost:8081/swagger-ui.html |
| Inventario | http://localhost:8082/api-docs | http://localhost:8082/swagger-ui.html |
| Compras | http://localhost:8084/v3/api-docs | http://localhost:8084/swagger-ui/index.html |
| Entradas | [Contrato estático](docs/entrega1/openapi/entradas.json) | Importar archivo en visor compatible OpenAPI 3.1 |
| Gateway | [Contrato público estático](docs/entrega1/openapi/gateway.json) | Importar archivo en visor compatible OpenAPI 3.1 |

Los [contratos centrales](docs/entrega1/openapi/README.md) corrigen textos de anotaciones antiguas y distinguen comandos internos y administrativos. Gateway no agrega Swagger UI. No publica `/internal/**` en el perfil normal ni permite confirmar/liberar inventario directamente; sí publica las rutas administrativas de Compras según su configuración actual.

## Demostrar los circuitos

Importar la [colección](docs/entrega1/postman/TicketYa-Entrega1.postman_collection.json) y el [entorno de ejemplo](docs/entrega1/postman/local.postman_environment.json). Seleccionar ese entorno, revisar `base_url` y ejecutar una carpeta completa en orden. Cada carpeta crea sus propios datos y genera fechas a ocho días. No hay un seed que dependa de IDs existentes.

Las carpetas cubren compra numerada aprobada y devolución; compra general y cancelación; pago rechazado. También incluyen publicación, búsqueda, suspensión, consultas y reintentos con claves estables. Ver [pasos y resultados esperados](docs/entrega1/demostracion.md).

## Pruebas seguras

El entorno normal anterior es distinto del entorno de prueba. No ejecutar escenarios integrales contra 8080 ni reutilizar sus esquemas. Las pruebas de Compras requieren MySQL desechable en 33079; los escenarios HTTP requieren otra instancia en 33489, esquemas exactos `ticketya_e1_*`, perfil `pruebas-integrales`, token aleatorio y puertos 28080–28085. La acreditación verifica la BD efectiva y destinos antes de escribir; un simple indicador booleano no basta.

Las pruebas de Entrega 1 se ejecutan con Java 21 y Maven. La guía incluye el comando de la corrida completa, la acreditación de los seis procesos y la ubicación de los informes locales. [Procedimiento de pruebas](docs/entrega1/pruebas.md).

## Problemas frecuentes

| Problema | Comprobación y solución |
|---|---|
| Puerto ocupado | Revisar qué proceso lo usa; no terminar procesos ajenos. Elegir puertos libres y ajustar URLs, o usar el entorno aislado indicado. |
| Fallo de conexión/credenciales MySQL | Comprobar esquema, puerto, permisos y `DB_PASSWORD` en **esa terminal**. Crear el esquema antes del arranque. |
| Java/Maven incompatible | Verificar que `mvn -version` use JDK 21 y que Maven esté en PATH. |
| 503 al publicar, pagar o cancelar | Revisar dependencias y sus URLs. Puede haber efectos previos: consultar estados y repetir con la misma configuración/clave; no crear otro cobro. |
| Confirmación o emisión no cambia tras editar código | Reconstruir desde fuentes; un JAR/proceso iniciado anteriormente conserva su versión. Esta guía no reinicia procesos existentes automáticamente. |
| IDs de asiento no coinciden | Reservar con `idAsiento` de disponibilidad, no con `seatId` del catálogo. |
| Devolución rechaza el ID de ticket | Consultar `/compras/{id}/entradas`: asocia `entradaId` numérico para devolución con `ticketId` UUID y asiento, por unidad. |
| Expiración lenta o prueba HTTP abortada | TTL normal 600 s; las pruebas aisladas necesitan 15 s y scheduler 1000 ms. No cambiar el entorno normal para correrlas. |
| Swagger incompleto o ruta interna 404 en Gateway | Usar los contratos centrales y el servicio propietario para coordinación interna. 404 en Gateway es esperado para rutas excluidas. |
| Datos del ejemplo ya existen | Ejecutar carpeta completa: genera nuevos IDs y sufijo. No repetir solamente creación de compra sobre una reserva ya utilizada. |

La [lista de límites reales](docs/entrega1/limitaciones.md) forma parte de la documentación de la entrega. La auditoría final de conformidad se realiza por separado.

Para las decisiones administrativas enviar `X-Admin-Id` positivo; en cancelar/suspender/status de Eventos también `X-User-Role: ADMIN`. Las reservas se habilitan solo después de confirmar la publicación; una habilitación pendiente se recupera repitiendo publicar. Una inicialización incompatible devuelve 422 y requiere revisar la configuración.
