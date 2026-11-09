param([string]$MavenCommand='mvn', [switch]$Offline, [string]$RunId = [guid]::NewGuid().ToString('N'))
$ErrorActionPreference='Stop'
# Cada JVM debe tomar la configuración de src/test/resources. Una configuración
# externa podría actuar antes de las guardas Spring (p. ej. inicialización SQL).
$overrides=Get-ChildItem Env: | Where-Object Name -Match '^(SPRING_|TEST_DB_|TICKETYA_(EVENTOS|INVENTARIO|ENTRADAS|COMPRAS)_|JAVA_TOOL_OPTIONS$|JDK_JAVA_OPTIONS$|_JAVA_OPTIONS$|MAVEN_ARGS$)'
if ($overrides) { throw "Usar una terminal de pruebas sin estas variables: $($overrides.Name -join ', ')" }
if ($RunId -notmatch '^[a-f0-9]{32}$') { throw 'ID de ejecución inválido' }
$ticketYa=Split-Path $PSScriptRoot -Parent
$out=Join-Path $PSScriptRoot "target/runs/$RunId/servicios"
New-Item -ItemType Directory -Path $out | Out-Null
$env:DB_TEST_PASSWORD=[guid]::NewGuid().ToString('N')
$env:MYSQL_ROOT_PASSWORD=$env:DB_TEST_PASSWORD
$container=$null
try {
    $container=docker run --rm -d --name ("ticketya-unit-"+$RunId) -p 127.0.0.1:33079:3306 -e MYSQL_ROOT_PASSWORD -e MYSQL_DATABASE=ticketya_compras_test mysql:8.4
    if ($LASTEXITCODE -ne 0 -or $container -notmatch '^[0-9a-f]{64}$') { throw 'No se pudo crear MySQL de pruebas' }
    $ready=$false
    for ($i=0;$i -lt 90;$i++) {
        $db=docker exec $container sh -c 'MYSQL_PWD="$MYSQL_ROOT_PASSWORD" mysql --protocol=TCP -h127.0.0.1 -uroot -Dticketya_compras_test -Nse "SELECT DATABASE()"' 2>$null
        if ($LASTEXITCODE -eq 0 -and $db -eq 'ticketya_compras_test') { $ready=$true;break }
        Start-Sleep -Seconds 1
    }
    if (-not $ready) { throw 'El MySQL de pruebas no acredita su base' }
    foreach ($project in @('EventoCatalogo_Service','inventarioReserva_service','comprapago_service','entradas_service','api-gateway')) {
        $dir=Join-Path $ticketYa $project
        $argsMvn=@('-B','-f',(Join-Path $dir 'pom.xml'),'test',("-Dsurefire.reportNameSuffix="+$RunId))
        if ($Offline) { $argsMvn=@('-o')+$argsMvn }
        & $MavenCommand @argsMvn *> (Join-Path $out "$project.log")
        if ($LASTEXITCODE -ne 0) { throw "Falló $project. Consultar $out" }
        $reports=Get-ChildItem -LiteralPath (Join-Path $dir 'target/surefire-reports') -Filter "TEST-*-$RunId.xml"
        if (-not $reports) { throw "Faltan los XML de $project para esta corrida" }
        $destino=New-Item -ItemType Directory -Path (Join-Path $out $project)
        foreach ($report in $reports) { Copy-Item -LiteralPath $report.FullName -Destination $destino.FullName }
        Write-Output "$project completado"
    }
    Write-Output "Informes: $out"
} finally {
    if ($container -match '^[0-9a-f]{64}$') { docker stop $container | Out-Null }
}
