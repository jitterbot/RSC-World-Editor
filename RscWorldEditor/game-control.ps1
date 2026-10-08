param([ValidateSet('Stop','StartServer','StartClient')][string]$Action)
$ErrorActionPreference='Stop'
$root=Split-Path $PSScriptRoot
$base=Join-Path $root 'openrsc-develop'
$java=Join-Path $base 'Portable_Windows\zulu8.50.0.51-ca-jdk8.0.275-win_x64\bin\java.exe'
$javaw=Join-Path (Split-Path $java) 'javaw.exe'
if($Action -eq 'Stop') {
    foreach($name in @('client','server')) {
        $file=Join-Path $root "logs\$name.pid"
        if(Test-Path $file) {
            $proc=Get-Process -Id ([int](Get-Content $file)) -ErrorAction SilentlyContinue
            if($proc -and $proc.Path -in @($java,$javaw)) {
                Stop-Process -Id $proc.Id -ErrorAction Stop
                $proc.WaitForExit(5000) | Out-Null
            }
        }
    }
    $socket=[Net.Sockets.TcpClient]::new();$stillRunning=$false
    try {$socket.Connect('127.0.0.1',43594);$stillRunning=$true} catch {} finally {$socket.Dispose()}
    if($stillRunning) {throw 'Another game server is still running. Stop it before using Save & Test.'}
    exit
}
if($Action -eq 'StartServer') {
    $p=Start-Process -FilePath $java -ArgumentList '-Xms256m -Xmx1024m -Dopenrsc.bindAddress=127.0.0.1 -DcoloredLogging=false -cp core.jar;lib/* com.openrsc.server.Server default.conf' -WorkingDirectory "$base\server" -WindowStyle Hidden -RedirectStandardOutput "$root\logs\server.log" -RedirectStandardError "$root\logs\server-errors.log" -PassThru
    $p.Id | Set-Content "$root\logs\server.pid"
    for($i=0;$i -lt 30;$i++) {
        if($p.HasExited) {throw 'Server failed to start. See logs/server-errors.log.'}
        $socket=[Net.Sockets.TcpClient]::new()
        try {$socket.Connect('127.0.0.1',43594);exit 0} catch {Start-Sleep -Milliseconds 700} finally {$socket.Dispose()}
    }
    throw 'Timed out waiting for the game server.'
}
if($Action -eq 'StartClient') {
    $p=Start-Process -FilePath $javaw -ArgumentList '-Xms256m -Xmx768m -jar Open_RSC_Client.jar' -WorkingDirectory "$base\Client_Base" -RedirectStandardOutput "$root\logs\client.log" -RedirectStandardError "$root\logs\client-errors.log" -PassThru
    $p.Id | Set-Content "$root\logs\client.pid"
}
