# =====================================================================
# Respaldo diario de la base de datos de Supabase (riesgo R-08)
# El plan gratuito de Supabase no incluye respaldos: este script guarda
# un pg_dump completo (esquema + datos) FUERA del repositorio.
#
# Requisitos (una sola vez):
#   1. Cliente de PostgreSQL 17 (pg_dump). Ej.: winget install PostgreSQL.PostgreSQL.17
#   2. Variable de entorno SUPABASE_DB_URL con la cadena de conexión
#      (Supabase > Project Settings > Database > Connection string > URI, modo «Session pooler»).
#      Nunca escribirla en este archivo ni subirla a GitHub.
#
# Uso:   powershell -ExecutionPolicy Bypass -File respaldar_supabase.ps1
# Restauración de prueba (en una base vacía):
#   pg_restore --no-owner --no-privileges -d "<URL_BASE_DE_PRUEBA>" <archivo.dump>
# =====================================================================
$ErrorActionPreference = 'Stop'

if (-not $env:SUPABASE_DB_URL) {
    Write-Error 'Falta la variable de entorno SUPABASE_DB_URL (ver instrucciones al inicio del script).'
}

# Carpeta de respaldos fuera del repositorio (contiene datos personales)
$destino = Join-Path $env:USERPROFILE 'Documents\Respaldos_BigBoys'
New-Item -ItemType Directory -Force -Path $destino | Out-Null

$fecha = Get-Date -Format 'yyyy-MM-dd_HHmm'
$archivo = Join-Path $destino "bigboys_$fecha.dump"

pg_dump --format=custom --no-owner --no-privileges --schema=public --file="$archivo" "$env:SUPABASE_DB_URL"
if ($LASTEXITCODE -ne 0) { Write-Error 'pg_dump terminó con error.' }

# Se conservan los últimos 30 respaldos (RNF-09: 30 días)
Get-ChildItem $destino -Filter 'bigboys_*.dump' | Sort-Object LastWriteTime -Descending |
    Select-Object -Skip 30 | Remove-Item -Force

Write-Output "Respaldo creado: $archivo ($([math]::Round((Get-Item $archivo).Length / 1KB)) KB)"
