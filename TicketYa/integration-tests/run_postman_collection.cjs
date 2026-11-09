// Ejecuta las APIs pm usadas por esta colección; no es el cliente Postman/Newman.
// Acredita toda la red de prueba antes de enviar cualquier solicitud de negocio.
const fs = require('node:fs');
const path = require('node:path');
const assert = require('node:assert/strict');
const {randomUUID,createHash} = require('node:crypto');

async function main() {
  const offset = Number(process.env.TICKETYA_TEST_PORT_OFFSET || 0);
  assert([0,10000].includes(offset), 'Red de prueba no permitida');
  const token = process.env.TICKETYA_TEST_TOKEN || '';
  assert(token.length >= 32, 'Falta token de prueba');
  const destinations = {
    gateway:{eventos:18081,inventario:18082,entradas:18083,compras:18084},
    eventos:{inventario:18082,entradas:18083,compras:18084},
    compras:{eventos:18081,inventario:18082,entradas:18083},entradas:{compras:18084}
  };
  for (const [service,port] of Object.entries({gateway:18080,eventos:18081,inventario:18082,entradas:18083,compras:18084,inventario2:18085})) {
    const response=await fetch(`http://127.0.0.1:${port+offset}/internal/test-environment`, {headers:{'X-TicketYa-Test-Token':token},signal:AbortSignal.timeout(5000)});
    assert.equal(response.status,200,`Acreditacion ${service}`);
    const data=await response.json(), name=service==='inventario2'?'inventario':service;
    assert.equal(data.service,name);
    if(name!=='gateway')assert.equal(data.database,'ticketya_e1_'+name);
    for(const [dest,target] of Object.entries(destinations[name]||{}))assert.equal(data[dest],`http://127.0.0.1:${target+offset}`);
  }
  const base=path.resolve(__dirname,'../../docs/entrega1/postman');
  const rawCollection=fs.readFileSync(path.join(base,'TicketYa-Entrega1.postman_collection.json'),'utf8');
  const collection=JSON.parse(rawCollection);
  const environment=JSON.parse(fs.readFileSync(path.join(base,'local.postman_environment.json'),'utf8'));
  const values=new Map(environment.values.map(v=>[v.key,v.value]));
  values.set('base_url',`http://127.0.0.1:${18080+offset}`);
  let checks=0, requests=0;
  const results=[];
  const replace = text => text.replace(/\{\{([^}]+)}}/g,(_,key)=>{
    if(key==='$guid')return randomUUID();
    assert(values.has(key),`Variable no declarada ${key}`);return String(values.get(key));
  });
  for (const folder of collection.item) {
    for (const item of folder.item) {
      let response,body;
      const pm={environment:{set:(k,v)=>values.set(k,v),get:k=>values.get(k)},variables:{replaceIn:replace},
        test:(name,fn)=>{fn();checks++;},expect:value=>({to:{equal:other=>assert.equal(value,other),include:other=>assert(value.includes(other))}})};
      for(const event of item.event||[])if(event.listen==='prerequest')new Function('pm',event.script.exec.join('\n'))(pm);
      const request=item.request;
      const headers=Object.fromEntries(request.header.filter(h=>!h.disabled).map(h=>[h.key,replace(h.value)]));
      const url=replace(request.url);
      assert(url.startsWith(values.get('base_url')+'/'),'Destino de negocio no aislado');
      response=await fetch(url,{method:request.method,headers,body:request.body?replace(request.body.raw):undefined,signal:AbortSignal.timeout(20000)});
      body=await response.text();requests++;
      pm.response={code:response.status,json:()=>JSON.parse(body)};
      try{for(const event of item.event||[])if(event.listen==='test')new Function('pm',event.script.exec.join('\n'))(pm);}
      catch(error){throw new Error(`${folder.name} / ${item.name}: HTTP ${response.status}, ${body}: ${error.message}`);}
      results.push({folder:folder.name,request:item.name,status:response.status});
    }
  }
  const report={executed_at:new Date().toISOString(),runner:'Node; API pm limitada a la usada por la coleccion, no Postman/Newman',collection_sha256:createHash('sha256').update(rawCollection).digest('hex'),gateway:values.get('base_url'),requests,checks,failures:0,results};
  fs.writeFileSync(path.join(__dirname,'astra-postman-results.json'),JSON.stringify(report,null,2)+'\n');
  console.log(`OK: ${requests} solicitudes, ${checks} aserciones; seis procesos acreditados antes del primer POST`);
}
main().catch(error=>{console.error(error);process.exitCode=1;});
