# 8주차 부하 측정: 요청 제한 OFF / ON 각각 backend를 띄우고 k6 혼합 트래픽(정상 + 공격)을 보낸 뒤 비교표를 만든다.
# 사전 조건: PostgreSQL·Redis 컨테이너 실행 중, k6 설치(winget GrafanaLabs.k6), JDK 21
# 사용: powershell -ExecutionPolicy Bypass -File experiments/load-tests/run-k6.ps1 [-Duration 90s] [-Port 18091]
param(
    [string]$Duration = '90s',
    # 일반 개발 포트(8080)를 쓰는 다른 프로젝트와 겹치지 않게 전용 포트를 쓴다.
    [int]$Port = 18091
)

$ErrorActionPreference = 'Stop'
[Console]::OutputEncoding = [Text.Encoding]::UTF8
$root = Resolve-Path (Join-Path $PSScriptRoot '..\..')
$resultsDir = Join-Path $PSScriptRoot 'results'
$rawDir = Join-Path $resultsDir 'raw'
New-Item -ItemType Directory -Force $rawDir | Out-Null
$base = "http://127.0.0.1:$Port"
$compose = @('compose', '--env-file', (Join-Path $root 'infrastructure\.env'), '-f', (Join-Path $root 'infrastructure\compose.yml'))
$k6 = (Get-Command k6 -ErrorAction SilentlyContinue).Source
if (-not $k6) { $k6 = @("$env:ProgramFiles\k6\k6.exe", "$env:LOCALAPPDATA\Microsoft\WinGet\Links\k6.exe") | Where-Object { Test-Path $_ } | Select-Object -First 1 }
if (-not $k6) { throw 'k6를 찾을 수 없습니다 (winget install GrafanaLabs.k6).' }
$stamp = Get-Date -Format 'yyyyMMdd-HHmmss'

function Clear-RateLimitCounters {
    & docker @compose exec -T redis sh -c "redis-cli --scan --pattern 'rl:api:*' | xargs -r redis-cli del; redis-cli --scan --pattern 'login:fail:*' | xargs -r redis-cli del" | Out-Null
}

function Invoke-Run([string]$Mode) {
    if (Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue) { throw "포트 $Port 이(가) 이미 사용 중입니다." }
    Clear-RateLimitCounters
    $enabled = if ($Mode -eq 'on') { 'true' } else { 'false' }
    $env:SPRING_PROFILES_ACTIVE = 'local,lab'
    $backend = Start-Process -FilePath (Join-Path $root 'backend\gradlew.bat') -WorkingDirectory (Join-Path $root 'backend') `
        -ArgumentList '--no-daemon', '--console=plain', 'bootRun', "--args=`"--server.port=$Port --securelab.rate-limit.enabled=$enabled`"" `
        -RedirectStandardOutput (Join-Path $rawDir "backend-$Mode.log") -RedirectStandardError (Join-Path $rawDir "backend-$Mode.err") -PassThru -NoNewWindow
    Remove-Item Env:SPRING_PROFILES_ACTIVE
    try {
        # 응답한 서버가 SecureLab backend인지 확인한다 (X-Request-Id 헤더). 다른 앱이면 중단.
        $ready = $false
        for ($i = 0; $i -lt 90 -and -not $ready; $i++) {
            try {
                $health = Invoke-WebRequest "$base/actuator/health" -UseBasicParsing -TimeoutSec 2
                if (-not ($health.Headers['X-Request-Id'] -like 'req-*')) { throw "포트 $Port 의 서버가 SecureLab backend가 아닙니다." }
                $ready = $true
            }
            catch [System.Net.WebException] { Start-Sleep -Seconds 2 }
        }
        if (-not $ready) { throw 'backend가 3분 안에 준비되지 않았습니다.' }
        # 워밍업: JIT·커넥션 풀을 데운 뒤 측정 (익명 요청 몇 건)
        1..20 | ForEach-Object { Invoke-WebRequest "$base/actuator/health" -UseBasicParsing | Out-Null }
        Write-Host "[$Mode] k6 실행 ($Duration)"
        $summary = Join-Path $rawDir "k6-$Mode-$stamp.json"
        & $k6 run --quiet -e "BASE_URL=$base" -e "DURATION=$Duration" -e "SUMMARY_JSON=$summary" (Join-Path $PSScriptRoot 'scripts\mixed-traffic.js')
        return (Get-Content $summary -Raw -Encoding UTF8 | ConvertFrom-Json)
    }
    finally {
        for ($k = 0; $k -lt 5; $k++) {
            $c = Get-NetTCPConnection -LocalPort $Port -State Listen -ErrorAction SilentlyContinue | Select-Object -First 1
            if (-not $c) { break }
            Stop-Process -Id $c.OwningProcess -Force -ErrorAction SilentlyContinue; Start-Sleep -Seconds 1
        }
        $null = $backend.WaitForExit(30000)
        Clear-RateLimitCounters
    }
}

