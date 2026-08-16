[CmdletBinding()]
param(
    [ValidateSet('up', 'down', 'logs', 'build', 'test')]
    [string]$Action = 'up'
)

$ErrorActionPreference = 'Stop'
$projectRoot = Split-Path -Parent $PSScriptRoot
Set-Location $projectRoot

if ($Action -eq 'test') {
    Push-Location 'campus-trade-api'
    try {
        if (Test-Path '.\\mvnw.cmd') { .\\mvnw.cmd test } else { mvn test }
    } finally {
        Pop-Location
    }
    exit $LASTEXITCODE
}

if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
    throw 'Docker was not found. Install and start Docker Desktop, then retry.'
}

if (-not (Test-Path '.env')) {
    Copy-Item '.env.example' '.env'
    Write-Warning 'Created .env. Set passwords, JWT secret, and mail settings before running this command again.'
    exit 1
}

switch ($Action) {
    'up' { docker compose up --build -d }
    'down' { docker compose down }
    'logs' { docker compose logs --follow }
    'build' { docker compose build }
}
