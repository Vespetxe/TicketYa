## 3. Design (Diseño Técnico)

### Decisiones Técnicas y Alternativas Descartadas
* **Generación automática de asientos:** Se optó por generar los registros de `Asiento` de forma inmediata al definir un sector numerado dentro de `eventos-catalogo`, permitiendo exponer una lista estática de IDs de asientos (`seatId`).
    * *Alternativa descartada:* Generar los asientos bajo demanda al crear el evento. Se descartó para evitar duplicidad de estructuras y asegurar que el mapa del lugar sea inmutable entre distintos eventos.
* **Separación de bases de datos:** El microservicio maneja de forma exclusiva su esquema SQL. La tabla de sectores y asientos no se conecta mediante Foreign Keys con otros microservicios (`inventario-reservas`), respetando los identificadores compartidos en formato UUID/Long.

### Modelo Entidad-Relación (Relacional)
```
 +------------------+        1:N        +-------------------+
 |      Venue       |------------------<|      Sector       |
 +------------------+                   +-------------------+
 | PK venue_id      |                   | PK sector_id      |
 |    name          |                   | FK venue_id       |
 |    address       |                   |    name           |
 |    latitude      |                   |    type (ENUM)    |
 |    longitude     |                   |    capacity       |
 +------------------+                   +-------------------+
                                                  |
                                                  | 1:N (Si type == NUMERADO)
                                                  v
                                        +-------------------+
                                        |       Seat        |
                                        +-------------------+
                                        | PK seat_id        |
                                        | FK sector_id      |
                                        |    row_label      |
                                        |    seat_number    |
                                        +-------------------+
```

---