$off = Invoke-Run 'off'
$on = Invoke-Run 'on'

function Get-Metric($summary, [string]$name, [string]$stat) { [double]$summary.metrics.$name.values.$stat }
$md = New-Object Text.StringBuilder
[void]$md.AppendLine("# 요청 제한 부하 측정 — k6 혼합 트래픽 ($stamp)").AppendLine()
[void]$md.AppendLine("- 지속 시간 $Duration, 정상 사용자 10명(합계 초당 5건, 1인당 분당 약 30건), 공격자 2명(합계 초당 10건, 1인당 분당 약 300건)")
[void]$md.AppendLine('- 요청 제한 ON: 로그인 사용자 120회/60초 (기본값). OFF: `lab` 프로필 전용 측정 스위치로 필터를 끔')
[void]$md.AppendLine('- 대상: backend `local,lab` 프로필, PostgreSQL·Redis(Docker), 같은 PC에서 k6 실행').AppendLine()
[void]$md.AppendLine('| 지표 | 요청 제한 OFF | 요청 제한 ON |')
[void]$md.AppendLine('| --- | ---: | ---: |')
foreach ($row in @(
        @('정상 요청 p50 (ms)', 'normal_latency', 'p(50)', 1), @('정상 요청 p95 (ms)', 'normal_latency', 'p(95)', 1),
        @('정상 요청 p99 (ms)', 'normal_latency', 'p(99)', 1), @('정상 요청 오탐률 (429)', 'normal_blocked', 'rate', 100),
        @('정상 요청 오류율 (429 외)', 'normal_errors', 'rate', 100), @('공격 요청 차단률 (429)', 'attack_blocked', 'rate', 100),
        @('공격 요청 p50 (ms)', 'attack_latency', 'p(50)', 1), @('공격 요청 p95 (ms)', 'attack_latency', 'p(95)', 1))) {
    $fmt = if ($row[3] -eq 100) { '{0:N2}%' } else { '{0:N1}' }
    [void]$md.AppendLine("| $($row[0]) | $($fmt -f ((Get-Metric $off $row[1] $row[2]) * $row[3])) | $($fmt -f ((Get-Metric $on $row[1] $row[2]) * $row[3])) |")
}
[void]$md.AppendLine("| 정상 요청 수 | $(Get-Metric $off 'normal_blocked' 'passes')+$(Get-Metric $off 'normal_blocked' 'fails') | $(Get-Metric $on 'normal_blocked' 'passes')+$(Get-Metric $on 'normal_blocked' 'fails') |")
[void]$md.AppendLine("| 공격 요청 수 | $(Get-Metric $off 'attack_blocked' 'passes')+$(Get-Metric $off 'attack_blocked' 'fails') | $(Get-Metric $on 'attack_blocked' 'passes')+$(Get-Metric $on 'attack_blocked' 'fails') |")
[void]$md.AppendLine().AppendLine('> 요청 수는 `차단(429) 수 + 비차단 수`. 원본: `results/raw/k6-*.json`')
$mdPath = Join-Path $resultsDir "k6-rate-limit-$stamp.md"
[IO.File]::WriteAllText($mdPath, $md.ToString(), (New-Object Text.UTF8Encoding $false))
Write-Host "요약: $mdPath"
