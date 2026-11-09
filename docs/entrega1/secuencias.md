# Secuencias de Entrega 1

Las flechas REST de respuesta al cliente abrevían el retorno a través del Gateway. La pasarela es un adaptador del proceso Compras. Cada persistencia usa transacción local; los diagramas no implican atomicidad distribuida.

## Preparación y habilitación comercial recuperable

```mermaid
sequenceDiagram
    actor O as Organizador
    participant G as Gateway
    participant E as Eventos
    participant I as Inventario
    O->>G: PATCH evento publish
    G->>E: Publicar BORRADOR
    loop Todos los sectores del snapshot
        E->>I: POST eventos id inventario
        I->>I: Crear PREPARADO o reutilizar configuracion compatible
        I-->>E: 201 sin habilitar reservas
    end
    Note over E,I: Fallo temporal deja BORRADOR y sectores PREPARADO#59; POST reservas devuelve 409
    E->>E: Commit PUBLICADO con control de version
    E->>I: POST internal eventos id publicar
    I->>I: Lock control_evento#59; PREPARADO a HABILITADO en todos los sectores
    I-->>E: 200 o 409 si ya suspendido/cancelado
    E-->>O: PUBLICADO via Gateway
    Note over E,I: Si falla el ultimo REST: 503#59; repetir PUBLICADO solo retoma habilitacion
    Note over E,I: Inicializacion incompatible 400/409 se comunica como 422 permanente
```

[Fuente Mermaid](diagramas/publicacion.mmd).

## Compra numerada con pago aprobado

```mermaid
sequenceDiagram
    actor C as Cliente
    participant G as Gateway
    participant E as Eventos
    participant I as Inventario
    participant P as Compras
    participant S as Pasarela simulada en Compras
    participant T as Entradas
    Note over E,I: Inventario PREPARADO no reservable#59; PUBLICADO confirmado y habilitacion comercial completada
    C->>G: GET disponibilidad
    G->>I: GET disponibilidad evento sector
    I-->>C: Asientos operativos libres via Gateway
    C->>G: POST reservas con idsAsientos operativos
    G->>I: Crear reserva
    I->>I: Bloquear stock y guardar PENDIENTE con vencimiento
    I-->>C: 201 idReserva via Gateway
    C->>G: POST compras con reserva y seleccion exacta
    G->>P: Crear compra
    P->>E: GET evento y precios
    P->>I: GET reserva
    P->>P: Validar limite y guardar PENDIENTE e importe historico
    P-->>C: 201 compra via Gateway
    C->>G: POST pagos con clave estable y medio
    G->>P: Procesar pago
    P->>P: Turno durable y PROCESANDO etapa PAGO
    P->>S: Cobrar importe con clave
    S-->>P: APROBADO
    P->>P: Guardar pago y etapa CONFIRMACION
    P->>I: POST reservas id confirmar
    I->>I: Bloqueo y PENDIENTE a CONFIRMADA
    I-->>P: 200
    P->>P: Guardar etapa EMISION
    P->>T: POST internal tickets issue con orderId
    T->>P: GET internal orders id
    P-->>T: Proyeccion CONFIRMADA sin cancelacion solicitada
    T->>T: Bloquear gate evento y lote orden#59; crear ticket UUID y QR por unidad
    T-->>P: 201 lote creado o 200 lote existente
    P->>P: COMPLETADA y FINALIZADA
    P-->>C: Resultado APROBADO via Gateway
    C->>G: GET tickets por orderId
    G->>T: Consultar tickets
    T-->>C: Entradas EMITIDA via Gateway
    Note over P,T: Repetir la misma clave no duplica cobro ni lote
```

[Fuente Mermaid](diagramas/compra-aprobada.mmd).

## Pago rechazado

```mermaid
sequenceDiagram
    actor C as Cliente
    participant G as Gateway
    participant E as Eventos
    participant I as Inventario
    participant P as Compras
    participant S as Pasarela simulada en Compras
    participant T as Entradas
    Note over C,P: Reserva PENDIENTE y compra PENDIENTE ya creadas via Gateway
    C->>G: POST pagos con misma clave en los reintentos
    G->>P: Procesar pago
    P->>I: Verificar reserva vigente
    P->>P: PROCESANDO etapa PAGO
    P->>S: Cobrar importe mayor a 100000
    S-->>P: RECHAZADO
    P->>P: Etapa LIBERACION
    P->>I: POST reservas id liberar
    I->>I: Liberar retencion una sola vez
    P->>P: FALLIDA y FINALIZADA
    P-->>C: Resultado RECHAZADO via Gateway
    C->>G: GET tickets por orderId
    G->>T: Consultar
    T-->>C: Lista vacia via Gateway
    Note over P,S: No hay reintegro de dinero porque no hubo cobro aprobado
```

[Fuente Mermaid](diagramas/pago-rechazado.mmd).

## Reserva que vence y variante con cobro incierto

