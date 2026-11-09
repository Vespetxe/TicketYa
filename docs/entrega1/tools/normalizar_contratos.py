"""Normaliza copias documentales. No compila ni modifica aplicaciones o bases."""
import copy
import json
from pathlib import Path

ROOT=Path(__file__).resolve().parents[3]
OUT=ROOT/'docs/entrega1/openapi'
def ref(name): return {'$ref':'#/components/schemas/'+name}
def array(name): return {'type':'array','items':ref(name)}
def obj(props,required=()): return {'type':'object','properties':props,'required':list(required)}
string={'type':'string'}
integer={'type':'integer','format':'int64'}
uuid={'type':'string','format':'uuid'}
ticket=obj({k:string for k in ['orderId','eventId','clientId','sectorId']} | {
    'ticketId':uuid,'qr':uuid,'seatId':{'type':['string','null']},
    'status':{'type':'string','enum':['EMITIDA','ANULADA','REINTEGRADA']}},
    ['ticketId','qr','orderId','eventId','clientId','sectorId','status'])
entry={'openapi':'3.1.0','info':{'title':'TicketYa - Entradas - Entrega 1','version':'entrega-1',
 'description':'Consulta publica y coordinacion interna. Los IDs de dominio se transportan como cadenas numericas; ticketId y qr son UUID. No incluye validacion de acceso QR.'},
 'servers':[{'url':'http://localhost:8083'}],'paths':{},'components':{'schemas':{
 'Ticket':ticket,'ApiError':obj({'code':string,'message':string},['code','message']),
 'IssueTicketsRequest':obj({'orderId':dict(string,minLength=1)},['orderId']),
 'TicketIssuanceResult':obj({'tickets':array('Ticket'),'created':{'type':'boolean'}},['tickets','created']),
 'TicketStatusRequest':obj({'orderId':string,'status':{'type':'string','enum':['ANULADA','REINTEGRADA']}},['orderId','status']),
 'OrderEventStatusRequest':obj({'orderId':string,'eventId':string,'status':{'type':'string','enum':['ANULADA','REINTEGRADA']}},['orderId','eventId','status'])}}}
def operation(method,path,summary,result=None,body=None,codes=('200',)):
    o={'summary':summary,'responses':{c:{'description':{'200':'Operacion completada o repetida','201':'Lote creado','400':'Solicitud invalida','404':'Recurso inexistente','409':'Orden no habilitada o cancelada','422':'Seleccion incompatible','503':'Compras no disponible'}[c]} for c in codes}}
    if result:
        for c in codes:
            if c in ('200','201'):o['responses'][c]['content']={'application/json':{'schema':result}}
    if body:o['requestBody']={'required':True,'content':{'application/json':{'schema':ref(body)}}}
    params=[{'name':part[1:-1],'in':'path','required':True,'schema':string} for part in path.split('/') if part.startswith('{')]
    if params:o['parameters']=params
    entry['paths'].setdefault(path,{})[method]=o
