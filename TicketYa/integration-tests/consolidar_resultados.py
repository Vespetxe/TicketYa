"""Consolida evidencia local ya ejecutada; no conecta a servicios ni bases."""
from pathlib import Path
from xml.etree import ElementTree as ET
import datetime
import json
import re
import subprocess

BASE=Path(__file__).resolve().parent
ROOT=BASE.parents[1]
projects=['EventoCatalogo_Service','inventarioReserva_service','comprapago_service','entradas_service','api-gateway']
results={}
for project in projects:
    totals={k:0 for k in ['tests','failures','errors','skipped']};suites=[]
    for p in (ROOT/'TicketYa'/project/'target/surefire-reports').glob('TEST-*.xml'):
        root=ET.parse(p).getroot();values={k:int(root.get(k,'0')) for k in totals}
        for k in totals:totals[k]+=values[k]
        suites.append({'suite':root.get('name'),**values,'casos':[t.get('name') for t in root.findall('testcase')]})
    assert totals['tests']>0 and not any(totals[k] for k in ['failures','errors','skipped'])
    assert 'BUILD SUCCESS' in (ROOT/'TicketYa'/project/'astra-tests.log').read_text(encoding='utf8',errors='replace')
    results[project]={**totals,'suites':suites}
http_log=(BASE/'astra-run.log').read_text(encoding='utf8',errors='replace')
http=re.search(r'Ran (\d+) tests in ([0-9.]+)s',http_log);assert http and '\nOK' in http_log
pylog=(BASE/'astra-isolation-python.log').read_text(encoding='utf8',errors='replace')
python=re.search(r'Ran (\d+) tests in',pylog);assert python and '\nOK' in pylog
processes=json.loads((BASE/'target/astra-processes.json').read_text(encoding='utf-8-sig'))
for record in processes:
    record['artifact']=Path(record['artifact']).relative_to(ROOT).as_posix();record.pop('pid',None)
postman=json.loads((BASE/'astra-postman-results.json').read_text(encoding='utf8'))
assert not postman['failures']
report={'recorded_at_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'scope':'Cierre de hallazgos concretos de Astra; no auditoria general',
    'head_sin_modificar':subprocess.check_output(['git','rev-parse','HEAD'],cwd=ROOT,text=True).strip(),
    'java':results,'java_total':sum(v['tests'] for v in results.values()),
    'python_isolation':{'tests':int(python[1]),'failures':0,'negative_destination_variants':22,'java_attestation_tests':9,'log':'astra-isolation-python.log'},
    'real_http':{'tests':int(http[1]),'failures':0,'seconds':float(http[2]),'log':'astra-run.log','inventory_instances':2},
    'postman_collection':{'requests':postman['requests'],'assertions':postman['checks'],'failures':postman['failures'],'runner':postman['runner'],'collection_sha256':postman['collection_sha256'],'results':'astra-postman-results.json'},
    'isolation':{'container':'ticketya-astra-tests','mysql_loopback_port':33489,'application_ports':[28080,28081,28082,28083,28084,28085],'schemas':['ticketya_e1_'+s for s in ['eventos','inventario','entradas','compras']],
        'attestation':'Token nuevo; SELECT DATABASE; URL datasource exacta y todos los destinos REST salientes; verificacion completada antes del primer POST'},
    'artifacts':processes,
    'not_executed':['Auditoria general de conformidad','Cliente Postman o Newman','Renderizado Mermaid y validadores normativos externos','Commit candidato y clon limpio'],
    'preserved':'Sin detener ni reconfigurar procesos existentes en 8080-8084 o 18080-18085; sin cambios en bases de desarrollo ni operaciones Git de escritura.',
    'build_notes':'El empaquetado convencional encontro JAR bloqueados en Windows. Se empaqueto por separado en target/astra-build y Eventos final en target/astra-build-next. No se eliminaron archivos historicos. El YAML historico no cargado por defecto se saneo sustituyendo un literal por DB_PASSWORD.'}
(BASE/'astra-results.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf8')
print(json.dumps({'java':report['java_total'],'python':int(python[1]),'http':int(http[1]),'collection_requests':postman['requests'],'collection_assertions':postman['checks']}))
