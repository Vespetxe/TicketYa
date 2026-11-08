## 3. Design (Diseño Técnico)

### Decisiones Técnicas y Alternativas Descartadas
* **Uso de JPA Specifications / Criteria API:** Para la búsqueda dinâmica dentro del microservicio se optó por implementaciones mediante `Spring Data JPA Specifications` que construyen las queries SQL condicionales eficientemente a nivel de base de datos relacional.
    * *Alternativa descartada:* Filtrado en memoria dentro de Java. Descartada por alto consumo de memoria y falta de escalabilidad con catálogos grandes.

### Consultas SQL / Especificaciones de Filtro
Las búsquedas excluyen automáticamente eventos en estado `BORRADOR` o `CANCELADO`. Se realiza un `JOIN` entre la entidad `Event`, `Venue` (para el filtro por ciudad) y `EventSectorPrice` (para resolver el ordenamiento por precio base).

---