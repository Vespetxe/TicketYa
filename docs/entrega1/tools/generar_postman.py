"""Construye ejemplos Postman sin realizar solicitudes HTTP."""
import json
import re
from pathlib import Path

OUT=Path(__file__).resolve().parents[1]/'postman'
OUT.mkdir(exist_ok=True)

def req(name,method,path,body=None,test='',pre='',admin=False,codes=(200,),description=''):
    headers=[]
    if body is not None:headers.append({'key':'Content-Type','value':'application/json'})
    if admin:headers.append({'key':'X-User-Role','value':'ADMIN'})
    if admin or path.startswith('/admin/'):headers.append({'key':'X-Admin-Id','value':'{{administrador_id}}'})
    request={'method':method,'header':headers,'url':'{{base_url}}'+path,'description':description or name}
    if body is not None:
        raw=json.dumps(body,ensure_ascii=False,indent=2)
        raw=re.sub(r'"\{\{(cliente_id|organizador_id|venue_id|participant_id|sector_id|evento_id|asiento_id|otro_asiento_id|reserva_id|compra_id|entrada_local_id|precio|importe|importe_devolucion)\}\}"',r'{{\1}}',raw)
        request['body']={'mode':'raw','raw':raw,'options':{'raw':{'language':'json'}}}
    code='pm.test("HTTP esperado", () => pm.expect('+json.dumps(list(codes))+').to.include(pm.response.code));\n'
    code+='if ('+json.dumps(list(codes))+'.includes(pm.response.code)) {\n'+test+'\n}'
    item={'name':name,'request':request,'event':[{'listen':'test','script':{'type':'text/javascript','exec':code.splitlines()}}]}
    if pre:item['event'].insert(0,{'listen':'prerequest','script':{'type':'text/javascript','exec':pre.splitlines()}})
    return item

def cap(field,var):return 'pm.environment.set("'+var+'", pm.response.json().'+field+');'
def assertjs(condition,label):return 'pm.test('+json.dumps(label,ensure_ascii=False)+', () => pm.expect('+condition+').to.equal(true));'

