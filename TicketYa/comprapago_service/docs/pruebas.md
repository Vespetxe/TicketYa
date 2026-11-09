# Verificación local aislada

> Evidencia histórica del servicio con simuladores. La ejecución integrada vigente y las instrucciones desde la raíz del repositorio están en [pruebas de Entrega 1](../../../docs/entrega1/pruebas.md). Los conteos siguientes no son nuevas ejecuciones.

Requisitos: JDK 21, Docker y Maven (o mvnw.cmd). No se necesita DB_PASSWORD para tests.

El classpath de pruebas contiene su propio application.properties y activa test.
El inicializador EntornoPruebasSeguro rechaza cualquier datasource que no sea
127.0.0.1:33079/ticketya_compras_test antes de inicializar Hibernate.
Ni variables de entorno ni perfiles pueden redirigir accidentalmente la prueba a desarrollo.

Iniciar MySQL temporal, sin volúmenes:

```powershell
docker run --detach --rm --name ticketya-compras-tests --publish 127.0.0.1:33079:3306 --env MYSQL_ROOT_PASSWORD=ticketya-test-only --env MYSQL_DATABASE=ticketya_compras_test mysql:8.4
docker exec ticketya-compras-tests mysqladmin ping -uroot -pticketya-test-only
```

Esperar a que mysqladmin indique que el servidor está disponible. La contraseña mostrada
es exclusivamente del contenedor de tests.

Desde `TicketYa/comprapago_service` (raíz del servicio):

```powershell
.\mvnw.cmd clean compile
.\mvnw.cmd verify
```

Los tests verifican persistencia y restricciones en MySQL, límite y pagos concurrentes,
reserva repetida, clave compartida entre compras, respuestas perdidas de pasarela,
confirmación, emisión y reintegro, compensaciones, recuperación de turnos abandonados,
reconstrucción de simuladores, HTTP real, OpenAPI y Swagger.

Los tests de API abren un puerto HTTP aleatorio; no requieren iniciar la aplicación manualmente.
El esquema aislado usa create-drop y las pruebas limpian sus datos. No ejecutar clases
de integración en paralelo sobre ese mismo esquema.

Detener y eliminar el contenedor temporal:

```powershell
docker stop ticketya-compras-tests
```

No se utilizan ni modifican las credenciales o el esquema de desarrollo.
La suite no demuestra integración con servicios REST reales ni Gateway (sección 7).

## Preparación previa a integración: medio de pago y límite

La verificación del 2026-11-07 ejecutó `mvn -o verify`: 283 pruebas, 0 fallos,
0 errores y 0 omisiones sobre el MySQL aislado configurado. Se conservaron los 268
escenarios anteriores, adaptando las invocaciones de pago para informar el medio,
y se agregaron 15 pruebas específicas en las suites existentes:

- ApiIntegracionTest: cuatro medios, persistencia/consulta, formatos inválidos,
  identidad de reintento, históricos sin dato y esquema OpenAPI del campo.
- PagoServiceTest: ausencia antes de llamadas externas y cambio de medio con resultado incierto.
- PagoRepositoryTest: conservación de históricos null; la prueba existente verifica medio persistido.
- FlujoCompraIntegracionTest: recuperación de un pago histórico pendiente sin atribuirle un medio.
- ReintegroIntegracionTest: cupo tras devolución parcial/total, repetición e historial de rechazo,
  solicitudes/aprobaciones sin efecto financiero, reintegros inciertos/rechazados,
  anulación pendiente y consumo concurrente del cupo devuelto.

Estas pruebas no certifican una migración de datos de desarrollo ni la no doble venta
del inventario real. No iniciar ni crear una base diferente para reproducirlas.
