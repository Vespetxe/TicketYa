# Guía de Pruebas de API REST - Microservicio Inventario y Reservas

**Base URL:** `http://localhost:8082`

---

## 1. Endpoints de Inventario (`InventarioController`)

### 1.1. Inicializar Inventario (Sector No Numerado - Aforo General)
* **Método:** `POST`
* **URL:** `/eventos/100/inventario`
* **Respuesta Éxito:** `201 Created`
* **Body (JSON):**
```json
{
  "idSector": 1,
  "esNumerado": false,
  "capacidadTotal": 500,
  "asientos": null
}
```

#### Solicitud de Error (HTTP 400 Bad Request): Intentar crear inventario con capacidad inválida
* **Método:** `POST`
* **URL:** `/eventos/100/inventario`
* **Body (JSON):**
```json
{
  "idSector": 1,
  "esNumerado": false,
  "capacidadTotal": -10,
  "asientos": null
}
```

#### Solicitud de Error (HTTP 400 Bad Request): Intentar inicializar un inventario duplicado
* **Método:** `POST`
* **URL:** `/eventos/100/inventario` *(Ejecutar con un idSector ya registrado)*
* **Body (JSON):**
```json
{
  "idSector": 1,
  "esNumerado": false,
  "capacidadTotal": 500,
  "asientos": null
}
```

---

### 1.2. Inicializar Inventario (Sector Numerado)
* **Método:** `POST`
* **URL:** `/eventos/100/inventario`
* **Respuesta Éxito:** `201 Created`
* **Body (JSON):**
```json
{
  "idSector": 2,
  "esNumerado": true,
  "capacidadTotal": 2,
  "asientos": [
    { "fila": "A", "numero": 1 },
    { "fila": "A", "numero": 2 }
  ]
}
```

---

### 1.3. Consultar Disponibilidad de Sector
* **Método:** `GET`
* **URL (Éxito):** `/disponibilidad/eventos/100/sectores/1`
* **Respuesta Éxito:** `200 OK`
* **Body:** *Ninguno (vacío)*

#### Solicitud de Error (HTTP 404 Not Found): Evento o sector inexistente
* **Método:** `GET`
* **URL:** `/disponibilidad/eventos/9999/sectores/9999`
* **Body:** *Ninguno (vacío)*

---

### 1.4. Liberar Lugares por Devolución
* **Método:** `POST`
* **URL:** `/ocupaciones/liberar`
* **Respuesta Éxito:** `200 OK`
* **Body (JSON):**
```json
{
  "idEvento": 100,
  "idSector": 2,
  "cantidad": 2,
  "idsAsientos": [1, 2]
}
```

#### Solicitud de Error (HTTP 404 Not Found): Asientos o inventario no encontrados
* **Método:** `POST`
* **URL:** `/ocupaciones/liberar`
* **Body (JSON):**
```json
{
  "idEvento": 100,
  "idSector": 2,
  "cantidad": 1,
  "idsAsientos": [9999]
}
```

---

### 1.5. Cancelar Inventario por Suspensión de Evento
* **Método:** `POST`
* **URL:** `/eventos/100/cancelar-inventario`
* **Respuesta Éxito:** `200 OK`
* **Body:** *Ninguno (vacío)*

---

## 2. Endpoints de Reservas (`ReservaController`)

### 2.1. Crear Reserva Temporal (TTL 10 min)
* **Método:** `POST`
* **URL:** `/reservas`
* **Respuesta Éxito:** `201 Created`
* **Body (JSON) - Sector General:**
```json
{
  "idCliente": 10,
  "idEvento": 100,
  "idSector": 1,
  "cantidad": 3,
  "idsAsientos": null
}
```

* **Body (JSON) - Sector Numerado:**
```json
{
  "idCliente": 10,
  "idEvento": 100,
  "idSector": 2,
  "cantidad": 2,
  "idsAsientos": [1, 2]
}
```

#### Solicitud de Error (HTTP 400 Bad Request): Parámetros incompletos o mal formados
* **Método:** `POST`
* **URL:** `/reservas`
* **Body (JSON):**
```json
{
  "idCliente": null,
  "idEvento": 100,
  "idSector": 1,
  "cantidad": 0,
  "idsAsientos": null
}
```

#### Solicitud de Error (HTTP 404 Not Found): Intentar reservar en inventario inexistente
* **Método:** `POST`
* **URL:** `/reservas`
* **Body (JSON):**
```json
{
  "idCliente": 10,
  "idEvento": 9999,
  "idSector": 9999,
  "cantidad": 1,
  "idsAsientos": null
}
```

#### Solicitud de Error (HTTP 409 Conflict): Capacidad excedida o asientos ocupados
* **Método:** `POST`
* **URL:** `/reservas`
* **Body (JSON):**
```json
{
  "idCliente": 10,
  "idEvento": 100,
  "idSector": 1,
  "cantidad": 9999,
  "idsAsientos": null
}
```

---

### 2.2. Consultar Reserva por ID
* **Método:** `GET`
* **URL (Éxito):** `/reservas/1`
* **Respuesta Éxito:** `200 OK`
* **Body:** *Ninguno (vacío)*

#### Solicitud de Error (HTTP 404 Not Found): Reserva inexistente
* **Método:** `GET`
* **URL:** `/reservas/9999`
* **Body:** *Ninguno (vacío)*

---

### 2.3. Consultar Historial de Reservas de un Cliente
* **Método:** `GET`
* **URL (Éxito):** `/reservas?idCliente=10`
* **Respuesta Éxito:** `200 OK`
* **Body:** *Ninguno (vacío)*

#### Solicitud de Error (HTTP 400 Bad Request): Parámetro `idCliente` faltante
* **Método:** `GET`
* **URL:** `/reservas`
* **Body:** *Ninguno (vacío)*

---

### 2.4. Confirmar Ocupación de Reserva (Pago Aprobado)
* **Método:** `POST`
* **URL (Éxito):** `/reservas/1/confirmar`
* **Respuesta Éxito:** `200 OK`
* **Body:** *Ninguno (vacío)*

#### Solicitud de Error (HTTP 404 Not Found): Reserva a confirmar inexistente
* **Método:** `POST`
* **URL:** `/reservas/9999/confirmar`
* **Body:** *Ninguno (vacío)*

#### Solicitud de Error (HTTP 409 Conflict): La reserva no se encuentra en estado PENDIENTE
* **Método:** `POST`
* **URL:** `/reservas/1/confirmar` *(Re-ejecutar sobre una reserva ya confirmada o expirada)*
* **Body:** *Ninguno (vacío)*

---

### 2.5. Liberar Reserva (Pago Rechazado o Cancelación Manual)
* **Método:** `POST`
* **URL (Éxito):** `/reservas/1/liberar`
* **Respuesta Éxito:** `200 OK`
* **Body:** *Ninguno (vacío)*

#### Solicitud de Error (HTTP 404 Not Found): Reserva a liberar inexistente
* **Método:** `POST`
* **URL:** `/reservas/9999/liberar`
* **Body:** *Ninguno (vacío)*