def folder(title,numerado,refund=False,cancel=False,reject=False):
    init='''const now = Date.now();
pm.environment.set('run_id', pm.variables.replaceIn('{{$guid}}'));
pm.environment.set('fecha_evento', new Date(now+8*86400000).toISOString());
pm.environment.set('desde', new Date(now+7*86400000).toISOString());
pm.environment.set('hasta', new Date(now+9*86400000).toISOString());
pm.environment.set('precio', '''+('150000' if reject else '15000')+''');'''
    a=[req('01 Crear lugar nuevo','POST','/api/v1/venues',{'name':'Sala {{run_id}}','address':'Calle Demo 123','city':'{{ciudad}}','latitude':-37.328,'longitude':-59.137},cap('venueId','venue_id'),init,codes=(201,))]
    a.append(req('02 Crear participante','POST','/api/v1/participants',{'name':'Artista {{run_id}}','role':'Banda'},cap('participantId','participant_id'),codes=(201,)))
    sector={'name':'Platea' if numerado else 'Campo','type':'NUMERADO' if numerado else 'NO_NUMERADO','capacity':2}
    if numerado:sector.update(rows=1,seatsPerRow=2)
    a.append(req('03 Crear sector','POST','/api/v1/venues/{{venue_id}}/sectors',sector,cap('sectorId','sector_id'),codes=(201,)))
    a.append(req('04 Crear evento borrador','POST','/api/v1/events',{'name':'Recital {{run_id}}','description':'Demostración Entrega 1','eventType':'Recital','organizerId':'{{organizador_id}}','venueId':'{{venue_id}}','startDateTime':'{{fecha_evento}}','participantIds':['{{participant_id}}']},cap('eventId','evento_id')+'\n'+assertjs("pm.response.json().status === 'BORRADOR'",'Estado BORRADOR'),codes=(201,)))
    a.append(req('05 Asignar precio a todos los sectores','POST','/api/v1/events/{{evento_id}}/prices',[{'sectorId':'{{sector_id}}','price':'{{precio}}'}],codes=(201,)))
    for label in ['06 Publicar e inicializar inventario','07 Repetir publicación compatible']:
        a.append(req(label,'PATCH','/api/v1/events/{{evento_id}}/publish',test=assertjs("pm.response.json().status === 'PUBLICADO'",'Estado PUBLICADO')))
    a.append(req('08 Consultar evento','GET','/api/v1/events/{{evento_id}}'))
    a.append(req('09 Buscar por ciudad tipo participante y fechas','GET','/api/v1/events/search?city={{ciudad}}&type=Recital&participantId={{participant_id}}&fromDate={{desde}}&toDate={{hasta}}&sort=RELEVANCE',test=assertjs('pm.response.json().some(e => String(e.eventId) === String(pm.environment.get("evento_id")))','Evento presente en búsqueda')))
    if numerado:
        a.append(req('10 Consultar asientos de catálogo (IDs de referencia)','GET','/api/v1/sectors/{{sector_id}}/seats',test=assertjs('pm.response.json().length === 2','Grilla de catálogo con dos asientos')))
    test=assertjs('pm.response.json().disponibles === 2','Capacidad inicial dos')
    if numerado:test+='\n'+"pm.environment.set('asiento_id', pm.response.json().asientosLibres[0].idAsiento); pm.environment.set('otro_asiento_id', pm.response.json().asientosLibres[1].idAsiento);"
    a.append(req('11 Consultar disponibilidad y capturar asiento operativo','GET','/disponibilidad/eventos/{{evento_id}}/sectores/{{sector_id}}',test=test))
    a.append(req('12 Reservar selección','POST','/reservas',{'idCliente':'{{cliente_id}}','idEvento':'{{evento_id}}','idSector':'{{sector_id}}','cantidad':2 if numerado else 1,'idsAsientos':['{{asiento_id}}','{{otro_asiento_id}}'] if numerado else None},cap('idReserva','reserva_id')+'\n'+assertjs("pm.response.json().estado === 'PENDIENTE'",'Reserva pendiente'),codes=(201,)))
    a.append(req('13 Consultar reserva','GET','/reservas/{{reserva_id}}'))
    a.append(req('14 Historial del cliente','GET','/reservas?idCliente={{cliente_id}}'))
    detalles=[{'sectorId':'{{sector_id}}','asientoId':'{{asiento_id}}' if numerado else None,'cantidad':1}]
    if numerado:detalles.append({'sectorId':'{{sector_id}}','asientoId':'{{otro_asiento_id}}','cantidad':1})
    a.append(req('15 Crear compra con selección exacta','POST','/compras',{'clienteId':'{{cliente_id}}','eventoId':'{{evento_id}}','reservaId':'{{reserva_id}}','detalles':detalles},cap('id','compra_id')+'\n'+cap('importeTotal','importe'),"pm.environment.set('clave_pago', 'pago-'+pm.variables.replaceIn('{{$guid}}'));",codes=(201,)))
    pago={'compraId':'{{compra_id}}','claveIdempotencia':'{{clave_pago}}','medioPago':'TARJETA_DEBITO'}
    for label in ['16 Procesar pago','17 Repetir pago con la misma clave']:
        a.append(req(label,'POST','/pagos',pago,assertjs("pm.response.json().estado === '"+('RECHAZADO' if reject else 'APROBADO')+"'",'Resultado financiero esperado'),codes=(201,) if label.startswith('16') else (200,)))
    compra_test=assertjs("pm.response.json().estado === '"+('FALLIDA' if reject else 'COMPLETADA')+"'",'Estado de compra esperado')
    a.append(req('18 Consultar compra y su ID local de entrada','GET','/compras/{{compra_id}}',test=compra_test))
    if not reject:
        select="const units = pm.response.json(); const chosen = units.find(e => String(e.asientoId) === String(pm.environment.get('asiento_id'))) || units[0]; pm.environment.set('entrada_local_id', chosen.entradaId); pm.environment.set('ticket_uuid', chosen.ticketId); pm.environment.set('importe_devolucion', pm.environment.get('precio'));"
        a.append(req('18b Seleccionar entrada por asiento y conocer su ID de devolución','GET','/compras/{{compra_id}}/entradas',test=select))
    tickettest=assertjs('pm.response.json().length === '+('0' if reject else '2' if numerado else '1'),'Cantidad de tickets esperada')
    a.append(req('19 Consultar tickets por orden','GET','/tickets?orderId={{compra_id}}',test=tickettest))
    a.append(req('20 Consultar pagos sin duplicación','GET','/pagos?compraId={{compra_id}}',test=assertjs('pm.response.json().length === 1','Un solo pago')))
    if refund:
        a.append(req('21 Solicitar devolución del asiento elegido','POST','/devoluciones',{'compraId':'{{compra_id}}','clienteId':'{{cliente_id}}','entradasIds':['{{entrada_local_id}}'],'motivo':'No puedo asistir'},cap('id','devolucion_id'),"pm.environment.set('clave_devolucion', 'devolucion-'+pm.variables.replaceIn('{{$guid}}'));",codes=(201,)))
        a.append(req('22 Consultar solicitudes administrativas','GET','/admin/devoluciones?estado=PENDIENTE'))
        a.append(req('23 Aprobar sin ejecutar reintegro','POST','/admin/devoluciones/{{devolucion_id}}/aprobar',{'motivo':'Revisión aprobada'},assertjs("pm.response.json().estado === 'APROBADA'",'Solicitud aprobada')))
        for label in ['24 Procesar devolución','25 Repetir devolución con misma identidad']:
            a.append(req(label,'POST','/admin/devoluciones/{{devolucion_id}}/procesar',{'claveIdempotencia':'{{clave_devolucion}}','importe':'{{importe_devolucion}}'},assertjs("pm.response.json().etapa === 'FINALIZADA'",'Procesamiento finalizado')+'\n'+assertjs('Number(pm.response.json().reintegro.importe) === Number(pm.environment.get("importe_devolucion"))','Reintegro por importe histórico')+'\n'+cap('reintegro.id','reintegro_id')))
        a.append(req('26 Consultar reintegro','GET','/admin/reintegros/{{reintegro_id}}',test=assertjs("pm.response.json().estado === 'COMPLETADO'",'Reintegro completado')))
        a.append(req('27 Consultar progreso de devolución','GET','/admin/devoluciones/{{devolucion_id}}/procesamiento'))
        a.append(req('28 Verificar asiento elegido anulado y otro vigente','GET','/tickets?orderId={{compra_id}}',test=assertjs("pm.response.json().length === 2 && pm.response.json().find(t => t.ticketId === pm.environment.get('ticket_uuid')).status === 'ANULADA' && pm.response.json().filter(t => t.status === 'EMITIDA').length === 1",'Identidad exacta del ticket anulado')))
        a.append(req('29 Verificar reposición del asiento exacto','GET','/disponibilidad/eventos/{{evento_id}}/sectores/{{sector_id}}',test=assertjs("pm.response.json().disponibles === 1 && pm.response.json().asientosLibres.length === 1 && String(pm.response.json().asientosLibres[0].idAsiento) === String(pm.environment.get('asiento_id'))",'Solo el asiento devuelto vuelve al stock')))
        a.append(req('30 Suspender evento','PATCH','/api/v1/events/{{evento_id}}/suspend',admin=True,test=assertjs("pm.response.json().status === 'SUSPENDIDO'",'Suspendido')))
        a.append(req('31 Cancelar evento suspendido','PATCH','/api/v1/events/{{evento_id}}/cancel',admin=True,test=assertjs("pm.response.json().status === 'CANCELADO'",'Cancelado')))
    if cancel:
        for label in ['21 Cancelar con venta','22 Repetir cancelación']:
            a.append(req(label,'PATCH','/api/v1/events/{{evento_id}}/cancel',admin=True,test=assertjs("pm.response.json().status === 'CANCELADO'",'Cancelado')))
        a.append(req('23 Verificar anulación','GET','/tickets?orderId={{compra_id}}',test=assertjs("pm.response.json().length === 1 && pm.response.json()[0].status === 'ANULADA'",'Ticket anulado')))
        a.append(req('24 Verificar reintegro total sin duplicación','GET','/pagos?compraId={{compra_id}}',test=assertjs('pm.response.json().length === 1 && pm.response.json()[0].estado === "REINTEGRADO_TOTAL" && Number(pm.response.json()[0].importeReintegrado) === Number(pm.environment.get("importe"))','Reintegro total una sola vez')))
        a.append(req('25 Verificar inventario bloqueado','GET','/disponibilidad/eventos/{{evento_id}}/sectores/{{sector_id}}',test=assertjs('pm.response.json().disponibles === 0','Sin reoferta de stock')))
    if reject:
        a.append(req('21 Verificar stock liberado','GET','/disponibilidad/eventos/{{evento_id}}/sectores/{{sector_id}}',test=assertjs('pm.response.json().disponibles === 2','Stock inicial restituido')))
    return {'name':title,'description':'Ejecutar completa y en orden sobre esquemas nuevos. Crea datos propios; la carpeta numerada compra dos asientos y devuelve el elegido, las otras una unidad. Claves estables en reintentos. No limpiar ni usar bases de desarrollo.','item':a}

