"""Verificaciones estáticas del paquete. No llama APIs ni conecta a bases de datos.

No sustituye validadores normativos OpenAPI/Postman ni renderizador Mermaid.
"""
import datetime
import json
import re
import shutil
import subprocess
from pathlib import Path

ROOT=Path(__file__).resolve().parents[3]
BASE=ROOT/'docs/entrega1'
errors=[]
counts={}
def check(ok,message):
    if not ok:errors.append(message)
def read(p):return p.read_text(encoding='utf-8-sig')
def load(p):return json.loads(read(p))
def normalized(p):
    p=re.sub(r'\{\{[^}]+\}\}','{}',p)
    return re.sub(r'\{[^}]+\}','{}',p)

services={'eventos':'EventoCatalogo_Service','inventario':'inventarioReserva_service','compras':'comprapago_service','entradas':'entradas_service'}
contracts={s:load(BASE/'openapi'/f'{s}.json') for s in [*services,'gateway']}
for path in ['/api/v1/events/{eventId}/suspend','/api/v1/events/{eventId}/status']:
    check('404' in contracts['eventos']['paths'][path]['patch']['responses'],'Falta 404 de Eventos: '+path)
check('400' in contracts['inventario']['paths']['/ocupaciones/liberar']['post']['responses'],'Falta 400 en liberacion de ocupaciones')
methods={'get','post','put','patch','delete'}

def resolve(ref,doc):
    check(ref.startswith('#/'),'Referencia externa no verificada: '+ref)
    value=doc
    for part in ref.removeprefix('#/').split('/'):
        value=value[part.replace('~1','/').replace('~0','~')]
    return value

def walk(value,doc):
    if isinstance(value,dict):
        if '$ref' in value:
            try:resolve(value['$ref'],doc)
            except (KeyError,TypeError):errors.append('Referencia no resuelta: '+value['$ref'])
        for v in value.values():walk(v,doc)
    elif isinstance(value,list):
        for v in value:walk(v,doc)

def matches(v,s,d):
    if '$ref' in s:return matches(v,resolve(s['$ref'],d),d)
    if 'anyOf' in s:return any(matches(v,x,d) for x in s['anyOf'])
    if 'allOf' in s:return all(matches(v,x,d) for x in s['allOf'])
    if s.get('nullable') and v is None:return True
    t=s.get('type');ts=t if isinstance(t,list) else [t]
    if v is None:return 'null' in ts or t is None
    if 'enum' in s and v not in s['enum']:return False
    if t is not None:
        types={'string':lambda:isinstance(v,str),'integer':lambda:isinstance(v,int) and not isinstance(v,bool),'number':lambda:isinstance(v,(int,float)) and not isinstance(v,bool),'boolean':lambda:isinstance(v,bool),'object':lambda:isinstance(v,dict),'array':lambda:isinstance(v,list),'null':lambda:v is None}
        if not any(types.get(k,lambda:True)() for k in ts):return False
    if isinstance(v,dict):
        if any(k not in v for k in s.get('required',[])):return False
        if s.get('additionalProperties') is False and any(k not in s.get('properties',{}) for k in v):return False
        return all(matches(v[k],p,d) for k,p in s.get('properties',{}).items() if k in v)
    if isinstance(v,list):return len(v)>=s.get('minItems',0) and all(matches(x,s.get('items',{}),d) for x in v)
    if isinstance(v,str):return len(v)>=s.get('minLength',0) and len(v)<=s.get('maxLength',float('inf'))
    if isinstance(v,(int,float)):return s.get('minimum',-float('inf'))<=v<=s.get('maximum',float('inf'))
    return True

