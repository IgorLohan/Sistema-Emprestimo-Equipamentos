$ErrorActionPreference = "Stop"

if (-not $env:POSTGRES_PASSWORD) {
    $env:POSTGRES_PASSWORD = "123456"
}

$psql = "C:\Program Files\PostgreSQL\18\bin\psql.exe"
if (-not (Test-Path $psql)) {
    throw "psql não encontrado em $psql"
}

$env:PGPASSWORD = $env:POSTGRES_PASSWORD
$user = if ($env:POSTGRES_USER) { $env:POSTGRES_USER } else { "postgres" }
$pgHost = if ($env:POSTGRES_HOST) { $env:POSTGRES_HOST } else { "localhost" }
$port = if ($env:POSTGRES_PORT) { $env:POSTGRES_PORT } else { "5432" }

foreach ($db in @("db_msusuario", "db_msequipamento", "db_msemprestimo")) {
    $exists = & $psql -U $user -h $pgHost -p $port -d postgres -w -tAc "SELECT 1 FROM pg_database WHERE datname='$db'"
    if ("$exists".Trim() -eq "1") {
        Write-Host "$db já existe"
    } else {
        & $psql -U $user -h $pgHost -p $port -d postgres -w -c "CREATE DATABASE $db"
        Write-Host "$db criado"
    }
}
