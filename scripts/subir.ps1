$ErrorActionPreference = "Stop"

if (-not $env:POSTGRES_PASSWORD) {
    $env:POSTGRES_PASSWORD = "123456"
}

$root = Split-Path $PSScriptRoot -Parent

function Start-Servico([string]$pasta) {
    Start-Process powershell -WorkingDirectory (Join-Path $root $pasta) -ArgumentList "-NoExit", "-Command", ".\mvnw.cmd spring-boot:run"
}

Start-Servico "discovery-server"
Start-Sleep -Seconds 12
Start-Servico "msusuario"
Start-Servico "msequipamento"
Start-Servico "msemprestimo"
Start-Sleep -Seconds 8
Start-Servico "api-gateway"

Write-Host "Serviços iniciados em janelas separadas."
Write-Host "Eureka: http://localhost:8761"
Write-Host "Gateway: http://localhost:8080"