routecounts={}
for service,doc in contracts.items():
    check(doc.get('openapi','').startswith('3.1'),'Version OpenAPI: '+service)
    walk(doc,doc)
    documented=set()
    for path,ops in doc['paths'].items():
        for method,op in ops.items():
            if method not in methods:continue
            documented.add((method,normalized(path)))
            params=op.get('parameters',[])
            for v in re.findall(r'\{([^}]+)\}',path):
                check(any(p.get('in')=='path' and p.get('name')==v and p.get('required') for p in params),service+' parametro de ruta '+path+' '+v)
            check(any(str(c).startswith('2') for c in op['responses']),service+' sin exito '+path)
            media=list(op.get('requestBody',{}).get('content',{}).values())
            for response in op['responses'].values():media+=list(response.get('content',{}).values())
            for item in media:
                if 'example' in item and 'schema' in item:
                    check(matches(item['example'],item['schema'],doc),service+' ejemplo incompatible '+method+' '+path+' '+str(item['example'])[:120])
    routecounts[service]=len(documented)
    if service=='gateway':
        check(all(not p.startswith('/internal/') for p in doc['paths']),'Gateway publica internos')
        check(not any(p.endswith('/confirmar') or p.endswith('/liberar') for p in doc['paths']),'Gateway publica comandos privados de inventario')
        continue
    actual=set()
    for p in (ROOT/'TicketYa'/services[service]/'src/main/java').rglob('*.java'):
        source=read(p)
        if '@RestController' not in source or '@Profile("pruebas-integrales")' in source:continue
        classpos=source.find('public class ')
        prefix=source[:classpos]
        mapping=re.search(r'@RequestMapping\(\s*"([^"]*)"',prefix)
        base=mapping.group(1) if mapping else ''
        for m in re.finditer(r'@(Get|Post|Put|Patch|Delete)Mapping\b(?:\s*\(([^)]*)\))?',source):
            arg=m.group(2) or ''
            suffix=re.search(r'"([^"]*)"',arg)
            path=base+(suffix.group(1) if suffix else '')
            actual.add((m.group(1).lower(),normalized(path)))
    check(actual==documented,service+' diferencias rutas: faltan='+str(sorted(actual-documented))+' sobran='+str(sorted(documented-actual)))
counts['operaciones_openapi']=routecounts

collection=load(BASE/'postman/TicketYa-Entrega1.postman_collection.json')
environment=load(BASE/'postman/local.postman_environment.json')
keys={v['key'] for v in environment['values']}
requests=0
node=shutil.which('node')
scripts=0
for folder in collection['item']:
    for item in folder['item']:
        requests+=1
        request=item['request']
        text=json.dumps(item,ensure_ascii=False)
        for var in re.findall(r'\{\{([^}]+)\}\}',text):check(var.startswith('$') or var in keys,'Variable Postman no declarada: '+var)
        path=request['url'].replace('{{base_url}}','',1).split('?')[0]
        candidate=normalized(path)
        check(any(normalized(p)==candidate and request['method'].lower() in ops for p,ops in contracts['gateway']['paths'].items()),'Ruta Postman no publicada: '+path)
        gateway_op=next((ops[request['method'].lower()] for p,ops in contracts['gateway']['paths'].items() if normalized(p)==candidate and request['method'].lower() in ops),None)
        if gateway_op:
            from urllib.parse import parse_qs, urlsplit
            query=parse_qs(urlsplit(request['url']).query,keep_blank_values=True)
            header_names={h['key'].lower() for h in request.get('header',[]) if not h.get('disabled')}
            for parameter in gateway_op.get('parameters',[]):
                if parameter.get('required') and parameter.get('in')=='query':check(parameter['name'] in query,'Query obligatoria Postman omitida '+item['name']+' '+parameter['name'])
                if parameter.get('required') and parameter.get('in')=='header':check(parameter['name'].lower() in header_names,'Header obligatorio Postman omitido '+item['name']+' '+parameter['name'])
            for event in item.get('event',[]):
                if event['listen']=='test':
                    status_match=re.search(r'pm.expect\((\[[0-9, ]+\])\)', '\n'.join(event['script']['exec']))
                    if status_match:
                        for code in json.loads(status_match.group(1)):check(str(code) in gateway_op['responses'],'HTTP Postman fuera de contrato '+item['name']+' '+str(code))
        if 'body' in request:
            raw=request['body']['raw']
            expanded=re.sub(r'\{\{[^}]+\}\}','1',raw)
            try:json.loads(expanded)
            except json.JSONDecodeError:errors.append('JSON Postman invalido '+item['name'])
        for event in item.get('event',[]):
            scripts+=1
            if node:
                result=subprocess.run([node,'--check','-'],input='\n'.join(event['script']['exec']),text=True,capture_output=True)
                check(result.returncode==0,'JavaScript Postman invalido '+item['name']+result.stderr)
