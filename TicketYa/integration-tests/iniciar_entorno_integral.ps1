param(
    [string]$JavaHome=$env:JAVA_HOME,
    [string]$MavenCommand='mvn',
    [switch]$BuildOnly,
    [switch]$Offline
)
$ErrorActionPreference='Stop'
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$runId=[guid]::NewGuid().ToString('N')
$out=Join-Path $PSScriptRoot ('target/runs/'+$runId)
if(-not (Test-Path (Join-Path $JavaHome 'bin/java.exe'))){throw 'Definir JAVA_HOME con JDK 21'}
# No usar JAR previos: cada invocacion compila fuentes actuales en una salida nueva.
$env:JAVA_HOME=$JavaHome
$env:PATH=(Join-Path $JavaHome 'bin')+';'+$env:PATH
Get-Command $MavenCommand -ErrorAction Stop | Out-Null
if(-not $BuildOnly){
    foreach($port in 28080..28085){
        $probe=[System.Net.Sockets.TcpClient]::new()
        try{$probe.Connect('127.0.0.1',$port);throw "Puerto de prueba ocupado: $port"}
        catch [System.Net.Sockets.SocketException]{}finally{$probe.Dispose()}
    }
}
if (-not $BuildOnly) {
    & (Join-Path $PSScriptRoot 'probar_servicios.ps1') -MavenCommand $MavenCommand -Offline:$Offline -RunId $runId
}
$buildId=$runId
$artifacts=@{}
foreach($project in @('EventoCatalogo_Service','inventarioReserva_service','comprapago_service','entradas_service','api-gateway')){
    $folder=Join-Path $root ('TicketYa/'+$project)
    [xml]$pom=Get-Content -Raw -LiteralPath (Join-Path $folder 'pom.xml')
    $ns=[System.Xml.XmlNamespaceManager]::new($pom.NameTable)
    $ns.AddNamespace('m','http://maven.apache.org/POM/4.0.0')
    $build=$pom.SelectSingleNode('/m:project/m:build',$ns)
    if(-not $build){$build=$pom.CreateElement('build',$ns.LookupNamespace('m'));[void]$pom.DocumentElement.AppendChild($build)}
    $buildRoot=Join-Path $folder ('target/integracion/'+$buildId)
    New-Item -ItemType Directory -Force -Path $buildRoot | Out-Null
    foreach($entry in @{directory=(Join-Path $buildRoot 'output');sourceDirectory=(Join-Path $folder 'src/main/java')}.GetEnumerator()){
        $node=$build.SelectSingleNode('m:'+$entry.Key,$ns)
        if(-not $node){$node=$pom.CreateElement($entry.Key,$ns.LookupNamespace('m'));[void]$build.AppendChild($node)}
        $node.InnerText=$entry.Value.Replace('\','/')
    }
    $resources=$build.SelectSingleNode('m:resources',$ns)
    if($resources){[void]$build.RemoveChild($resources)}
    $resources=$pom.CreateElement('resources',$ns.LookupNamespace('m'))
    $resource=$pom.CreateElement('resource',$ns.LookupNamespace('m'))
    $directory=$pom.CreateElement('directory',$ns.LookupNamespace('m'))
    $directory.InnerText=(Join-Path $folder 'src/main/resources').Replace('\','/')
    [void]$resource.AppendChild($directory);[void]$resources.AppendChild($resource);[void]$build.AppendChild($resources)
    $tempPom=Join-Path $buildRoot 'pom.xml';$pom.Save($tempPom)
    $mavenArgs=@('-f',$tempPom,'package','-Dmaven.test.skip=true')
    if($Offline){$mavenArgs=@('-o')+$mavenArgs}
    & $MavenCommand @mavenArgs *> (Join-Path $buildRoot 'build.log')
    if($LASTEXITCODE -ne 0){throw "Fallo compilando $project; revisar su build.log de esta invocacion"}
    $artifactId=$pom.SelectSingleNode('/m:project/m:artifactId',$ns).InnerText
    $version=$pom.SelectSingleNode('/m:project/m:version',$ns).InnerText
    $jar=Join-Path $buildRoot ('output/'+$artifactId+'-'+$version+'.jar')
    if(-not (Test-Path -LiteralPath $jar -PathType Leaf)){throw "No se genero el artefacto exacto de $project"}
    $artifacts[$project]=$jar
}
if($BuildOnly){Write-Output 'BuildOnly: cinco artefactos reconstruidos desde fuentes actuales; sin iniciar procesos ni conectar bases';return}
New-Item -ItemType Directory -Force -Path $out | Out-Null
$revision=git -C $root rev-parse HEAD
if ($LASTEXITCODE -ne 0) { throw 'No se pudo identificar la revisión de Git' }
$cambios=git -C $root status --porcelain
if ($LASTEXITCODE -ne 0) { throw 'No se pudo determinar el estado del árbol de trabajo' }
@{head=$revision; cambiosLocales=[bool]$cambios} | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $out 'origen.json')
$env:TICKETYA_TEST_PORT_OFFSET='10000'
$env:SPRING_PROFILES_ACTIVE='integracion,pruebas-integrales'
$env:EVENTOS_BASE_URL='http://127.0.0.1:28081'
$env:INVENTARIO_BASE_URL='http://127.0.0.1:28082'
$env:ENTRADAS_BASE_URL='http://127.0.0.1:28083'
$env:COMPRAS_BASE_URL='http://127.0.0.1:28084'
$env:TICKETYA_TEST_TOKEN=[guid]::NewGuid().ToString('N')+[guid]::NewGuid().ToString('N')
$env:DB_USERNAME='root'

# Crear un MySQL nuevo sin volúmenes externos. Docker rechaza un puerto ocupado.
$env:DB_PASSWORD=[guid]::NewGuid().ToString('N')
$env:MYSQL_ROOT_PASSWORD=$env:DB_PASSWORD
$container=$null
$procesos=@()
$records=@()
try {
    $container = docker run --rm -d --name ("ticketya-e1-"+$runId) -e MYSQL_ROOT_PASSWORD -p 127.0.0.1:33489:3306 mysql:8.4
    if ($LASTEXITCODE -ne 0 -or $container -notmatch '^[0-9a-f]{64}$') { throw 'No se pudo crear el MySQL exclusivo' }
    $ready=$false
    for ($attempt=0;$attempt -lt 90;$attempt++) {
        docker exec $container sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --protocol=TCP -h127.0.0.1 -uroot -Nse "SELECT 1"' 2>$null | Out-Null
        if ($LASTEXITCODE -eq 0) { $ready=$true;break }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw 'MySQL de la corrida no disponible' }
    foreach ($service in @('eventos','inventario','compras','entradas')) {
        "CREATE DATABASE ticketya_e1_$service CHARACTER SET utf8mb4;" | docker exec -i $container sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql -uroot'
        if ($LASTEXITCODE -ne 0) { throw "Error creando esquema nuevo de $service" }
    }
    $specs=@(@('inventarioReserva_service','inventario',28082),@('entradas_service','entradas',28083),@('comprapago_service','compras',28084),@('EventoCatalogo_Service','eventos',28081),@('api-gateway','gateway',28080),@('inventarioReserva_service','inventario',28085))

    foreach($spec in $specs){
        if ($spec[2] -eq 28085) {
            $previo=@('-f',(Join-Path $PSScriptRoot 'pom.xml'),'test','-Dtest=AcreditacionParcial',("-Dticketya.run="+$runId+"/previo"))
            if ($Offline) { $previo=@('-o')+$previo }
            & $MavenCommand @previo *> (Join-Path $out 'acreditacion-parcial.log')
            if ($LASTEXITCODE -ne 0) { throw "Falló el control sin la segunda JVM. Revisar $out/acreditacion-parcial.log" }
        }
        $env:DB_URL='jdbc:mysql://127.0.0.1:33489/ticketya_e1_'+$spec[1]+'?allowPublicKeyRetrieval=true&useSSL=false'
        $folder=Join-Path $root ('TicketYa/'+$spec[0])
        $jar=$artifacts[$spec[0]]
        $jvmArgs=@('-Duser.timezone=UTC','-jar',('"'+$jar+'"'),('--server.port='+$spec[2]),'--server.address=127.0.0.1','--reservas.ttl-seconds=15','--reservas.expiration-interval-ms=1000')
        $jvmArgs+=('--spring.datasource.url='+$env:DB_URL)
        $jvmArgs+='--spring.profiles.active=integracion,pruebas-integrales'
        foreach ($destino in @('eventos','inventario','entradas','compras')) {
            $url=[Environment]::GetEnvironmentVariable($destino.ToUpperInvariant()+'_BASE_URL')
            $jvmArgs+="--ticketya.$destino.url=$url"
            $jvmArgs+="--ticketya.$destino.base-url=$url"
        }
        $proc=Start-Process -FilePath (Join-Path $JavaHome 'bin/java.exe') -ArgumentList $jvmArgs -WindowStyle Hidden -PassThru -WorkingDirectory $folder -RedirectStandardOutput (Join-Path $out "$($spec[2]).log") -RedirectStandardError (Join-Path $out "$($spec[2]).err.log")
        $procesos+=$proc
        # Primera instancia lista antes de iniciar otra sobre el mismo esquema.
        $ready=$false
        for($attempt=0;$attempt -lt 180;$attempt++){
            if ($proc.HasExited) { throw "El proceso $($spec[0]) finalizó antes de acreditarse" }
            try {
                $acreditacion=Invoke-RestMethod -Uri ('http://127.0.0.1:'+$spec[2]+'/internal/test-environment') -Headers @{'X-TicketYa-Test-Token'=$env:TICKETYA_TEST_TOKEN} -TimeoutSec 1 -MaximumRedirection 0
                $ready=$true
                break
            } catch { Start-Sleep -Milliseconds 500 }
        }
        if(-not $ready){throw "Proceso no acreditado en $($spec[2])"}
        $records+=@{service=$spec[1];port=$spec[2];pid=$proc.Id;artifact=$jar;sha256=(Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash;acreditacion=$acreditacion}
        $records | ConvertTo-Json -Depth 4 | Set-Content -LiteralPath (Join-Path $out 'processes.json')
    }
    $inventarios=@($records | Where-Object service -eq 'inventario')
    if ($inventarios.Count -ne 2 -or $inventarios[0].pid -eq $inventarios[1].pid -or
        $inventarios[0].sha256 -ne $inventarios[1].sha256 -or ($procesos | Where-Object HasExited)) {
        throw 'Se requieren dos JVM independientes y activas del mismo Inventario'
    }
    $records | ForEach-Object {"$($_.service) puerto=$($_.port) PID=$($_.pid)"}
    $env:TICKETYA_RUN_ID=$runId
    $mavenArgs=@('-f',(Join-Path $PSScriptRoot 'pom.xml'),'-Pe2e','verify',("-Dticketya.run="+$runId))
    if ($Offline) { $mavenArgs=@('-o')+$mavenArgs }
    & $MavenCommand @mavenArgs *> (Join-Path $out 'verify.log')
    if ($LASTEXITCODE -ne 0) { throw "Falló Maven. Revisar $out/verify.log" }
    node (Join-Path $PSScriptRoot 'run_postman_collection.cjs')
    if ($LASTEXITCODE -ne 0) { throw 'Falló la colección Postman' }
    $resumenArgs=@('-f',(Join-Path $PSScriptRoot 'pom.xml'),'org.codehaus.mojo:exec-maven-plugin:3.5.0:java',
        '-Dexec.mainClass=ar.edu.ticketya.pruebas.InformeEjecucion',("-Dticketya.modulo="+$PSScriptRoot))
    if ($Offline) { $resumenArgs=@('-o')+$resumenArgs }
    & $MavenCommand @resumenArgs *> (Join-Path $out 'resumen.log')
    if ($LASTEXITCODE -ne 0) { throw "No se pudo consolidar la corrida. Revisar $out/resumen.log" }
    Write-Output "Informes de la corrida: $out"
} finally {
    foreach ($proc in $procesos) {
        if (-not $proc.HasExited) { $proc.Kill(); $proc.WaitForExit() }
        $proc.Dispose()
    }
    if ($container -match '^[0-9a-f]{64}$') { docker stop $container | Out-Null }
}
