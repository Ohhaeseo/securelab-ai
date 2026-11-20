# SEC-01/SEC-02 비교 실험: 보호 버전(backend)과 취약 대조군(vulnerable-api)을 띄우고 같은 공격을 보낸 뒤 종료한다.
# 사전 조건: PostgreSQL 컨테이너 실행 중 (README "로컬 실행" 1단계), JDK 21
# 사용: powershell -ExecutionPolicy Bypass -File experiments/api-security/run-sec-experiment.ps1 [-Trials 30] [-Warmup 3]
param(
    [int]$Trials = 30,
    [int]$Warmup = 3,
    # 보호 버전(backend) 포트. 일반 개발 포트(8080)를 쓰는 다른 프로젝트와 겹치지 않게 전용 포트를 쓴다.
    [int]$BackendPort = 18081
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

# 응답한 서버가 SecureLab 앱인지 확인한다 (X-Request-Id 헤더 또는 vulnerable-api의 CSRF 쿠키). 다른 앱에 공격 요청을 보내지 않기 위해서다.
function Wait-Ready([string]$Url, [string]$Name, [scriptblock]$IsOurs) {
    for ($i = 0; $i -lt 90; $i++) {
        try {
            $response = Invoke-WebRequest $Url -UseBasicParsing -TimeoutSec 2
            if (-not (& $IsOurs $response)) { throw "$Name 자리의 서버가 SecureLab 앱이 아닙니다: $Url" }
            Write-Host "$Name 준비됨"; return
        }
        catch [System.Net.WebException] { Start-Sleep -Seconds 2 }
    }
    throw "$Name 이(가) 3분 안에 준비되지 않았습니다: $Url"
}

foreach ($port in $BackendPort, 18080) {
    if (Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue) { throw "포트 $port 가 이미 사용 중입니다. 먼저 종료하세요." }
}

# --no-daemon: 실험이 끝나면 Gradle 프로세스도 함께 끝나게 한다 (공유 데몬이 남아 다른 프로젝트 빌드에 섞이지 않도록).
# 익명 IP 한도만 높인다: 러너의 가상 사용자가 모두 127.0.0.1에서 가입·로그인하므로, 기본값(60/분)이면 SEC-01/02 실험이
# 요청 제한에 먼저 막힌다. SEC-03(로그인 보호)·SEC-04(로그인 사용자 한도)는 기본값으로 측정한다.
$backend = Start-Process -FilePath (Join-Path $root 'backend\gradlew.bat') -WorkingDirectory (Join-Path $root 'backend') `
    -ArgumentList '--no-daemon', '--console=plain', 'bootRun', "--args=--server.port=$BackendPort --securelab.rate-limit.anonymous-limit=100000" `
    -RedirectStandardOutput (Join-Path $logDir 'backend.log') `
    -RedirectStandardError (Join-Path $logDir 'backend.err') -PassThru -NoNewWindow
$vulnDir = Join-Path $PSScriptRoot 'vulnerable-api'
$vuln = Start-Process -FilePath (Join-Path $vulnDir 'gradlew.bat') -WorkingDirectory $vulnDir `
    -ArgumentList '--no-daemon', '--console=plain', 'bootRun' -RedirectStandardOutput (Join-Path $logDir 'vulnerable-api.log') `
    -RedirectStandardError (Join-Path $logDir 'vulnerable-api.err') -PassThru -NoNewWindow
try {
    Wait-Ready "http://127.0.0.1:$BackendPort/actuator/health" 'backend(보호)' { param($r) $r.Headers['X-Request-Id'] -like 'req-*' }
    Wait-Ready 'http://127.0.0.1:18080/api/v1/auth/csrf' 'vulnerable-api(취약)' { param($r) [string]$r.Headers['Set-Cookie'] -like '*VULNLAB-XSRF-TOKEN*' }
    & java '-Dstdout.encoding=UTF-8' '-Dstderr.encoding=UTF-8' (Join-Path $PSScriptRoot 'runner\ScenarioRunner.java') `
        --target "protected=http://127.0.0.1:$BackendPort" --target 'vulnerable=http://127.0.0.1:18080' `
        --trials $Trials --warmup $Warmup --out (Join-Path $PSScriptRoot 'results')
    if ($LASTEXITCODE -ne 0) { throw "러너 실패 (exit $LASTEXITCODE)" }
}
finally {
    Stop-Port $BackendPort
    Stop-Port 18080
    $null = $backend.WaitForExit(30000)
    $null = $vuln.WaitForExit(30000)
    Write-Host '두 앱을 종료했습니다.'
    # SEC-03이 127.0.0.1의 로그인을 15분 동안 막으므로, 실험 카운터를 지워 로컬 개발에 영향을 남기지 않는다.
    & docker compose --env-file (Join-Path $root 'infrastructure\.env') -f (Join-Path $root 'infrastructure\compose.yml') `
        exec -T redis sh -c "redis-cli --scan --pattern 'login:fail:*' | xargs -r redis-cli del; redis-cli --scan --pattern 'rl:api:*' | xargs -r redis-cli del" | Out-Null
    Write-Host 'Redis 실험 카운터(login:fail:*, rl:api:*)를 지웠습니다.'
}
