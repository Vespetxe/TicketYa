## 2. Specs (Especificación de Requisitos)

### Requisito 1: Registro de Lugares con Coordenadas Geográficas
El administrador debe poder dar de alta un lugar indicando su nombre, dirección postal y ubicación geográfica ($latitud$ y $longitud$).

* **Escenario Exitoso:** Crear un lugar correctamente.
    * **Dado** que el administrador envía una solicitud con nombre `"Teatro Gran Rex"`, dirección `"Av. Corrientes 857"`, latitud $-34.6037$ y longitud $-58.3789$.
    * **Cuando** se procesa la solicitud `POST /api/v1/venues`.
    * **Entonces** el sistema persiste el lugar, asigna un `venueId` único y retorna el código HTTP `201 Created`.

* **Escenario Negativo:** Datos de ubicación inválidos.
    * **Dado** que se envía una solicitud de creación con latitud $100.0$ (fuera de rango $[-90, 90]$).
    * **Cuando** se ejecuta la solicitud `POST /api/v1/venues`.
    * **Entonces** el sistema rechaza la petición con código HTTP `400 Bad Request` e indica el error de validación en la latitud.

---
### Requisito 2: Definición de Sectores (Numerados vs No Numerados)
Cada lugar debe permitir asociar sectores que pueden ser de tipo **Numerado** (filas y asientos específicos) o **No Numerado** (zona con aforo máximo).

* **Escenario Exitoso 1:** Crear sector no numerado (ej. "Campo General").
    * **Dado** un `venueId` existente.
    * **Cuando** se envía `POST /api/v1/venues/{venueId}/sectors` con tipo `"NO_NUMERADO"` y capacidad $5000$.
    * **Entonces** el sistema guarda el sector asociado y responde HTTP `201 Created` con el `sectorId` generado.

* **Escenario Exitoso 2:** Crear sector numerado con filas y asientos.
    * **Dado** un `venueId` existente.
    * **Cuando** se envía `POST /api/v1/venues/{venueId}/sectors` de tipo `"NUMERADO"` especificando $10$ filas y $20$ asientos por fila.
    * **Entonces** el sistema crea el sector y genera internamente la grilla de $200$ asientos (`seatId`), devolviendo HTTP `201 Created`.

* **Escenario Negativo:** Capacidad o dimensiones no válidas.
    * **Dado** una solicitud para crear un sector no numerado con capacidad $0$ o negativa.
    * **Cuando** se envía la petición al backend.
    * **Entonces** el sistema responde con HTTP `422 Unprocessable Entity` impidiendo la creación del sector.

---