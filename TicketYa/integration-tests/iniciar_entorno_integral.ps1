param(
    [string]$JavaHome=$env:JAVA_HOME,
    [string]$MavenCommand='mvn',
    [switch]$BuildOnly,
    [switch]$Offline
)
$ErrorActionPreference='Stop'
$root=Split-Path (Split-Path $PSScriptRoot -Parent) -Parent
$out=Join-Path $PSScriptRoot 'target'
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
$buildId=[guid]::NewGuid().ToString('N')
$artifacts=@{}
foreach($project in @('EventoCatalogo_Service','inventarioReserva_service','comprapago_service','entradas_service','api-gateway')){
    $folder=Join-Path $root ('TicketYa/'+$project)
    [xml]$pom=Get-Content -Raw -LiteralPath (Join-Path $folder 'pom.xml')
    $ns=[System.Xml.XmlNamespaceManager]::new($pom.NameTable)
    $ns.AddNamespace('m','http://maven.apache.org/POM/4.0.0')
    $build=$pom.SelectSingleNode('/m:project/m:build',$ns)
    if(-not $build){$build=$pom.CreateElement('build',$ns.LookupNamespace('m'));[void]$pom.DocumentElement.AppendChild($build)}
    $buildRoot=Join-Path $folder ('target/astra-rebuild/'+$buildId)
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
$env:TICKETYA_TEST_PORT_OFFSET='10000'
$env:SPRING_PROFILES_ACTIVE='integracion,pruebas-integrales'
$env:EVENTOS_BASE_URL='http://127.0.0.1:28081'
$env:INVENTARIO_BASE_URL='http://127.0.0.1:28082'
$env:ENTRADAS_BASE_URL='http://127.0.0.1:28083'
$env:COMPRAS_BASE_URL='http://127.0.0.1:28084'
$env:TICKETYA_TEST_TOKEN=[guid]::NewGuid().ToString('N')+[guid]::NewGuid().ToString('N')
$env:DB_USERNAME='root'
if(-not $env:DB_PASSWORD){
    $privatePassword=Join-Path $out 'astra-mysql-password.txt'
    if(-not (Test-Path -LiteralPath $privatePassword)){throw 'Definir DB_PASSWORD para el MySQL desechable de 33489'}
    $env:DB_PASSWORD=Get-Content -Raw -LiteralPath $privatePassword
}
Set-Content -LiteralPath (Join-Path $out 'astra-token.txt') -Value $env:TICKETYA_TEST_TOKEN -NoNewline
$specs=@(@('inventarioReserva_service','inventario',28082),@('entradas_service','entradas',28083),@('comprapago_service','compras',28084),@('EventoCatalogo_Service','eventos',28081),@('api-gateway','gateway',28080),@('inventarioReserva_service','inventario',28085))
$records=@()
foreach($spec in $specs){
    $env:DB_URL='jdbc:mysql://127.0.0.1:33489/ticketya_e1_'+$spec[1]+'?allowPublicKeyRetrieval=true&useSSL=false'
    $folder=Join-Path $root ('TicketYa/'+$spec[0])
    $jar=$artifacts[$spec[0]]
    $args=@('-jar',('"'+$jar+'"'),('--server.port='+$spec[2]),'--server.address=127.0.0.1','--reservas.ttl-seconds=15','--reservas.expiration-interval-ms=1000')
    $proc=Start-Process -FilePath (Join-Path $JavaHome 'bin/java.exe') -ArgumentList $args -WindowStyle Hidden -PassThru -WorkingDirectory $folder -RedirectStandardOutput (Join-Path $out ('astra-'+$spec[2]+'.log')) -RedirectStandardError (Join-Path $out ('astra-'+$spec[2]+'.err.log'))
    $records+=@{service=$spec[1];port=$spec[2];pid=$proc.Id;artifact=$jar;sha256=(Get-FileHash -LiteralPath $jar -Algorithm SHA256).Hash}
    $records | ConvertTo-Json | Set-Content -LiteralPath (Join-Path $out 'astra-processes.json')
    # Primera instancia lista antes de iniciar otra sobre el mismo esquema.
    if($spec[2]-eq 28082){
        $ready=$false
        for($attempt=0;$attempt -lt 40;$attempt++){
            try{Invoke-RestMethod -Uri 'http://127.0.0.1:28082/internal/test-environment' -Headers @{'X-TicketYa-Test-Token'=$env:TICKETYA_TEST_TOKEN} -TimeoutSec 1 | Out-Null;$ready=$true;break}catch{Start-Sleep -Milliseconds 500}
        }
        if(-not $ready){throw 'Primera instancia no acreditada; revisar logs antes de continuar'}
    }
}
$records | ForEach-Object {"$($_.service) puerto=$($_.port) PID=$($_.pid)"}
