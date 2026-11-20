# 인수인계 (2026-11-21 01:00 기준)

## 1. 진행 현황

| 주차 | 상태 | 비고 |
| --- | --- | --- |
| 1~7주 | ✅ 완료 | 인증·문서·취약 대조군·요청 제한·로그인 보호·Mock AI·AI-03·Ollama 벤치마크·도구 정책·CI |
| 8주 | 🟡 거의 완료 | k6 부하 측정, 실험 보고서, 회고, 포트폴리오 스토리, 시연 대본 완료. **시연 영상 녹화만 남음**(사용자) |

- 테스트: backend 113건 + vulnerable-api 13건, GitHub Actions CI 통과.
- 실측: SEC-01~04, SEC-04 부하(k6), AI-01(+AI-02/04 경로) → [experiment-results.md](experiment-results.md)
- 사용자 위임 결정 기록: [DECISIONS-LOG.md](DECISIONS-LOG.md) (Ollama·k6 설치, MIT 라이선스, 측정 스위치 등)
- 커밋 규칙: 한 덩어리 작업이 끝나면 커밋 후 `origin main`에 push (사용자 확인 없이).

## 2. 다시 시작하는 방법

```powershell
# 1) Docker Desktop 실행 후 DB·Redis
docker compose --env-file infrastructure/.env -f infrastructure/compose.yml up -d --wait
# 2) 백엔드 (local 프로필, http://127.0.0.1:8080, Swagger: /swagger-ui/index.html)
cd backend; .\gradlew.bat bootRun
# 3) 테스트
.\gradlew.bat build
```

- **Ollama**: 실험할 때는 데스크톱 앱 대신 `ollama serve`를 권장 (`$env:OLLAMA_MAX_LOADED_MODELS='1'` 후 실행). 설치 위치 `%LOCALAPPDATA%\Programs\Ollama`, 모델 `qwen2.5:1.5b`·`gemma3:1b`. 계정 가입 불필요.
- **k6**: `C:\Program Files\k6\k6.exe` (2.4.0)
- **실험 스크립트** (전용 포트를 쓰고, 응답한 서버가 SecureLab인지 확인한 뒤에만 실행):
  - SEC-01~04: `experiments/api-security/run-sec-experiment.ps1` (backend 18081, 취약 앱 18080, 약 1분)
  - AI-01: `experiments/prompt-injection/run-ai-benchmark.ps1` (backend 18090, Ollama 필요, 약 7분)
  - 부하: `experiments/load-tests/run-k6.ps1` (backend 18091, 약 5분)

## 3. 남은 일

1. **시연 영상**: [demo-script.md](demo-script.md) 대본대로 약 5분 녹화 → README에 링크.
2. (선택) k6 OFF/ON을 3회 이상 번갈아 재서 지연 차이 변동 범위 확인.
3. (선택) [회고](retrospective.md)의 "다음에 할 일" — 점진적 제재, 분산 대입 방어, 더 큰 모델로 AI-01.

## 4. 환경 주의 사항

- **포트**: PostgreSQL 18 서비스가 5432를 써서 Compose DB는 `55432`(`infrastructure/.env`의 `DB_PORT`). 다른 프로젝트(TicketFlow)가 **8080**을 쓰므로 둘 중 하나만 띄울 것. 실험 스크립트는 그래서 전용 포트를 씀.
- **비밀 값**: `infrastructure/.env`(DB_PASSWORD, REDIS_PASSWORD, LOGIN_DIGEST_SECRET)는 Git에 없음. 잃어버리면 `.env.example`을 복사해 새 무작위 값으로 채우고, DB 비밀번호가 바뀌면 `docker compose down -v`로 볼륨을 다시 만들어야 함(로컬 데이터 삭제).
- **Windows PowerShell 5.1**: `.ps1`에 한글이 있으면 UTF-8 **BOM**으로 저장. `--args`에 인자가 여럿이면 `"--args=`"... ...`""`처럼 따옴표로 감쌀 것.
- **k6**: 반복마다 쿠키를 비우는 것이 기본 — 세션을 쓰는 시나리오는 `noCookiesReset: true`.
- **gemma3:1b**: JSON 모드에서 가끔 토큰 반복으로 생성이 중단됨(`token repeat limit reached`) — 벤치마크는 시드를 바꿔 재시도.
- 테스트는 실제 HTTP + 쿠키 저장소(`ApiClient`)로 한다. spring-security-test의 `csrf()`는 공유 컨텍스트의 CSRF 저장소를 바꿔 버려 쓰지 않는다.

## 5. 종료 상태

작업이 끝나면 이 프로젝트의 서버를 모두 종료한다: Docker Compose `securelab`(postgres·redis, **볼륨은 유지**), Ollama, Gradle 데몬. backend·vulnerable-api는 실험 스크립트가 끝날 때 스스로 종료한다.
