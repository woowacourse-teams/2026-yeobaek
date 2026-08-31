param(
    [ValidateSet('up', 'down', 'status', 'logs', 'db', 'dev', 'help')]
    [string]$Command = 'help'
)

$ErrorActionPreference = 'Stop'
$ScriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
$EnvFile = Join-Path $ScriptDir '.env.local'
$GradleWrapper = Join-Path $ScriptDir 'gradlew.bat'
$ComposeEnvArgs = if (Test-Path -LiteralPath $EnvFile) { @('--env-file', $EnvFile) } else { @() }
$PathBytes = [System.Text.Encoding]::UTF8.GetBytes([System.IO.Path]::GetFullPath($ScriptDir).ToLowerInvariant())
$Sha256 = [System.Security.Cryptography.SHA256]::Create()
try {
    $PathHash = (($Sha256.ComputeHash($PathBytes) | ForEach-Object { $_.ToString('x2') }) -join '').Substring(0, 8)
} finally {
    $Sha256.Dispose()
}
$ProjectName = if ($env:COMPOSE_PROJECT_NAME) { $env:COMPOSE_PROJECT_NAME } else { "yeobaek-local-$PathHash" }

function Invoke-Compose {
    param([Parameter(ValueFromRemainingArguments = $true)][string[]]$ComposeArgs)
    & docker compose --project-name $ProjectName @ComposeEnvArgs @ComposeArgs
    if ($LASTEXITCODE -ne 0) { throw "docker compose failed (exit code $LASTEXITCODE)." }
}

function Assert-Docker {
    if (-not (Get-Command docker -ErrorAction SilentlyContinue)) {
        throw 'Docker를 찾을 수 없습니다. Docker Desktop을 설치하고 다시 실행하세요.'
    }
    & docker compose version *> $null
    if ($LASTEXITCODE -ne 0) { throw 'Docker Compose를 사용할 수 없습니다. Docker Desktop이 실행 중인지 확인하세요.' }
    & docker info *> $null
    if ($LASTEXITCODE -ne 0) { throw 'Docker 엔진에 연결할 수 없습니다. Docker Desktop 또는 Docker Engine을 시작하세요.' }
}

function Wait-Backend {
    $url = 'http://localhost:8080/v3/api-docs'
    for ($attempt = 1; $attempt -le 60; $attempt++) {
        try {
            $response = Invoke-WebRequest -Uri $url -UseBasicParsing -TimeoutSec 3
            if ($response.StatusCode -eq 200) {
                Write-Host '백엔드 준비 완료: http://localhost:8080/swagger-ui/index.html'
                return
            }
        } catch {
            Start-Sleep -Seconds 2
        }
    }
    Invoke-Compose --profile api logs --tail 100 app
    throw '백엔드가 120초 안에 준비되지 않았습니다. 위 app 로그를 확인하세요.'
}

function Show-Help {
    @'
사용법: .\local-env.ps1 <command>
  up      사전 빌드된 백엔드 이미지와 MySQL을 실행하고 HTTP 준비를 기다림
  down    서버와 DB를 종료하고 로컬 DB 데이터도 정리
  status  컨테이너 상태 확인
  logs    서버와 DB 로그 실시간 확인
  db      IDE 실행을 위해 MySQL만 실행
  dev     MySQL을 실행하고 로컬 Gradle 서버 실행(Ctrl+C 시 MySQL 종료)
'@ | Write-Host
}

Push-Location $ScriptDir
try {
    if ($Command -eq 'help') {
        Show-Help
        return
    }
    Assert-Docker
    switch ($Command) {
        'up' {
            Invoke-Compose --profile api pull app
            Invoke-Compose --profile api up -d --wait
            Wait-Backend
        }
        'down' { Invoke-Compose --profile api down --volumes --remove-orphans }
        'status' { Invoke-Compose --profile api ps }
        'logs' { Invoke-Compose --profile api logs --follow app mysql }
        'db' {
            Invoke-Compose --profile api stop app
            Invoke-Compose up -d --wait mysql
        }
        'dev' {
            if (-not (Get-Command java -ErrorAction SilentlyContinue)) {
                throw 'Java를 찾을 수 없습니다. 백엔드 개발 모드는 JDK 21이 필요합니다.'
            }
            $CleanupDev = $true
            $GradleExitCode = 0
            try {
                Invoke-Compose --profile api stop app
                Invoke-Compose up -d --wait mysql
                $PSNativeCommandUseErrorActionPreference = $false
                & $GradleWrapper bootRun `
                    '--console=plain' '--args=--spring.profiles.active=local'
                $GradleExitCode = $LASTEXITCODE
                if ($GradleExitCode -eq 75) {
                    $CleanupDev = $false
                } elseif ($GradleExitCode -ne 0) {
                    throw "Gradle bootRun failed (exit code $GradleExitCode)."
                }
            } finally {
                if ($CleanupDev) {
                    Invoke-Compose --profile api down --volumes --remove-orphans
                }
            }
            if ($GradleExitCode -eq 75) {
                exit 75
            }
        }
    }
} finally {
    Pop-Location
}
