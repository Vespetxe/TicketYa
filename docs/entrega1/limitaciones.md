# Alcance real, limitaciones y antecedentes

Este documento registra decisiones y límites observables del código. No declara nuevos hallazgos de auditoría ni certifica conformidad académica.

- No hay autenticación de usuario: los IDs de cliente/organizador son declarados; `X-User-Role: ADMIN` es una comprobación de encabezado en Eventos. Las rutas administrativas de Compras exigen `X-Admin-Id` positivo; Eventos también lo exige para sus cambios administrativos y conserva el rol ADMIN. Son identidades declaradas, sin autenticación.
- Pago y reintegro usan una pasarela simulada persistente. No hay integración bancaria.
- Las operaciones distribuidas pueden quedar parcialmente completadas. Repetir la misma operación recupera etapas; no existe worker automático ni rollback global. Una publicación parcialmente inicializada se retoma con la misma configuración; no hay reconciliación general de configuraciones incompatibles simultáneas.
- La búsqueda no implementa nombre, paginación ni ranking textual. Ciudad es opcional y se compara exactamente, sin distinguir mayúsculas. Los eventos antiguos sin ciudad no satisfacen ese filtro.
- Agotado no se detecta automáticamente. No existe endpoint público de reanudación del suspendido. Stock devuelto a un agotado/suspendido no habilita ventas automáticamente.
- El QR es un UUID opaco; validar acceso, detectar uso único y las demás prestaciones de Entrega 2 quedan fuera de esta entrega documental.
- Compras expone IDs numéricos de entradas y Entradas expone UUID. GET `/compras/{id}/entradas` asocia ambas identidades por unidad con su sector, asiento y estado; la demostración de dos asientos elige una entrada desde esa respuesta, sin correspondencia por posición.
- No hay Swagger UI agregado en Gateway ni Swagger dinámico en Entradas. Se importan los contratos estáticos. Algunas anotaciones Swagger históricas de los otros servicios conservan textos/códigos incompletos; el paquete central corrige esas descripciones sin modificar código funcional.
- Los formatos de error difieren por servicio. Un error de conversión o JSON puede usar el formato de Spring; Inventario puede devolver 500 al faltar un parámetro requerido por su manejador genérico. Un 500/503 no demuestra ausencia de efectos.
- Participante tiene validación en la entidad y no en el DTO: un nombre inválido puede fallar al persistir y devolver 500. No se presenta como una validación uniforme de entrada con 400.

## Documentación histórica que no debe usarse como contrato actual

Se conserva para no borrar el trabajo previo. Se recomienda retirarla del circuito de lectura de la entrega o archivarla después de revisión del equipo; no se eliminó ningún archivo.

| Antecedente | Motivo y reemplazo |
|---|---|
| `TicketYa/inventarioReserva_service/README.md` (ejemplos originales) | IDs fijos, inicialización repetida y liberaciones antiguas. Banner de precedencia; usar Postman y OpenAPI centrales. |
| `TicketYa/comprapago_service/docs/flujo-compra.md` | Descripción anterior a la integración completa; reemplazo: arquitectura y secuencias centrales. |
| `TicketYa/comprapago_service/docs/modelo-entidades.md` | Modelo anterior a correspondencias UUID, etapas y cancelación durable; reemplazo: cuatro ER actuales. |
| `TicketYa/comprapago_service/docs/pruebas.md` y `informe-finalizacion.md` | Evidencia histórica con otro alcance; usar pruebas centrales y conservar fechas de las ejecuciones previas. |
| OpenSpec y propuestas anteriores de los servicios | Son antecedentes de diseño, no contratos operativos de la versión integrada. No se reescribieron retrospectivamente. |
| Colecciones antiguas junto a servicios | No garantizan Gateway integrado ni IDs dinámicos; usar colección central de Entrega 1. |

El README principal y los contratos de Entradas se actualizaron tras leer sus versiones anteriores. Los documentos centrales explican las diferencias relevantes; no se cambiaron anotaciones Java, configuración de aplicaciones, procesos o bases de desarrollo.