operation('get','/tickets','Consultar por exactamente uno de orderId o clientId',array('Ticket'),codes=('200','400'))
entry['paths']['/tickets']['get']['parameters']=[{'name':x,'in':'query','required':False,'schema':string} for x in ['orderId','clientId']]
operation('post','/internal/tickets/issue','Emitir el lote de una orden habilitada',ref('TicketIssuanceResult'),'IssueTicketsRequest',('200','201','400','404','409','422','503'))
operation('patch','/internal/tickets/{ticketId}/status','Anular o registrar reintegro de un ticket',ref('Ticket'),'TicketStatusRequest',('200','400','404'))
operation('patch','/internal/tickets/status','Cambiar estado por orden y evento',array('Ticket'),'OrderEventStatusRequest',('200','400'))
operation('post','/internal/tickets/orders/{id}/annul','Anular orden y registrar barrera durable aunque no existan tickets')
operation('post','/internal/events/{id}/cancel-emission','Bloquear nuevas emisiones del evento; espera transacciones de emision previas')
(OUT/'entradas.json').write_text(json.dumps(entry,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

EXAMPLES={
 'EntradaSeleccionable':dict(entradaId=1,ticketId='00000000-0000-4000-8000-000000000001',sectorId=1,asientoId=1,estado='EMITIDA'),
 'VenueRequestDTO':dict(name='Sala Demo',address='Calle Demo 123',city='Tandil',latitude=-37.328,longitude=-59.137),
 'ParticipantRequestDTO':dict(name='Artista Demo',role='Banda'),
 'SectorRequestDTO':dict(name='Platea',type='NUMERADO',capacity=2,rows=1,seatsPerRow=2),
 'EventRequestDTO':dict(name='Recital Demo',description='Evento de demostracion',eventType='Recital',organizerId=1,venueId=1,startDateTime='2030-12-01T21:00:00Z',participantIds=[1]),
 'EventSectorPriceRequestDTO':dict(sectorId=1,price=15000),
 'CrearReservaRequest':dict(idCliente=1,idEvento=1,idSector=1,cantidad=1,idsAsientos=[1]),
 'InicializarInventarioRequest':dict(idSector=1,esNumerado=True,capacidadTotal=2,asientos=[dict(catalogoAsientoId=1,fila='Fila 1',numero=1),dict(catalogoAsientoId=2,fila='Fila 1',numero=2)]),
 'CrearCompraDTO':dict(clienteId=1,eventoId=1,reservaId=1,detalles=[dict(sectorId=1,asientoId=1,cantidad=1)]),
 'ProcesarPagoDTO':dict(compraId=1,claveIdempotencia='pago-demo-1',medioPago='TARJETA_DEBITO'),
 'SolicitarDevolucionDTO':dict(compraId=1,clienteId=1,entradasIds=[1],motivo='No puedo asistir'),
 'AprobarDevolucionDTO':dict(motivo='Solicitud revisada'), 'RechazarDevolucionDTO':dict(motivo='Solicitud improcedente'),
 'ProcesarDevolucionDTO':dict(claveIdempotencia='devolucion-demo-1',importe=15000),
 'DevolverLugaresRequest':dict(entradas=[dict(entradaId='00000000-0000-4000-8000-000000000001',asientoId=1)]),
 'LiberarDevolucionRequest':dict(idReserva=1,entradasIds=['00000000-0000-4000-8000-000000000001'],idEvento=1,idSector=1,cantidad=1,idsAsientos=[1]),
 'IssueTicketsRequest':dict(orderId='1'),
 'TicketStatusRequest':dict(orderId='1',status='ANULADA'),
 'OrderEventStatusRequest':dict(orderId='1',eventId='1',status='ANULADA'),
 'Ticket':dict(ticketId='00000000-0000-4000-8000-000000000001',qr='00000000-0000-4000-8000-000000000002',orderId='1',eventId='1',clientId='1',sectorId='1',seatId='1',status='EMITIDA'),
 'Order':dict(orderId='1',clientId='1',status='CONFIRMADA',items=[dict(eventId='1',sectorId='1',seatId='1',quantity=1)]),
 'MensajeResponse':dict(mensaje='Operacion completada',exitoso=True),
}
def example(schema,schemas,seen=()):
    if '$ref' in schema:
        name=schema['$ref'].rsplit('/',1)[1]
        if name in EXAMPLES:return EXAMPLES[name]
        if name in seen:return None
        return example(schemas[name],schemas,seen+(name,))
    if 'example' in schema:return schema['example']
    if 'enum' in schema:return schema['enum'][0]
    kind=schema.get('type','object');kind=next((k for k in kind if k!='null'),'string') if isinstance(kind,list) else kind
    if kind=='array':return [example(schema.get('items',{}),schemas,seen)]
    if kind=='object':return {k:example(v,schemas,seen) for k,v in schema.get('properties',{}).items()}
    if kind=='boolean':return True
    if kind in ('integer','number'):return schema.get('minimum',1)
    if schema.get('format')=='date-time':return '2030-12-01T21:00:00Z'
    if schema.get('format')=='uuid':return '00000000-0000-4000-8000-000000000001'
    return 'ejemplo'

def public(service,path,method):
    if service=='eventos':return path.startswith('/api/v1/')
    if service=='compras':return not path.startswith('/internal/')
    if service=='entradas':return path=='/tickets' and method=='get'
    return (path=='/reservas' and method in ('get','post')) or (path=='/reservas/{id}' and method=='get') or (path.startswith('/disponibilidad/') and method=='get')

documents={}
for service in ['eventos','inventario','compras','entradas']:
    d=json.loads((OUT/(service+'.json')).read_text(encoding='utf-8'))
    schemas=d.setdefault('components',{}).setdefault('schemas',{})
    nullable_fields={'compras':{'EntradaSeleccionable':['asientoId'],'DetalleCompraDTO':['asientoId'],'DetalleCompraRespuestaDTO':['asientoId']},'inventario':{'CrearReservaRequest':['idsAsientos'],'InicializarInventarioRequest':['asientos'],'DisponibilidadResponse':['asientosLibres']}}
    for name,fields in nullable_fields.get(service,{}).items():
        for field in fields:
            prop=schemas.get(name,{}).get('properties',{}).get(field)
            if prop and isinstance(prop.get('type'),str):prop['type']=[prop['type'],'null']
    if service=='compras' and 'EntradaSeleccionable' in schemas:
        schemas['EntradaSeleccionable']['description']='Una unidad: ID numerico de devolucion, UUID de Entradas y asiento operativo (null en general). Seleccionar por identidad, nunca por posicion.'
        schemas['EntradaSeleccionable']['properties']['ticketId']['format']='uuid'
    if service in ('compras','inventario'):
        for schema in schemas.values():
            for name,prop in schema.get('properties',{}).items():
                if name.startswith('fecha') and prop.get('format')=='date-time':
                    prop.pop('format',None)
                    prop['description']='Fecha y hora ISO local sin offset; los flujos usan reloj UTC.'
                    prop['example']='2030-12-01T21:00:00'
    if service=='eventos' and 'VenueResponseDTO' in schemas:
        created=schemas['VenueResponseDTO']['properties'].get('createdAt',{})
        created.pop('format',None)
        created['description']='Fecha y hora local de creacion, sin offset.'
        created['example']='2030-11-01T10:00:00'
    schemas['ErrorHTTP']=obj({'timestamp':string,'status':{'type':'integer'},'error':string,'path':string})
    if service=='compras':schemas['ErrorResponse']=obj({'status':{'type':'integer'},'message':string},['status','message']);err='ErrorResponse'
    elif service=='inventario':err='MensajeResponse'
    elif service=='entradas':err='ApiError'
    else:schemas['ErrorEvento']=obj({'timestamp':string,'status':{'type':'integer'},'message':string,'errors':{'type':'object','additionalProperties':string}},['timestamp','status']);err='ErrorEvento'
    if service=='eventos':
        for tag in d.get('tags',[]):
            if tag['name']=='Sectores - Contrato Interno':tag.update(name='Asientos de catalogo',description='Consulta de grilla de catalogo accesible por Gateway; no sustituye disponibilidad operativa.')
        d['paths']['/api/v1/sectors/{sectorId}/seats']['get']['tags']=['Asientos de catalogo']
        schemas['SeatResponseDTO']=obj({'seatId':integer,'rowLabel':string,'seatNumber':{'type':'integer'}},['seatId','rowLabel','seatNumber'])
        d['paths']['/api/v1/sectors/{sectorId}/seats']['get']['responses']['200']['content']={'application/json':{'schema':array('SeatResponseDTO')}}
        search=d['paths']['/api/v1/events/search']['get']
        search['description']='Filtros opcionales combinables: tipo, organizador, participante, ciudad exacta sin distinguir mayusculas y fechas inclusivas. Lista sin paginacion. Orden DATE_ASC predeterminado, DATE_DESC, PRICE_ASC, PRICE_DESC o RELEVANCE (PUBLICADO primero, fecha e ID).'
        search['responses']['422']={'description':'fromDate posterior a toDate'}
        for p in search.get('parameters',[]):
            if p['name']=='city':p['description']='Ciudad exacta sin distinguir mayusculas; no filtra por direccion.'
        for path in ['/api/v1/events/{eventId}/cancel','/api/v1/events/{eventId}/suspend','/api/v1/events/{eventId}/status']:
            d['paths'][path]['patch']['responses']['422']={'description':'Rol ADMIN requerido o regla/transicion de negocio invalida'}
            d['paths'][path]['patch']['responses']['404']={'description':'Evento inexistente'}
        publish=d['paths']['/api/v1/events/{eventId}/publish']['patch']
        publish['description']='Prepara sectores no reservables; confirma PUBLICADO antes de habilitar inventario comercial. Repetir PUBLICADO recupera habilitacion pendiente sin inicializar otra vez. Activacion tardia no reabre suspendido/cancelado. Inicializacion incompatible 400/409 de Inventario se traduce a 422 permanente; indisponibilidad temporal produce 503.'
    if service=='inventario':
        descriptions={
            '/eventos/{idEvento}/inventario':'Preparacion no comercial: crea sector PREPARADO y no admite reservas. 201 tanto al crear como al repetir configuracion compatible (capacidad, tipo y grilla con catalogoAsientoId). Configuracion incompatible: 400. Habilitar comercialmente es una operacion interna separada, posterior a PUBLICADO.',
            '/internal/eventos/{id}/publicar':'Habilitacion comercial posterior al commit PUBLICADO en Eventos. Transicion PREPARADO a HABILITADO en una transaccion para todos los sectores. Repetir es idempotente. DESHABILITADO o CANCELADO no se reabren por esta operacion (409).',
            '/reservas/{id}/confirmar':'Confirma una reserva PENDIENTE vigente con inventario HABILITADO. Repetir CONFIRMADA es 200 mientras el inventario siga habilitado. Vencida, liberada o inventario bloqueado: 409.',
            '/reservas/{id}/liberar':'Libera PENDIENTE. Repetir LIBERADA/EXPIRADA no repone stock nuevamente; EXPIRADA pasa a LIBERADA. CONFIRMADA requiere compensacion/devolucion: 409.',
            '/internal/reservas/{id}/compensar':'Compensa retencion o confirmacion sin devoluciones parciales previas. Repeticion sin efectos adicionales; inventario CANCELADO no recupera stock ofrecido.',
            '/ocupaciones/liberar':'Liberacion con reserva e identidad estable por entrada; no basta evento/sector/cantidad. Se ejecuta despues del reintegro y anulacion, no al aprobar la solicitud.',
            '/eventos/{idEvento}/cancelar-inventario':'Cancela stock del evento, marca asientos cancelados y no reofrece lugares; no representa una suspension reversible.',
            '/internal/reservas/{id}/devoluciones':'Diario de identidades externas devueltas por reserva. POST requiere reserva CONFIRMADA, seleccion propia y coherente; repetir misma identidad no duplica stock.',
        }
        for p,description in descriptions.items():
            for o in d['paths'][p].values():o['description']=description
    if service=='compras':
        returns={('/admin/devoluciones','get'):array('DevolucionDTO'),('/devoluciones/{id}','get'):ref('DevolucionDTO'),('/admin/reintegros/{id}','get'):ref('ReintegroDTO'),('/admin/devoluciones/{id}/procesamiento','get'):ref('ResultadoDevolucionDTO'),('/admin/devoluciones/{id}/procesar','post'):ref('ResultadoDevolucionDTO'),('/admin/devoluciones/{id}/aprobar','post'):ref('DevolucionDTO'),('/admin/devoluciones/{id}/rechazar','post'):ref('DevolucionDTO')}
        for (p,m),s in returns.items():d['paths'][p][m]['responses']['200']={'description':'Resultado de la operacion','content':{'application/json':{'schema':s}}}
        cancel=d['paths']['/admin/eventos/{id}/cancelacion']['post']
        cancel['summary']='Recuperar cancelacion de evento confirmada por Eventos'
        cancel['description']='El cliente real comprueba CANCELADO en Eventos, recupera pagos en procesamiento con su clave original y reintegra ventas. 202 conserva compras pendientes si una dependencia o turno impide finalizar; repetir. Preferir PATCH /api/v1/events/{eventId}/cancel para iniciar el flujo completo.'
        for code in ['200','202']:cancel['responses'][code]['content']={'application/json':{'schema':ref('CancelacionEventoDTO')}}
    for path,methods in d['paths'].items():
        for method,o in methods.items():
            if method not in ('get','post','put','patch','delete'):continue
            o['x-visibility']='publica' if public(service,path,method) else 'interna'
            if path.startswith('/admin/'):o['x-visibility']='administrativa'
            o['operationId']=service+'_'+method+'_'+path.strip('/').replace('/','_').replace('{','').replace('}','').replace('-','_')
            o.setdefault('summary',method.upper()+' '+path)
            o.setdefault('description','Operacion implementada por '+service+'.')
            if service=='compras' and path.startswith('/admin/'):
                params=o.setdefault('parameters',[])
                if not any(p.get('name')=='X-Admin-Id' for p in params):params.append({'name':'X-Admin-Id','in':'header','required':True,'schema':dict(integer,minimum=1),'example':7,'description':'Identidad administrativa declarada en Entrega 1; sin autenticacion JWT.'})
                o['description']=o['description'].replace('Esta entrega no implementa seguridad.','Identificacion administrativa declarada; sin JWT.')
            for suffix in [' Accesible por el Gateway.',' Llamada directa entre servicios; excluida del Gateway normal.']:
                o['description']=o['description'].replace(suffix,'')
            o['description']+=' '+('Accesible por el Gateway.' if public(service,path,method) else 'Llamada directa entre servicios; excluida del Gateway normal.')
            responses=o.setdefault('responses',{})
            if service=='eventos':
                if method in ('post','patch','put','delete'):responses.setdefault('422',{'description':'BusinessRuleException: regla de negocio o transicion invalida'})
                if method=='patch':responses.setdefault('503',{'description':'Coordinacion pendiente; consultar estado y repetir la misma operacion'})
                if path.endswith('/publish') or path.endswith('/cancel'):responses.pop('409',None)
                responses['400']={'description':'Formato JSON, enum o parametro invalido; reglas de negocio usan 422'}
            if service=='inventario':
                for c,msg in [('400','Datos invalidos'),('404','Recurso inexistente'),('409','Estado o disponibilidad incompatible')]:responses.setdefault(c,{'description':msg})
                allowed=({'200','500'} if path=='/reservas' else {'200','404','500'}) if method=='get' else {'200','201','400','404','409','500'}
                if method=='post' and ((path.startswith('/reservas/') and (path.endswith('/confirmar') or path.endswith('/liberar'))) or path.endswith('/compensar')):allowed.discard('400')
                if method=='post' and (path.startswith('/eventos/') or path.startswith('/internal/eventos/')):
                    allowed.discard('404')
                    if not path.endswith('/inventario'):allowed.discard('400')
                for c in list(responses):
                    if c not in allowed:responses.pop(c)
                if path.endswith('/confirmar') and method=='post':responses['409']={'description':'Reserva vencida/liberada o inventario bloqueado; confirmar de nuevo una confirmada con inventario habilitado es 200'}
                if path.endswith('/inventario') and method=='post':responses['400']={'description':'Datos invalidos o inicializacion incompatible; una repeticion compatible devuelve 201'}
            if service=='compras':
                for c,msg in [('400','Solicitud invalida'),('404','Recurso inexistente'),('409','Conflicto de estado, identidad o turno')]:responses.setdefault(c,{'description':msg})
                responses.setdefault('406',{'description':'Accept no admitido; error devuelto como JSON'})
                if o.get('requestBody'):responses.setdefault('415',{'description':'Content-Type no admitido'})
                if path=='/compras/{id}/entradas':responses.setdefault('503',{'description':'Entradas inaccesible o respuesta incompatible'})
            responses.setdefault('500',{'description':'Error interno; no interpretar como ausencia de efectos'})
            for code,response in responses.items():
                if code.isdigit() and int(code)>=400:
                    entry_code={'400':'INVALID_REQUEST','404':'ORDER_NOT_FOUND','409':'ORDER_NOT_CONFIRMED','422':'INVALID_ORDER','503':'ORDERS_UNAVAILABLE'}.get(code,'INTERNAL_ERROR')
                    if service=='entradas' and code=='400' and path=='/tickets':entry_code='INVALID_TICKET_QUERY'
                    if service=='entradas' and code=='404' and 'ticketId' in path:entry_code='TICKET_NOT_FOUND'
                    response['content']={'application/json':{'schema':{'anyOf':[ref(err),ref('ErrorHTTP')]},'example':({'status':int(code),'message':'Operacion no completada'} if service=='compras' else {'mensaje':'Operacion no completada','exitoso':False} if service=='inventario' else {'code':entry_code,'message':'Operacion no completada'} if service=='entradas' and code!='500' else {'timestamp':'2030-12-01T21:00:00','status':int(code),'message':'Operacion no completada'})}}
            for content in [o.get('requestBody',{}).get('content',{})]+[r.get('content',{}) for c,r in responses.items() if c.startswith('2')]:
                for media in content.values():
                    if 'schema' in media:media['example']=example(media['schema'],schemas)
            for param in o.get('parameters',[]):
                if param.get('in')=='path':param['example']='00000000-0000-4000-8000-000000000001' if param['name']=='ticketId' else '1' if service=='entradas' else 1
    (OUT/(service+'.json')).write_text(json.dumps(d,ensure_ascii=False,indent=2)+'\n',encoding='utf-8');documents[service]=d

gateway={'openapi':'3.1.0','info':{'title':'TicketYa - API publica del Gateway','version':'entrega-1','description':'Rutas efectivamente publicadas por GatewayApplication. Sin JWT en Entrega 1. Los errores de negocio se conservan; un proveedor inaccesible produce 503. No publica /internal/** ni mutaciones directas de inventario.'},'servers':[{'url':'http://localhost:8080'}],'paths':{},'components':{'schemas':{}}}
def prefix_refs(value,service):
    if isinstance(value,dict):return {k:('#/components/schemas/'+service+'_'+v.rsplit('/',1)[1] if k=='$ref' and isinstance(v,str) and v.startswith('#/components/schemas/') else prefix_refs(v,service)) for k,v in value.items()}
    if isinstance(value,list):return [prefix_refs(v,service) for v in value]
    return value
for service,d in documents.items():
    for name,s in d['components']['schemas'].items():gateway['components']['schemas'][service+'_'+name]=prefix_refs(s,service)
    for path,methods in d['paths'].items():
        for method,o in methods.items():
            if not public(service,path,method):continue
            op=prefix_refs(copy.deepcopy(o),service)
            op['responses']['503']={'description':'Proveedor no disponible o recuperacion pendiente; no generar otra identidad de operacion','content':{'application/json':{'schema':{'type':'object','additionalProperties':True},'example':{'status':503,'message':'Servicio no disponible'}}}}
            gateway['paths'].setdefault(path,{})[method]=op
(OUT/'gateway.json').write_text(json.dumps(gateway,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')

lines=['# Inventario de endpoints de Entrega 1','','Contrato publico: [gateway.json](gateway.json). Los contratos por servicio incluyen coordinacion interna.','','| Servicio | Metodo | Ruta | Visibilidad | Respuestas |','|---|---|---|---|---|']
for service,d in documents.items():
    for path,methods in sorted(d['paths'].items()):
        for method,o in methods.items():lines.append(f"| {service} | {method.upper()} | `{path}` | {o['x-visibility']} | {', '.join(sorted(o['responses']))} |")
lines += ['', 'Los ejemplos de IDs son ilustrativos; para ejecutar, importar la coleccion Postman que captura identidades reales.', 'Los codigos 500 y los errores de transporte no autorizan a repetir con una nueva clave de pago/reintegro.', 'El endpoint de acreditacion `/internal/test-environment` existe solo en el perfil `pruebas-integrales`; no integra la API de negocio normal.', 'El Gateway tambien devuelve 404 para rutas sin coincidencia. Los contratos no uniforman artificialmente los formatos de error de los servicios.','']
lines += ['Los esquemas de Entradas usan alias documentales: `Ticket` corresponde a `TicketResponse`, `TicketStatusRequest` a `TicketStatusUpdateRequest` y `OrderEventStatusRequest` a `EventTicketsStatusUpdateRequest`; sus propiedades JSON son las del codigo. El archivo del servicio `docs/openapi.yaml` contiene esta misma especificacion en sintaxis JSON, que tambien es YAML 1.2 valida.','', 'Las fechas de reserva/pago/devolucion son LocalDateTime sin offset; el inicio del evento es OffsetDateTime. Los ejemplos de solicitud no son seeds ni autorizan a usar bases de desarrollo.','']
(OUT/'README.md').write_text('\n'.join(lines),encoding='utf-8')
print('Contratos documentales normalizados: cuatro servicios y Gateway')