```mermaid
sequenceDiagram
    actor C as Cliente
    participant G as Gateway
    participant I as Inventario
    participant P as Compras
    participant S as Pasarela simulada en Compras
    participant T as Entradas
    C->>G: POST reservas
    G->>I: Crear retencion
    I-->>C: PENDIENTE y fechaExpiracion via Gateway
    C->>G: POST compras mientras reserva vigente
    G->>P: Crear compra
    P->>I: Validar reserva
    P-->>C: Compra PENDIENTE via Gateway
    I->>I: Scheduler tras vencimiento#59; bloqueo y EXPIRADA#59; liberar stock
    C->>G: POST pagos despues del vencimiento
    G->>P: Intentar pago
    P->>I: Consultar reserva
    I-->>P: EXPIRADA
    P-->>C: 400 sin emitir via Gateway
    Note over C,T: Variante: un efecto de pago incierto se recupera despues de vencer
    C->>G: Repetir pago original incierto
    G->>P: Recuperar PROCESANDO
    P->>S: Consultar efecto por clave original
    S-->>P: Cobro APROBADO persistido
    P->>I: Confirmar reserva
    I-->>P: Conflicto por vencimiento
    P->>T: Anular lote de orden con barrera durable
    P->>S: Reintegrar cobro con clave de compensacion estable
    S-->>P: Reintegro confirmado
    P->>I: Compensar reserva sin duplicar stock
    P->>P: FALLIDA y FINALIZADA
    P-->>C: Pago REINTEGRADO_TOTAL via Gateway
```

[Fuente Mermaid](diagramas/reserva-vencida.mmd).

## Selección inequívoca y devolución parcial

```mermaid
sequenceDiagram
    actor C as Cliente
    actor A as Administrador
    participant G as Gateway
    participant P as Compras
    participant T as Entradas
    participant S as Pasarela simulada en Compras
    participant I as Inventario
    C->>G: GET compras id entradas
    G->>P: Consultar seleccion por unidad
    P->>T: GET tickets por orderId
    T-->>P: UUID, sector, asiento y estado
    P->>P: Resolver UUID a entradaId numerico persistido
    P-->>C: entradaId y ticketId con asiento via Gateway
    C->>G: POST devoluciones con entradaId del asiento elegido
    G->>P: Validar titularidad, vigencia y al menos 72 horas
    P-->>C: Solicitud PENDIENTE via Gateway
    A->>G: Aprobar con motivo y X-Admin-Id positivo
    G->>P: POST admin devoluciones id aprobar
    P-->>A: APROBADA sin movimiento financiero via Gateway
    A->>G: Procesar con clave estable e importe exacto
    G->>P: POST admin devoluciones id procesar
    P->>S: Reintegrar solo precio historico seleccionado
    S-->>P: Reintegro confirmado
    P->>T: PATCH UUID seleccionado status ANULADA
    P->>I: Registrar devolucion por identidad estable
    I->>I: Reponer solo el asiento elegido si corresponde
    P-->>A: FINALIZADA via Gateway
    Note over P,I: Repeticion no duplica reintegro ni stock#59; otros tickets siguen EMITIDA
```

[Fuente Mermaid](diagramas/devolucion-parcial.mmd).

## Cancelación con recuperación e impedimento de emisión tardía

```mermaid
sequenceDiagram
    actor A as Administrador
    participant G as Gateway
    participant E as Eventos
    participant T as Entradas
    participant I as Inventario
    participant P as Compras
    participant S as Pasarela simulada en Compras
    A->>G: PATCH evento cancel con rol ADMIN y X-Admin-Id positivo
    G->>E: Cancelar
    E->>T: POST internal events id cancel-emission
    T->>T: Gate BD espera emision previa y persiste bloqueo
    T-->>E: 200 barrera confirmada
    E->>E: Registrar CANCELADO
    E->>I: POST eventos id cancelar-inventario
    I->>I: Bloquear stock y reservas
    E->>P: POST admin eventos id cancelacion con X-Admin-Id recibido
    P->>E: Verificar CANCELADO
    P->>P: Persistir cancelacionSolicitada de compras en curso
    opt Pago PROCESANDO o incierto
        P->>S: Recuperar con clave y medio originales
        S-->>P: Efecto conocido o todavia incierto
        P->>T: Anular lote incluso si aun no existe
        P->>S: Reintegrar cobro confirmado una sola vez
        P->>I: Compensar sin reofrecer stock cancelado
    end
    opt Ventas completadas
        P->>S: Reintegrar entradas vigentes por precio historico
        P->>T: Anular entradas
    end
    alt Todas las compras resueltas
        P-->>E: 200
        E-->>A: Evento CANCELADO via Gateway
    else Turno ocupado o dependencia pendiente
        P-->>E: 202 pendientes
        E-->>A: 503 recuperable via Gateway
        Note over A,P: Repetir cancelacion#59; no crear otra identidad financiera
    end
```

[Fuente Mermaid](diagramas/cancelacion.mmd).

La variante de cobro incierto es un escenario alternativo: no continúa el intento rechazado antes de cobrar del primer bloque. Un 503 o timeout conserva efectos posibles; se consulta el resultado y se repite con las claves originales.
