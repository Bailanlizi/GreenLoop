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
    throw '未检测到 Docker。请安装并启动 Docker Desktop 后重试。'
}

if (-not (Test-Path '.env')) {
    Copy-Item '.env.example' '.env'
    Write-Warning '已创建 .env。请先填写其中的密码、JWT 密钥和邮箱配置，再重新执行本脚本。'
    exit 1
}

switch ($Action) {
    'up' { docker compose up --build -d }
    'down' { docker compose down }
    'logs' { docker compose logs --follow }
    'build' { docker compose build }
}
