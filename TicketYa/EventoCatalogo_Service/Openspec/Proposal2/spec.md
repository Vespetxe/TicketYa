## 2. Specs (Especificación de Requisitos)

### Requisito 1: Creación de Eventos y Precios por Sector
Los organizadores crean eventos en estado `borrador`. Para poder publicarlos, deben asociar un lugar (`venueId`), fecha/hora válida y configurar el precio para cada sector del lugar.

* **Escenario Exitoso:** Evento creado en borrador con precios.
    * **Dado** que un organizador envía la información de un evento (nombre, tipo, fecha futura, `venueId`, lista de `participantId`).
    * **Cuando** se ejecuta `POST /api/v1/events`.
    * **Entonces** el evento se guarda con estado `"BORRADOR"` y se habilita la carga de precios por sector (`POST /api/v1/events/{eventId}/prices`).

* **Escenario Negativo:** Publicar evento sin precios configurados en todos los sectores.
    * **Dado** un evento en estado `"BORRADOR"` con 3 sectores en su `venueId`, pero solo 2 sectores con precio configurado.
    * **Cuando** el organizador solicita `PATCH /api/v1/events/{eventId}/publish`.
    * **Entonces** el sistema rechaza la publicación con HTTP `422 Unprocessable Entity` indicando la falta de precio en el sector faltante.

---

### Requisito 2: Transiciones de Estado del Evento
El estado del evento solo puede transicionar según las reglas permitidas:
$\text{BORRADOR} \rightarrow \text{PUBLICADO} \rightarrow (\text{AGOTADO} \mid \text{CANCELADO} \mid \text{FINALIZADO})$.

* **Escenario Exitoso:** Cancelación administrativa.
    * **Dado** un evento en estado `"PUBLICADO"`.
    * **Cuando** un usuario con rol de Administrador (identificado vía encabezado `X-User-Role: ADMIN`) realiza `PATCH /api/v1/events/{eventId}/cancel`.
    * **Entonces** el estado cambia a `"CANCELADO"` y se retorna el evento actualizado con HTTP `200 OK`.

* **Escenario Negativo:** Transición no permitida.
    * **Dado** un evento en estado `"FINALIZADO"`.
    * **Cuando** un organizador intenta cambiar el estado a `"PUBLICADO"`.
    * **Entonces** el sistema responde con HTTP `409 Conflict` prohibiendo la modificación.

---