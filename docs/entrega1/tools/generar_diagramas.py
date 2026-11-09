"""Genera únicamente diagramas y páginas documentales desde metadatos guardados."""
import json
import re
from collections import defaultdict
from pathlib import Path

BASE = Path(__file__).resolve().parents[1]
OUT = BASE / 'diagramas'
OUT.mkdir(exist_ok=True)
diagrams = {
'arquitectura': '''flowchart LR
    C[Cliente o administrador] -->|REST JSON| G[API Gateway 8080]
    G -->|REST| E[Eventos y Catalogo 8081]
    G -->|REST| I[Inventario y Reservas 8082]
    G -->|REST| P[Compras y Pagos 8084]
    G -->|GET tickets| T[Entradas 8083]
    E -->|Preparar habilitar y bloquear stock| I
    E -->|Barrera de emision| T
    E -->|Coordinar cancelacion| P
    P -->|Consultar evento y precios| E
    P -->|Verificar confirmar compensar| I
    P -->|Emitir y anular| T
    T -->|Consultar orden habilitada| P
    E --- ED[(BD Eventos)]
    I --- ID[(BD Inventario)]
    T --- TD[(BD Entradas)]
    subgraph CP[Proceso Compras]
        P --> PS[Adaptador pasarela simulada]
        PS --- PD[(BD Compras y diario de efectos)]
        P --- PD
    end
''',
'compra-aprobada': '''sequenceDiagram
    actor C as Cliente
    participant G as Gateway
    participant E as Eventos
    participant I as Inventario
    participant P as Compras
    participant S as Pasarela simulada en Compras
    participant T as Entradas
    Note over E,I: Inventario PREPARADO no reservable; PUBLICADO confirmado y habilitacion comercial completada
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
    T->>T: Bloquear gate evento y lote orden; crear ticket UUID y QR por unidad
    T-->>P: 201 lote creado o 200 lote existente
    P->>P: COMPLETADA y FINALIZADA
    P-->>C: Resultado APROBADO via Gateway
    C->>G: GET tickets por orderId
    G->>T: Consultar tickets
    T-->>C: Entradas EMITIDA via Gateway
    Note over P,T: Repetir la misma clave no duplica cobro ni lote
''',
'pago-rechazado': '''sequenceDiagram
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
''',
'reserva-vencida': '''sequenceDiagram
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
    I->>I: Scheduler tras vencimiento; bloqueo y EXPIRADA; liberar stock
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
''',
'cancelacion': '''sequenceDiagram
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
        Note over A,P: Repetir cancelacion; no crear otra identidad financiera
    end
''',
'publicacion': '''sequenceDiagram
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
    Note over E,I: Fallo temporal deja BORRADOR y sectores PREPARADO; POST reservas devuelve 409
    E->>E: Commit PUBLICADO con control de version
    E->>I: POST internal eventos id publicar
    I->>I: Lock control_evento; PREPARADO a HABILITADO en todos los sectores
    I-->>E: 200 o 409 si ya suspendido/cancelado
    E-->>O: PUBLICADO via Gateway
    Note over E,I: Si falla el ultimo REST: 503; repetir PUBLICADO solo retoma habilitacion
    Note over E,I: Inicializacion incompatible 400/409 se comunica como 422 permanente
''',
'devolucion-parcial': '''sequenceDiagram
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
    Note over P,I: Repeticion no duplica reintegro ni stock; otros tickets siguen EMITIDA
'''
}
for name, source in diagrams.items():
    (OUT/(name+'.mmd')).write_text(source,encoding='utf-8')
page=BASE/'arquitectura.md'
s=page.read_text(encoding='utf-8')
s=s.replace('<!-- DIAGRAMA:arquitectura.mmd -->','```mermaid\n'+diagrams['arquitectura']+'```\n\n[Fuente Mermaid](diagramas/arquitectura.mmd).')
s=re.sub(r'```mermaid\n.*?```',lambda _: '```mermaid\n'+diagrams['arquitectura']+'```',s,count=1,flags=re.S)
page.write_text(s,encoding='utf-8')
text=['# Secuencias de Entrega 1','','Las flechas REST de respuesta al cliente abrevían el retorno a través del Gateway. La pasarela es un adaptador del proceso Compras. Cada persistencia usa transacción local; los diagramas no implican atomicidad distribuida.','']
for name,title in [('publicacion','Preparación y habilitación comercial recuperable'),('compra-aprobada','Compra numerada con pago aprobado'),('pago-rechazado','Pago rechazado'),('reserva-vencida','Reserva que vence y variante con cobro incierto'),('devolucion-parcial','Selección inequívoca y devolución parcial'),('cancelacion','Cancelación con recuperación e impedimento de emisión tardía')]:
    text += ['## '+title,'','```mermaid',diagrams[name].rstrip(),'```','',f'[Fuente Mermaid](diagramas/{name}.mmd).','']
text += ['La variante de cobro incierto es un escenario alternativo: no continúa el intento rechazado antes de cobrar del primer bloque. Un 503 o timeout conserva efectos posibles; se consulta el resultado y se repite con las claves originales.','']
(BASE/'secuencias.md').write_text('\n'.join(text),encoding='utf-8')

