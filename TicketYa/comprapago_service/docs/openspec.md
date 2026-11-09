# OpenSpec de Compras y Pagos

Las especificaciones utilizadas se conservan en [openspec](../openspec/changes/compras-pagos/proposal.md) del proyecto `TicketYa/comprapago_service`. El artifactId Maven es `compras-service`; **no es el nombre del directorio**. La configuración local es [openspec/config.yaml](../openspec/config.yaml), con esquema `spec-driven`.

Con la CLI OpenSpec instalada, ejecutar desde la raíz de este repositorio:

```powershell
Set-Location TicketYa/comprapago_service
openspec list --json
openspec status --change compras-pagos --json
openspec validate compras-pagos --strict --json
openspec instructions apply --change compras-pagos --json
```

Si la terminal ya está en `TicketYa`, usar `Set-Location comprapago_service`; si está en la raíz del servicio, omitir ese paso. Los comandos muestran el cambio local `compras-pagos`; no es necesario mover ni renombrar el proyecto. `openspec/`, `docs/` y `db/` dentro de sus artefactos se interpretan desde la raíz del servicio.

El diseño y los registros de tareas conservan cortes históricos de implementación. El sistema vigente incluye Gateway y adaptadores REST reales para Eventos, Reservas y Entradas bajo el perfil `integracion`; solo la pasarela financiera sigue simulada. La arquitectura actual está en [arquitectura](../../../docs/entrega1/arquitectura.md), los contratos en [OpenAPI](../../../docs/entrega1/openapi/README.md) y la evidencia ejecutada en [pruebas](../../../docs/entrega1/pruebas.md). Las anotaciones históricas no sustituyen esos documentos ni prueban por sí solas una ejecución nueva.