collection={'info':{'name':'TicketYa — Entrega 1 por Gateway','schema':'https://schema.getpostman.com/json/collection/v2.1.0/collection.json','description':'Demostración pública integrada con datos dinámicos. No ejecutar en bases existentes de desarrollo. No incluye comandos internos. Datos y scripts documentales, sin ejecución automática durante su generación.'},'item':[folder('1 Compra numerada aprobada y devolución',True,refund=True),folder('2 Compra general y cancelación',False,cancel=True),folder('3 Pago rechazado y liberación',False,reject=True)]}
(OUT/'TicketYa-Entrega1.postman_collection.json').write_text(json.dumps(collection,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
values={'base_url':'http://localhost:8080','cliente_id':'1','organizador_id':'1','administrador_id':'7','ciudad':'Tandil','otro_asiento_id':'','importe_devolucion':''}
for v in ['run_id','fecha_evento','desde','hasta','precio','venue_id','participant_id','sector_id','evento_id','asiento_id','reserva_id','compra_id','importe','clave_pago','entrada_local_id','ticket_uuid','qr','devolucion_id','clave_devolucion','reintegro_id']:values[v]=''
env={'name':'TicketYa local NUEVO — ejemplo sin secretos','values':[{'key':k,'value':v,'type':'default','enabled':True} for k,v in values.items()],'_postman_variable_scope':'environment'}
(OUT/'local.postman_environment.json').write_text(json.dumps(env,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print('Coleccion y entorno generados, sin solicitudes HTTP')
