import json
import os
import urllib.request

PORT_OFFSET = int(os.environ.get('TICKETYA_TEST_PORT_OFFSET', '0'))
if PORT_OFFSET not in (0, 10000):
    raise RuntimeError('Red de prueba no permitida')
BASE = f'http://127.0.0.1:{18080+PORT_OFFSET}'
DESTINATIONS = {
    'gateway': {'eventos': 18081, 'inventario': 18082, 'entradas': 18083, 'compras': 18084},
    'eventos': {'inventario': 18082, 'entradas': 18083, 'compras': 18084},
    'compras': {'eventos': 18081, 'inventario': 18082, 'entradas': 18083},
    'entradas': {'compras': 18084},
}
DESTINATIONS = {service: {name: port+PORT_OFFSET for name,port in destinations.items()} for service,destinations in DESTINATIONS.items()}

def verify_isolation():
    token=os.environ.get('TICKETYA_TEST_TOKEN','')
    if len(token)<32:
        raise RuntimeError('Se requiere el token aleatorio del entorno aislado')
    for service,port in [('gateway',18080),('eventos',18081),('inventario',18082),('entradas',18083),('compras',18084)]:
        req=urllib.request.Request(f'http://127.0.0.1:{port+PORT_OFFSET}/internal/test-environment',headers={'X-TicketYa-Test-Token':token})
        with urllib.request.urlopen(req,timeout=5) as response:
            data=json.load(response)
        if data.get('service')!=service or (service!='gateway' and data.get('database')!='ticketya_e1_'+service):
            raise RuntimeError('El proceso no acredita su base temporal: '+service)
        for destination, port in DESTINATIONS.get(service, {}).items():
            if data.get(destination) != f'http://127.0.0.1:{port}':
                raise RuntimeError('El proceso no acredita su destino REST: '+service+' -> '+destination)
    if os.environ.get('TICKETYA_SECOND_INVENTORY')=='1':
        req=urllib.request.Request(f'http://127.0.0.1:{18085+PORT_OFFSET}/internal/test-environment',headers={'X-TicketYa-Test-Token':token})
        with urllib.request.urlopen(req,timeout=5) as response:data=json.load(response)
        if data!={'service':'inventario','database':'ticketya_e1_inventario'}:raise RuntimeError('Segunda instancia no aislada')
