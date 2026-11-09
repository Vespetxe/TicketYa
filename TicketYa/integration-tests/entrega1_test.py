import concurrent.futures
import datetime
import json
import os
import unittest
import urllib.request
import urllib.error
import uuid
import time

from isolation import BASE, PORT_OFFSET, verify_isolation

def request(method, path, body=None, headers=None, base=BASE):
    if path.startswith('/admin/'):
        headers={'X-Admin-Id':'7', **(headers or {})}
    if (headers or {}).get('X-User-Role')=='ADMIN':
        headers={'X-Admin-Id':'7', **headers}
    req = urllib.request.Request(base + path, data=None if body is None else json.dumps(body).encode(),
                                 method=method, headers={'Content-Type': 'application/json', **(headers or {})})
    try:
        with urllib.request.urlopen(req, timeout=25) as response:
            data = response.read()
            return response.status, json.loads(data) if data else None
    except urllib.error.HTTPError as error:
        return error.code, error.read().decode()

class Entrega1RealTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        verify_isolation()

    def ok(self, method, path, body=None, headers=None):
        status, result = request(method, path, body, headers)
        self.assertIn(status, (200, 201), (method, path, status, result))
        return result

    def evento(self, numerado=False, capacidad=2, precio=15000):
        suffix = uuid.uuid4().hex[:12]
        venue = self.ok('POST', '/api/v1/venues', dict(name='Sala '+suffix, address='Calle 1', city='Tandil', latitude=1, longitude=1))
        sector_body = dict(name='Sector', type='NUMERADO' if numerado else 'NO_NUMERADO', capacity=capacidad)
        if numerado:
            sector_body.update(rows=1, seatsPerRow=capacidad)
        sector = self.ok('POST', f"/api/v1/venues/{venue['venueId']}/sectors", sector_body)
        fecha = (datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(days=8)).isoformat()
        event = self.ok('POST', '/api/v1/events', dict(name='Evento '+suffix, description='Prueba aislada', eventType='Recital', organizerId=1, venueId=venue['venueId'], startDateTime=fecha, participantIds=[]))
        eid, sid = event['eventId'], sector['sectorId']
        self.ok('POST', f'/api/v1/events/{eid}/prices', [dict(sectorId=sid, price=precio)])
        self.ok('PATCH', f'/api/v1/events/{eid}/publish')
        self.ok('PATCH', f'/api/v1/events/{eid}/publish')
        return eid, sid

    def disponible(self, eid, sid):
        return self.ok('GET', f'/disponibilidad/eventos/{eid}/sectores/{sid}')

    def compra(self, eid, sid, cantidad=1, numerada=False, client=1, key=None):
        seats = self.disponible(eid, sid)['asientosLibres'] if numerada else None
        ids = [s['idAsiento'] for s in seats[:cantidad]] if numerada else None
        reserva = self.ok('POST', '/reservas', dict(idCliente=client,idEvento=eid,idSector=sid,cantidad=cantidad,idsAsientos=ids))
        detalles = [dict(sectorId=sid,asientoId=i,cantidad=1) for i in ids] if numerada else [dict(sectorId=sid,asientoId=None,cantidad=cantidad)]
        compra = self.ok('POST','/compras',dict(clienteId=client,eventoId=eid,reservaId=reserva['idReserva'],detalles=detalles))
        pago = dict(compraId=compra['id'],claveIdempotencia=key or uuid.uuid4().hex,medioPago='TARJETA_DEBITO')
        result = self.ok('POST','/pagos',pago)
        return compra, pago, result

    def test_compra_numerada_y_reintentos(self):
        eid,sid=self.evento(True)
        compra,pago,result=self.compra(eid,sid,2,True)
        self.assertEqual('APROBADO',result['estado'])
        actual=self.ok('GET',f"/compras/{compra['id']}")
        self.assertEqual('COMPLETADA',actual['estado'])
        tickets=self.ok('GET',f"/tickets?orderId={compra['id']}")
        self.assertEqual(2,len(tickets));self.assertEqual(2,len({t['ticketId'] for t in tickets}))
        self.assertEqual(2,len({t['qr'] for t in tickets}))
        self.ok('POST','/pagos',pago)
        self.assertEqual(tickets,self.ok('GET',f"/tickets?orderId={compra['id']}"))
        self.assertEqual(0,self.disponible(eid,sid)['disponibles'])

    def test_pago_rechazado_libera(self):
        eid,sid=self.evento(precio=150000)
        compra,_,result=self.compra(eid,sid)
        self.assertEqual('RECHAZADO',result['estado'])
        self.assertEqual([],self.ok('GET',f"/tickets?orderId={compra['id']}"))
        self.assertEqual(2,self.disponible(eid,sid)['disponibles'])

    def test_timeout_pago_se_recupera_sin_duplicar(self):
        eid,sid=self.evento()
        compra,pago,result=self.compra(eid,sid,key='simular-timeout-'+uuid.uuid4().hex)
        self.assertEqual('RESULTADO_DESCONOCIDO',result['estado'])
        self.ok('POST','/pagos',pago)
        self.assertEqual('COMPLETADA',self.ok('GET',f"/compras/{compra['id']}")['estado'])
        self.assertEqual(1,len(self.ok('GET',f"/tickets?orderId={compra['id']}")))

    def test_pago_recuperado_tras_vencimiento_se_compensa(self):
        eid,sid=self.evento()
        compra,pago,result=self.compra(eid,sid,key='simular-timeout-'+uuid.uuid4().hex)
        reserva=self.ok('GET',f"/reservas/{compra['reservaId']}")
        vencimiento=datetime.datetime.fromisoformat(reserva['fechaExpiracion']).replace(tzinfo=datetime.timezone.utc)
        segundos=(vencimiento-datetime.datetime.now(datetime.timezone.utc)).total_seconds()
        self.assertLess(segundos,40)
        time.sleep(max(0,segundos)+2)
        pago_result=self.ok('POST','/pagos',pago)
        self.assertEqual('REINTEGRADO_TOTAL',pago_result['estado'])
        actual=self.ok('GET',f"/compras/{compra['id']}")
        self.assertEqual('FALLIDA',actual['estado']);self.assertEqual('FINALIZADA',actual['etapa'])
        self.ok('POST','/pagos',pago)
        self.assertEqual(2,self.disponible(eid,sid)['disponibles'])
        self.assertEqual([],self.ok('GET',f"/tickets?orderId={compra['id']}"))

    def test_devolucion_parcial_idempotente(self):
        eid,sid=self.evento(True)
        compra,_,_=self.compra(eid,sid,2,True)
        actual=self.ok('GET',f"/compras/{compra['id']}")
        units=self.ok('GET',f"/compras/{compra['id']}/entradas")
        self.assertEqual(2,len(units))
        selected=max(units,key=lambda e:e['asientoId'])
        other=next(e for e in units if e['entradaId']!=selected['entradaId'])
        before=self.ok('GET',f"/tickets?orderId={compra['id']}")
        exact=next(t for t in before if t['ticketId']==selected['ticketId'])
        self.assertEqual(str(selected['asientoId']),exact['seatId'])
        devolucion=self.ok('POST','/devoluciones',dict(compraId=compra['id'],clienteId=1,entradasIds=[selected['entradaId']],motivo='Prueba'))
        did=devolucion['id']
        self.ok('POST',f'/admin/devoluciones/{did}/aprobar',dict(motivo='Prueba aprobada'))
        body=dict(claveIdempotencia=uuid.uuid4().hex,importe=15000)
        result=self.ok('POST',f'/admin/devoluciones/{did}/procesar',body)
        self.assertEqual('FINALIZADA',result['etapa'])
        self.ok('POST',f'/admin/devoluciones/{did}/procesar',body)
        self.assertEqual(1,self.disponible(eid,sid)['disponibles'])
        tickets=self.ok('GET',f"/tickets?orderId={compra['id']}")
        self.assertEqual(1,sum(t['status']=='ANULADA' for t in tickets))
        self.assertEqual('ANULADA',next(t for t in tickets if t['ticketId']==selected['ticketId'])['status'])
        self.assertEqual('EMITIDA',next(t for t in tickets if t['ticketId']==other['ticketId'])['status'])
        free=self.disponible(eid,sid)['asientosLibres']
        self.assertEqual([selected['asientoId']],[seat['idAsiento'] for seat in free])
        pagos=self.ok('GET',f"/pagos?compraId={compra['id']}")
        self.assertEqual(15000,pagos[0]['importeReintegrado'])

    def test_cancelacion_reintegra_y_bloquea(self):
        eid,sid=self.evento()
        compra,_,_=self.compra(eid,sid)
        for _ in range(2):self.ok('PATCH',f'/api/v1/events/{eid}/cancel',headers={'X-User-Role':'ADMIN'})
        self.assertEqual(0,self.disponible(eid,sid)['disponibles'])
        status,_=request('POST','/reservas',dict(idCliente=2,idEvento=eid,idSector=sid,cantidad=1))
        self.assertEqual(409,status)
        tickets=self.ok('GET',f"/tickets?orderId={compra['id']}")
        self.assertEqual(1,len(tickets));self.assertTrue(all(t['status']=='ANULADA' for t in tickets))
        pagos=self.ok('GET',f"/pagos?compraId={compra['id']}")
        self.assertEqual(1,len(pagos));self.assertEqual('REINTEGRADO_TOTAL',pagos[0]['estado']);self.assertEqual(15000,pagos[0]['importeReintegrado'])

    def test_cancelacion_recupera_pago_incierto_con_reintentos_concurrentes(self):
        eid,sid=self.evento()
        compra,_,pago=self.compra(eid,sid,key='simular-timeout-'+uuid.uuid4().hex)
        self.assertEqual('RESULTADO_DESCONOCIDO',pago['estado'])
        def cancelar(_):return request('PATCH',f'/api/v1/events/{eid}/cancel',headers={'X-User-Role':'ADMIN'})[0]
        with concurrent.futures.ThreadPoolExecutor(max_workers=2) as pool:
            codes=list(pool.map(cancelar,range(2)))
        self.assertTrue(all(c in (200,503) for c in codes),codes)
        self.ok('PATCH',f'/api/v1/events/{eid}/cancel',headers={'X-User-Role':'ADMIN'})
        actual=self.ok('GET',f"/compras/{compra['id']}")
        self.assertEqual('FALLIDA',actual['estado']);self.assertEqual('FINALIZADA',actual['etapa'])
        pagos=self.ok('GET',f"/pagos?compraId={compra['id']}")
        self.assertEqual(1,len(pagos));self.assertEqual('REINTEGRADO_TOTAL',pagos[0]['estado']);self.assertEqual(15000,pagos[0]['importeReintegrado'])
        self.assertEqual([],self.ok('GET',f"/tickets?orderId={compra['id']}"))
        self.assertEqual(0,self.disponible(eid,sid)['disponibles'])
        base=f'http://127.0.0.1:{18083+PORT_OFFSET}'
        self.assertEqual(409,request('POST','/internal/tickets/issue',dict(orderId=str(compra['id'])),base=base)[0])

    def test_concurrencia_ultimo_cupo(self):
        eid,sid=self.evento(capacidad=1)
        def intento(client):
            base=f'http://127.0.0.1:{18085+PORT_OFFSET}' if client%2 and os.getenv('TICKETYA_SECOND_INVENTORY')=='1' else BASE
            code,reserva=request('POST','/reservas',dict(idCliente=client,idEvento=eid,idSector=sid,cantidad=1),base=base)
            if code==201:
                compra=self.ok('POST','/compras',dict(clienteId=client,eventoId=eid,reservaId=reserva['idReserva'],detalles=[dict(sectorId=sid,asientoId=None,cantidad=1)]))
                self.ok('POST','/pagos',dict(compraId=compra['id'],claveIdempotencia=uuid.uuid4().hex,medioPago='TARJETA_DEBITO'))
                self.assertEqual('COMPLETADA',self.ok('GET',f"/compras/{compra['id']}")['estado'])
            return code
        with concurrent.futures.ThreadPoolExecutor(max_workers=10) as executor:codes=list(executor.map(intento,range(100,110)))
        self.assertEqual(1,codes.count(201));self.assertEqual(9,codes.count(409))

    def test_suspension_bloquea_reservas(self):
        eid,sid=self.evento()
        self.ok('PATCH',f'/api/v1/events/{eid}/suspend',headers={'X-User-Role':'ADMIN'})
        self.assertEqual(409,request('POST','/reservas',dict(idCliente=2,idEvento=eid,idSector=sid,cantidad=1))[0])

    def test_anulacion_impide_emision_tardia(self):
        order='barrera-'+uuid.uuid4().hex
        base=f'http://127.0.0.1:{18083+PORT_OFFSET}'
        self.assertEqual(200,request('POST',f'/internal/tickets/orders/{order}/annul',base=base)[0])
        self.assertEqual(409,request('POST','/internal/tickets/issue',dict(orderId=order),base=base)[0])

    def test_reserva_vencida_no_puede_pagarse(self):
        eid,sid=self.evento()
        reserva=self.ok('POST','/reservas',dict(idCliente=1,idEvento=eid,idSector=sid,cantidad=1))
        compra=self.ok('POST','/compras',dict(clienteId=1,eventoId=eid,reservaId=reserva['idReserva'],detalles=[dict(sectorId=sid,asientoId=None,cantidad=1)]))
        vencimiento=datetime.datetime.fromisoformat(reserva['fechaExpiracion']).replace(tzinfo=datetime.timezone.utc)
        segundos=(vencimiento-datetime.datetime.now(datetime.timezone.utc)).total_seconds()
        self.assertLess(segundos,40,'La prueba requiere TTL corto configurado solo en el proceso aislado')
        time.sleep(max(0,segundos)+2)
        status,_=request('POST','/pagos',dict(compraId=compra['id'],claveIdempotencia=uuid.uuid4().hex,medioPago='TARJETA_DEBITO'))
        self.assertEqual(400,status)
        self.assertEqual(2,self.disponible(eid,sid)['disponibles'])
        self.assertEqual([],self.ok('GET',f"/tickets?orderId={compra['id']}"))

    def test_busqueda_ciudad_y_gateway_interno(self):
        eid,_=self.evento()
        values=self.ok('GET','/api/v1/events/search?city=Tandil&type=Recital&sort=RELEVANCE')
        self.assertIn(eid,[e['eventId'] for e in values])
        self.assertEqual([],self.ok('GET','/api/v1/events/search?city=NoExiste'))
        self.assertEqual(404,request('GET','/internal/orders/1')[0])

    def test_estados_relevancia_y_devolucion_de_evento_agotado(self):
        eid,sid=self.evento(capacidad=1)
        compra,_,_=self.compra(eid,sid)
        self.ok('PATCH',f'/api/v1/events/{eid}/status?status=AGOTADO',headers={'X-User-Role':'ADMIN'})
        otro,_=self.evento()
        values=self.ok('GET','/api/v1/events/search?sort=RELEVANCE')
        ids=[e['eventId'] for e in values];self.assertLess(ids.index(otro),ids.index(eid))
        actual=self.ok('GET',f"/compras/{compra['id']}")
        d=self.ok('POST','/devoluciones',dict(compraId=compra['id'],clienteId=1,entradasIds=actual['entradasIds'],motivo='Prueba agotado'))
        self.ok('POST',f"/admin/devoluciones/{d['id']}/aprobar",dict(motivo='Aprobada'))
        r=self.ok('POST',f"/admin/devoluciones/{d['id']}/procesar",dict(claveIdempotencia=uuid.uuid4().hex,importe=15000))
        self.assertEqual('COMPLETADO',r['reintegro']['estado']);self.assertEqual(15000,r['reintegro']['importe'])
        self.assertEqual(1,self.disponible(eid,sid)['disponibles'])
        self.ok('PATCH',f'/api/v1/events/{eid}/status?status=PUBLICADO',headers={'X-User-Role':'ADMIN'})
        self.ok('PATCH',f'/api/v1/events/{eid}/status?status=FINALIZADO',headers={'X-User-Role':'ADMIN'})
        self.assertEqual(409,request('POST','/reservas',dict(idCliente=2,idEvento=eid,idSector=sid,cantidad=1))[0])

    def test_publicacion_reanuda_inventario_parcial_sin_duplicar(self):
        venue=self.ok('POST','/api/v1/venues',dict(name='Parcial '+uuid.uuid4().hex,address='Calle',city='Tandil',latitude=1,longitude=1))
        sectors=[self.ok('POST',f"/api/v1/venues/{venue['venueId']}/sectors",dict(name='Sector '+str(n),type='NO_NUMERADO',capacity=2))['sectorId'] for n in range(2)]
        fecha=(datetime.datetime.now(datetime.timezone.utc)+datetime.timedelta(days=8)).isoformat()
        event=self.ok('POST','/api/v1/events',dict(name='Publicacion parcial',description='Prueba',eventType='Recital',organizerId=1,venueId=venue['venueId'],startDateTime=fecha,participantIds=[]))
        eid=event['eventId'];self.ok('POST',f'/api/v1/events/{eid}/prices',[dict(sectorId=s,price=100) for s in sectors])
        code,_=request('POST',f'/eventos/{eid}/inventario',dict(idSector=sectors[0],esNumerado=False,capacidadTotal=2,asientos=[]),base=f'http://127.0.0.1:{18082+PORT_OFFSET}')
        self.assertEqual(201,code)
        self.assertEqual('BORRADOR',self.ok('GET',f'/api/v1/events/{eid}')['status'])
        self.assertEqual(409,request('POST','/reservas',dict(idCliente=99,idEvento=eid,idSector=sectors[0],cantidad=1))[0])
        self.assertEqual(404,request('GET',f'/disponibilidad/eventos/{eid}/sectores/{sectors[1]}')[0])
        self.ok('PATCH',f'/api/v1/events/{eid}/publish');self.ok('PATCH',f'/api/v1/events/{eid}/publish')
        for sector in sectors:self.assertEqual(2,self.disponible(eid,sector)['disponibles'])

if __name__=='__main__':unittest.main(verbosity=2)