counts['solicitudes_postman']=requests
counts['scripts_postman_sintaxis']=scripts if node else 'no ejecutado: Node no disponible'

local_only={BASE/'cierre-astra.md',BASE/'preparacion-git.md',BASE/'manifiesto-git.json',BASE/'revision-indice.json',BASE/'credenciales-configuracion.json',BASE/'tools/preparar_manifiesto_git.py',BASE/'tools/verificar_indice.py',ROOT/'TicketYa/HELP.md',ROOT/'TicketYa/inventarioReserva_service/HELP.md'}
pages=[p for p in BASE.rglob('*.md') if p not in local_only]+[ROOT/'README.md',ROOT/'TicketYa/inventarioReserva_service/README.md',ROOT/'TicketYa/entradas_service/docs/compras-order-contract.md']
pages+=list((ROOT/'TicketYa/comprapago_service/docs').glob('*.md'))
pages+=list((ROOT/'TicketYa/comprapago_service/openspec').rglob('*.md'))
links=0
for p in pages:
    content=read(p)
    check(not re.search(r'C:[/\\]Users[/\\]',content,re.I),'Ruta personal en '+str(p.relative_to(ROOT)))
    check(content.count('```')%2==0,'Fence incompleto '+str(p))
    for target in re.findall(r'\[[^\]]*\]\(([^)]+)\)',content):
        if '://' in target or target.startswith('#'):continue
        links+=1
        path=target.split('#')[0]
        check((p.parent/path).resolve() not in local_only,'Enlace a material local excluido '+str(p.relative_to(ROOT))+' -> '+target)
        # El informe se escribe al terminar esta misma verificacion.
        check((p.parent/path).exists() or path=='verificacion-documental.json','Enlace roto '+str(p.relative_to(ROOT))+' -> '+target)
counts['enlaces_locales']=links

meta=load(BASE/'modelos/esquema-referencia.json')
for service in services:
    schema='ticketya_e1_'+service
    diagram=read(BASE/f'diagramas/er-{service}.mmd')
    for c in meta['columns']:
        if c['schema']==schema:check(re.search(r'\b'+re.escape(c['column'])+r'\b',diagram) is not None,'Columna ER omitida '+str(c))
    fks=[f for f in meta['foreign_keys'] if f['schema']==schema]
    check(sum('--' in line for line in diagram.splitlines())==len(fks),'Cantidad FK ER '+service)
    for f in fks:check(f'{f["table"]} : "{f["column"]}"' in diagram,'FK ER omitida '+str(f))
counts['tablas_er']=len({(c['schema'],c['table']) for c in meta['columns']})
counts['columnas_er']=len(meta['columns'])
counts['fk_fisicas']=len(meta['foreign_keys'])
counts['diagramas_mermaid']=len(list((BASE/'diagramas').glob('*.mmd')))
check(read(ROOT/'TicketYa/entradas_service/docs/openapi.yaml')==read(BASE/'openapi/entradas.json'),'Copia contrato Entradas divergente')
report={'fecha_utc':datetime.datetime.now(datetime.timezone.utc).isoformat(),'tipo':'verificacion estatica documental, sin auditoria final de conformidad','resultado':'OK' if not errors else 'ERRORES','conteos':counts,'errores':errors,'fuera_del_alcance_de_este_comando':['Maven, HTTP y coleccion: resultados de ejecucion separados en pruebas.md y astra-results.json','Renderizado Mermaid: no ejecutado','Validadores normativos externos de OpenAPI y Postman: no ejecutados'],'efectos':'Lectura de archivos y comprobacion de sintaxis JavaScript; solo escribe este informe documental.'}
(BASE/'verificacion-documental.json').write_text(json.dumps(report,ensure_ascii=False,indent=2)+'\n',encoding='utf-8')
print(json.dumps(report,ensure_ascii=False,indent=2))
raise SystemExit(bool(errors))
