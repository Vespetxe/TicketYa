## 3. Design (Diseño Técnico)

### Decisiones Técnicas y Alternativas Descartadas
* **Referencias externas desnormalizadas:** `organizerId` y `participantId` se guardan como simples identificadores numéricos/UUID en el microservicio `eventos-catalogo`. No existe relación de BD relacional hacia un servicio de usuarios o participantes externos.
* **Imputación de precios:** Se crea la entidad `EventSectorPrice` para desacoplar el precio del sector físico, permitiendo que un mismo sector tenga precios distintos en eventos diferentes.

### Modelo Entidad-Relación (Relacional)
```
+--------------------+        1:N        +---------------------------+
|       Event        |------------------<|     EventSectorPrice      |
+--------------------+                   +---------------------------+
| PK event_id        |                   | PK id                     |
|    name            |                   | FK event_id               |
|    description     |                   |    sector_id (ref ext)    |
|    event_type      |                   |    price                  |
|    organizer_id    |                   +---------------------------+
|    venue_id        |
|    start_date_time |        N:M        +---------------------------+
|    status (ENUM)   |------------------<|     EventParticipant      |
+--------------------+                   +---------------------------+
                                         | FK event_id               |
                                         | FK participant_id         |
                                         +---------------------------+
```

---