## 2. Specs (Especificación de Requisitos)

### Requisito 1: Búsqueda Multi-Criterio de Eventos
Los clientes pueden realizar búsquedas especificando uno o más parámetros de filtro de forma opcional.

* **Escenario Exitoso:** Filtrar eventos por ciudad y tipo en un rango de fechas.
    * **Dado** una lista de eventos publicados en la base de datos.
    * **Cuando** el cliente realiza `GET /api/v1/events/search?city=Tandil&type=RECITAL&fromDate=2026-05-01&toDate=2026-05-31`.
    * **Entonces** el sistema devuelve únicamente los eventos en estado `"PUBLICADO"` o `"AGOTADO"` que coincidan con todos los filtros enviados y respuesta HTTP `200 OK`.

* **Escenario Negativo:** Rango de fechas incoherente.
    * **Dado** que se envía un filtro con `fromDate` posterior a `toDate` (`fromDate=2026-06-01` y `toDate=2026-05-01`).
    * **Cuando** se ejecuta la búsqueda HTTP GET.
    * **Entonces** el backend intercepta el error y responde con HTTP `400 Bad Request` indicando la inconsistencia en las fechas.

---

### Requisito 2: Ordenamiento de Resultados
El usuario puede solicitar el ordenamiento de los eventos filtrados por `DATE_ASC`, `DATE_DESC`, `PRICE_ASC` o `PRICE_DESC`.

* **Escenario Exitoso:** Ordenar catálogo por precio ascendente.
    * **Dado** múltiples eventos publicados con diferentes rangos de precio por sector.
    * **Cuando** se ejecuta `GET /api/v1/events/search?sort=PRICE_ASC`.
    * **Entonces** el sistema calcula el precio mínimo de cada evento y responde con la lista ordenada de menor a mayor precio.

---