data=json.loads((BASE/'modelos/esquema-referencia.json').read_text(encoding='utf-8'))
text=['# Modelo relacional por microservicio','','Los diagramas representan tablas y claves físicas del mapeo JPA actual. Los metadatos se consultaron en modo lectura en el MySQL aislado de la verificación previa; [snapshot](modelos/esquema-referencia.json). Cada esquema es propiedad de un servicio. No hay FK entre bases. `PK`, `FK` y `UK` indican claves; un UK compuesto se detalla como conjunto debajo y no como unicidad individual. Los tipos enum se simplifican a `enum` para Mermaid; su dominio literal permanece en el snapshot.','']
for service in ['eventos','inventario','compras','entradas']:
    schema='ticketya_e1_'+service
    cols=[c for c in data['columns'] if c['schema']==schema]
    fks=[f for f in data['foreign_keys'] if f['schema']==schema]
    indexes=defaultdict(list)
    for u in data['unique_indexes']:
        if u['schema']==schema:indexes[(u['table'],u['index'])].append((int(u['position']),u['column']))
    unique={(t,sorted(v)[0][1]) for (t,i),v in indexes.items() if i!='PRIMARY' and len(v)==1}
    lines=['erDiagram']
    for table in sorted({c['table'] for c in cols}):
        lines.append('    '+table+' {')
        for c in [x for x in cols if x['table']==table]:
            keys=[]
            if c['key']=='PRI':keys.append('PK')
            if any(f['table']==table and f['column']==c['column'] for f in fks):keys.append('FK')
            if (table,c['column']) in unique:keys.append('UK')
            typ=c['type'].split('(')[0].replace(' ','_')
            lines.append('        '+typ+' '+c['column']+(' '+','.join(keys) if keys else '')+' "'+('nullable' if c['nullable']=='YES' else 'required')+'"')
        lines.append('    }')
    for f in fks:
        nullable=next(c['nullable']=='YES' for c in cols if c['table']==f['table'] and c['column']==f['column'])
        parent='|o' if nullable else '||'
        child='o|' if (f['table'],f['column']) in unique else 'o{'
        lines.append(f'    {f["target_table"]} {parent}--{child} {f["table"]} : "{f["column"]}"')
    source='\n'.join(lines)+'\n'
    (OUT/('er-'+service+'.mmd')).write_text(source,encoding='utf-8')
    text+=['## '+service.capitalize(),'','```mermaid',source.rstrip(),'```','',f'[Fuente Mermaid](diagramas/er-{service}.mmd).','','Restricciones únicas compuestas (además de las claves individuales del gráfico):','']
    comp=[f'- `{t}`: `{", ".join(c for _,c in sorted(v))}` ({"PK" if i=="PRIMARY" else "UK"}).' for (t,i),v in indexes.items() if len(v)>1]
    text += comp or ['- No hay índices únicos compuestos en el snapshot.']
    text+=['']
text += ['## Identidades y relaciones lógicas','','| Referencia | Correspondencia real |','|---|---|','| Evento y lugar | `event.venue_id` referencia lógica a `venue.venue_id`; no FK física, aun dentro de la misma base. |','| Precio y sector | `event_sector_price.sector_id` es referencia lógica; solo su relación con event tiene FK. |','| Asiento catálogo e inventario | `seat.seat_id` viaja como `catalogoAsientoId` al inicializar. Inventario asigna otro `asiento.id_asiento`. Reservar y comprar usa ese ID operativo; fila/número ayudan a presentarlo. |','| Compra y reserva | `compras.reserva_id` único apunta a Inventario por REST; no FK entre esquemas. |','| Compra y entrada | `entrada_externa.id` numérico local corresponde a UUID externo único. `CompraDTO.entradasIds` contiene los numéricos; `Ticket.ticketId` contiene UUID. |','| Colección de entradas | `compra_entradas.compra_id` tiene FK; `entrada_id` es valor numérico único de la colección, sin FK física a entrada_externa. |','| Ticket y orden | `ticket_batches.order_id` es string con ID de compra; ticket referencia físicamente a su lote. Los IDs de evento/cliente/sector/asiento son strings externos. |','| Gate de emisión | Identidad del evento externa a Entradas, sin FK a Eventos. |','| Lugar devuelto | Registro idempotente por entrada externa; referencias a reserva son internas, asiento_id es escalar. |','','## Invariantes adicionales','','- `control_limite_entradas` serializa el cupo por cliente/evento; pagos y reintegros guardan claves y efectos; `operaciones_simuladas` es el diario de la pasarela simulada.','- Precios e importes son decimales; no se calcula el reintegro con precios actuales del catálogo.','- El modelo de devolución separa solicitud, detalles por entrada y reintegro; aprobar no ejecuta el movimiento financiero.','- Bloqueos, transiciones y validaciones de selección complementan las restricciones SQL. Un DER no expresa por sí solo la prohibición de emitir después de cancelar.','- No se presupone igualdad numérica entre asientos de distintos servicios ni correspondencia posicional entre listas de entradas. GET /compras/{id}/entradas expone por unidad entradaId, ticketId UUID, sector, asiento y estado para seleccionar exactamente la devolucion.','']
text += ['## Entidades con información solapada','','`seat` representa la grilla del lugar y `asiento` la disponibilidad operativa. `entrada_externa` proyecta el UUID y datos del ticket para asociarlo con detalles y devoluciones locales. Los detalles de compra conservan precios históricos del catálogo. No hay un segundo propietario del catálogo ni relaciones SQL entre servicios: se coordinan por REST.','']
(BASE/'modelo-datos.md').write_text('\n'.join(text),encoding='utf-8')
print(f'Generados {len(diagrams)+4} diagramas Mermaid y modelos/secuencias documentales')
