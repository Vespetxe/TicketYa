# OpenSpec de compras-service

Las especificaciones de este microservicio se encuentran en `compras-service/openspec/`
desde la raíz del repositorio TicketYa. La configuración está en `openspec/config.yaml`
desde la raíz del servicio y utiliza el esquema `spec-driven`.

Ejecutar los comandos desde `compras-service/`:

```powershell
cd compras-service
openspec list --json
openspec status --change compras-pagos --json
openspec validate compras-pagos --strict --json
openspec instructions apply --change compras-pagos --json
```

Si la terminal ya está en `compras-service/`, omitir el comando `cd`.
OpenSpec detecta esta raíz local automáticamente. Para trabajar con este servicio
desde un agente abierto en TicketYa, establecer también `compras-service/` como
directorio de trabajo de los comandos OpenSpec.

Las rutas `openspec/`, `docs/` y `db/` de los artefactos de OpenSpec se interpretan
desde la raíz del servicio. Las referencias `openspec/` de los informes existentes
en `docs/` usan la misma base. Las herramientas genéricas de `.agents/skills/`
permanecen en la raíz del repositorio; no contienen una ruta fija al proyecto.
