# SEC-01/SEC-02 비교 실험: 보호 버전(backend)과 취약 대조군(vulnerable-api)을 띄우고 같은 공격을 보낸 뒤 종료한다.
# 사전 조건: PostgreSQL 컨테이너 실행 중 (README "로컬 실행" 1단계), JDK 21
# 사용: powershell -ExecutionPolicy Bypass -File experiments/api-security/run-sec-experiment.ps1 [-Trials 30] [-Warmup 3]
param(
    [int]$Trials = 30,
    [int]$Warmup = 3
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$root = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$logDir = Join-Path $PSScriptRoot 'results\raw'
New-Item -ItemType Directory -Force $logDir | Out-Null

function Stop-Port([int]$Port) {
    for ($i = 0; $i -lt 5; $i++) {
        $c = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
        if (-not $c) { return }
        Stop-Process -Id $c.OwningProcess -Force -ErrorAction SilentlyContinue
        Start-Sleep -Seconds 1
    }
}

function Wait-Ready([string]$Url, [string]$Name) {
    for ($i = 0; $i -lt 90; $i++) {
        try { Invoke-WebRequest $Url -UseBasicParsing -TimeoutSec 2 | Out-Null; Write-Host "$Name 준비됨"; return } catch { Start-Sleep -Seconds 2 }
    }
    throw "$Name 이(가) 3분 안에 준비되지 않았습니다: $Url"
}

foreach ($port in 8080, 18080) {
    if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) { throw "포트 $port 가 이미 사용 중입니다. 먼저 종료하세요." }
}

# --no-daemon: 실험이 끝나면 Gradle 프로세스도 함께 끝나게 한다 (공유 데몬이 남아 다른 프로젝트 빌드에 섞이지 않도록).
$backend = Start-Process -FilePath (Join-Path $root 'backend\gradlew.bat') -WorkingDirectory (Join-Path $root 'backend') `
    -ArgumentList '--no-daemon', '--console=plain', 'bootRun' -RedirectStandardOutput (Join-Path $logDir 'backend.log') `
    -RedirectStandardError (Join-Path $logDir 'backend.err') -PassThru -NoNewWindow
$vulnDir = Join-Path $PSScriptRoot 'vulnerable-api'
$vuln = Start-Process -FilePath (Join-Path $vulnDir 'gradlew.bat') -WorkingDirectory $vulnDir `
    -ArgumentList '--no-daemon', '--console=plain', 'bootRun' -RedirectStandardOutput (Join-Path $logDir 'vulnerable-api.log') `
    -RedirectStandardError (Join-Path $logDir 'vulnerable-api.err') -PassThru -NoNewWindow
try {
    Wait-Ready 'http://127.0.0.1:8080/actuator/health' 'backend(보호)'
    Wait-Ready 'http://127.0.0.1:18080/api/v1/auth/csrf' 'vulnerable-api(취약)'
    & java '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' (Join-Path $PSScriptRoot 'runner\ScenarioRunner.java') `
        --target 'protected=http://127.0.0.1:8080' --target 'vulnerable=http://127.0.0.1:18080' `
        --trials $Trials --warmup $Warmup --out (Join-Path $PSScriptRoot 'results')
    if ($LASTEXITCODE -ne 0) { throw "러너 실패 (exit $LASTEXITCODE)" }
}
finally {
    Stop-Port 8080
    Stop-Port 18080
    $null = $backend.WaitForExit(30000)
    $null = $vuln.WaitForExit(30000)
    Write-Host '두 앱을 종료했습니다.'
}
