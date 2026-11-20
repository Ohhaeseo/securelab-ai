# 인수인계 (2026-11-21 01:05 기준)

## 1. 한눈에

| 항목 | 상태 |
| --- | --- |
| 로드맵 | **1~7주차 완료, 8주차는 시연 영상 녹화만 남음** |
| 테스트 | backend 113건 + vulnerable-api 13건 통과, GitHub Actions CI 통과 (마지막 확인 커밋 `51aea26`) |
| 실측 | SEC-01~04, SEC-04 부하(k6 2회), AI-01(+AI-02/04 경로) → [experiment-results.md](experiment-results.md) |
| 보완 사항 | I-1~I-7, I-9 반영, I-8 부분 반영(부하 혼합은 k6로 측정 완료, 반복 수 보강 여지) |
| 위임 결정 | [DECISIONS-LOG.md](DECISIONS-LOG.md) D-01~D-15 (Ollama·k6 설치, MIT 라이선스, 측정 스위치 등) |
| 서버 | 모두 종료함 (아래 6장) |

## 2. 핵심 결과 (면접·README용)

| 시나리오 | 취약 대조군 / 방어 없음 | 보호 버전 |
| --- | --- | --- |
| SEC-01 BOLA (조회·목록·수정·삭제, 30회) | ASR 100% | **ASR 0%**, 정상 오탐 0% |
| SEC-02 일반 사용자의 관리 API | ASR 100% | **0%** |
| SEC-03 비밀번호 반복 대입 | 30회 실패 뒤 로그인 성공 | 5회 실패 뒤 차단, 같은 IP 다른 계정 정상 |
| SEC-04 부하 (정상 10명 + 공격 2명, 90초 × 2) | 차단 없음 | 공격 46% 차단(이론값 일치), **정상 오탐 0%**, p50 +1.2~2.5ms |
| AI-01 문서 속 악성 지시 (페이로드 10종 × 3) | 모델 반응 qwen 23~33%, gemma 70% | **실제 행위 0%** (정책 게이트) |

스토리 정리: [portfolio-story.md](portfolio-story.md) · 회고: [retrospective.md](retrospective.md)

## 3. 다시 시작하는 방법

```powershell
# 1) Docker Desktop 실행 후 DB·Redis
docker compose --env-file infrastructure/.env -f infrastructure/compose.yml up -d --wait
# 2) 백엔드 (local 프로필, http://127.0.0.1:8080, Swagger: /swagger-ui/index.html)
cd backend; .\gradlew.bat bootRun
# 3) 테스트
.\gradlew.bat build
```

- **Ollama** (AI 실험·로컬 모델 도우미): 데스크톱 앱 대신 `$env:OLLAMA_MAX_LOADED_MODELS='1'; ollama serve` 권장. 설치 위치 `%LOCALAPPDATA%\Programs\Ollama`, 모델 `qwen2.5:1.5b`·`gemma3:1b`. **계정 가입 불필요.**
- **k6**: `C:\Program Files\k6\k6.exe` (2.4.0)
- **실험 스크립트** — 전용 포트를 쓰고, 응답한 서버가 SecureLab인지 확인한 뒤에만 실행:

| 스크립트 | 내용 | 포트 | 소요 |
| --- | --- | --- | --- |
| `experiments/api-security/run-sec-experiment.ps1` | SEC-01~04 (보호 vs 취약) | 18081, 18080 | 약 1분 |
| `experiments/load-tests/run-k6.ps1` | 요청 제한 OFF/ON 혼합 부하 | 18091 | 약 4분 |
| `experiments/prompt-injection/run-ai-benchmark.ps1` | AI-01 (Ollama 필요) | 18090 | 약 7분 |

## 4. 남은 일 (우선순위)

1. **시연 영상 녹화** — [demo-script.md](demo-script.md) 대본대로 약 5분, README에 링크.
2. **Ollama 모드 도우미 확인** — README 6단계(`$env:SECURELAB_AI_PROVIDER='ollama'; .\gradlew.bat bootRun`)는 코드상 동작하지만 **실제 실행으로는 아직 확인하지 않음** (벤치마크는 같은 클라이언트로 확인됨).
3. (선택) k6 반복 측정 추가 — 3회차는 종료 시간 때문에 중단(D-14). 꼬리 지연(p95·p99) 차이는 2회로는 판단 불가.
4. (선택) [회고](retrospective.md)의 "다음에 할 일": 반복 위반자 점진적 제재, 분산 대입 방어(계정 단위 상한), 더 큰 모델로 AI-01, 쓰기 도구를 넣는다면 사용자 확인 단계.
5. (선택) `GET /api/v1/experiments/{id}` — 기획서에 있으나 미구현(결과는 POST 응답과 `experiment_runs`로 확인).

## 5. 환경 주의 사항

- **포트**: PostgreSQL 18 서비스가 5432를 써서 Compose DB는 `55432`(`infrastructure/.env`의 `DB_PORT`). 다른 프로젝트(TicketFlow)가 **8080**을 쓰므로 둘 중 하나만 띄울 것. 실험 스크립트는 그래서 전용 포트를 씀.
- **비밀 값**: `infrastructure/.env`(DB_PASSWORD, REDIS_PASSWORD, LOGIN_DIGEST_SECRET)는 Git에 없음. 잃어버리면 `.env.example`을 복사해 새 무작위 값으로 채우고, DB 비밀번호가 바뀌면 `docker compose down -v`로 볼륨을 다시 만들어야 함(로컬 데이터 삭제).
- **Windows PowerShell 5.1**: `.ps1`에 한글이 있으면 UTF-8 **BOM**으로 저장. bootRun `--args`에 인자가 여럿이면 `"--args=`"a b`""`처럼 따옴표로 감쌀 것.
- **k6**: 반복마다 쿠키를 비우는 것이 기본 — 세션을 쓰는 시나리오는 `noCookiesReset: true`.
- **gemma3:1b**: JSON 모드에서 가끔 토큰 반복으로 생성이 중단됨 — 벤치마크는 시드를 바꿔 재시도.
- **테스트**: 실제 HTTP + 쿠키 저장소(`ApiClient`)로 한다. spring-security-test의 `csrf()`는 공유 컨텍스트의 CSRF 저장소를 바꿔 버려 쓰지 않는다. 공통 테스트 설정은 요청 제한·로그인 보호 한도를 크게 둔다.
- **측정 스위치** `securelab.rate-limit.enabled=false`는 `lab` 프로필에서만 허용(다른 프로필에서는 기동 실패).

## 6. 종료 상태 (01:05)

- 모든 변경 커밋·push 완료 (`main` = `origin/main`).
- 종료함: Docker Compose `securelab`(postgres·redis — **데이터 볼륨은 유지**), Ollama(`ollama serve`), Gradle 데몬, 실험용 backend·k6.
- 설치된 채로 둔 것: Ollama 앱·모델, k6 (제거 방법은 DECISIONS-LOG D-01·D-02·D-08).
