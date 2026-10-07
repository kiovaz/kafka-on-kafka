# Stops whatever listens on port 8080 (the app, from the IDE or a leftover) and PROVES the port is free.
# Prints "port 8080 is free" only after checking; exits 1 otherwise.
$conns = Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue
foreach ($c in $conns) {
    Write-Host "stopping process $($c.OwningProcess) ($((Get-Process -Id $c.OwningProcess).ProcessName))"
    Stop-Process -Id $c.OwningProcess -Force
}
Start-Sleep -Seconds 2
if (Get-NetTCPConnection -LocalPort 8080 -State Listen -ErrorAction SilentlyContinue) {
    Write-Host "ERROR: port 8080 is still in use"; exit 1
}
Write-Host "port 8080 is free"
