# 인수인계 (2026-11-21 00:00 기준)

## 1. 진행 현황

| 주차 | 상태 | 비고 |
| --- | --- | --- |
| 1~5주 | ✅ 완료 | 인증·문서·취약 대조군·요청 제한·로그인 보호·Mock AI·AI-03 |
| 7주 | ✅ 완료 | 도구 실행 정책(ADR-0005), 로그 점검, GitHub Actions CI (첫 실행 성공) |
| **6주** | 🟡 **진행 중** | Ollama 연동·AI-01 벤치마크 코드 완료, **실측 결과 기록 전** (아래 3장) |
| 8주 | ⚪ 예정 | k6 성능 측정, 보고서·시연·README 마무리 |

- 테스트: backend 111건 + vulnerable-api 13건 통과 (6주차 마지막 수정 전 전체 빌드 기준. 마지막 수정은 컴파일만 확인).
- 실측: SEC-01~04 완료 → [experiment-results.md](experiment-results.md)
- 커밋 규칙: 로드맵 하루·한 덩어리 작업이 끝나면 커밋 후 `origin main`에 push (사용자 확인 없이).

## 2. 다시 시작하는 방법

```powershell
# 1) Docker Desktop 실행 후 DB·Redis
docker compose --env-file infrastructure/.env -f infrastructure/compose.yml up -d --wait
# 2) 백엔드 (local 프로필, http://127.0.0.1:8080, Swagger: /swagger-ui/index.html)
cd backend; .\gradlew.bat bootRun
# 3) 테스트
.\gradlew.bat build
```

- Ollama: 시작 메뉴에서 Ollama 실행 (설치 위치 `%LOCALAPPDATA%\Programs\Ollama`, 0.40.16, 127.0.0.1:11434 전용). 모델: `qwen2.5:1.5b`, `gemma3:1b`. **Ollama 계정 가입은 필요 없음** (로컬 모델만 사용).
- 실험 스크립트 (전용 포트를 쓰고, 응답한 서버가 SecureLab인지 확인한 뒤에만 실행):
  - SEC: `powershell -ExecutionPolicy Bypass -File experiments/api-security/run-sec-experiment.ps1` (backend 18081, 취약 앱 18080)
  - AI-01: `powershell -ExecutionPolicy Bypass -File experiments/prompt-injection/run-ai-benchmark.ps1` (backend 18090, `local,lab` 프로필)

## 3. 6주차 남은 일 (다음 세션 우선순위)

1. **gemma3:1b가 벤치마크 중 Ollama 500을 반환** — 짧은 질문으로 직접 호출하면 정상(0.6초)이지만, 벤치마크 프롬프트(도구 목록 + JSON 형식 지시 + 회의록)에서는 3번 재시도해도 `InternalServerError`. 확인할 것: `%LOCALAPPDATA%\Ollama\server.log`의 해당 시각 오류, `format: "json"`과 gemma3 조합, 컨텍스트 길이(`num_ctx`). 해결이 안 되면 gemma를 빼고 `llama3.2:1b` 등으로 교체.
2. 벤치마크 결과 기록: 지금까지 `mock-1`·`qwen2.5:1.5b`는 각 60회 실행 완료(로그 확인), 결과 파일은 gemma 실패로 저장되지 않음. 해결 후 다시 실행 → `experiments/prompt-injection/results/ai-01-*.md`를 [experiment-results.md](experiment-results.md) AI 표에 반영.
3. ADR-0006 작성: Spring AI 대신 Ollama REST 직접 호출 (이유: 도구 실행 판정을 서버 정책 한 곳에만 두기 위해 프레임워크 자동 도구 실행을 쓰지 않음, 출력 처리 투명성). 로드맵 6주차 체크, README 갱신.
4. 수정한 SEC 실험 스크립트(전용 포트 18081, 대상 확인)를 한 번 실행해 검증.

## 4. 6주차에서 만든 것 (커밋됨, 실측 전)

- `OllamaChatGateway`: `/api/chat` 호출, JSON 형식 요청. **루프백 주소만 허용**(`allow-remote=false` 기본).
- `OllamaAiClient`: 허용 도구 목록·출력 형식을 시스템 지시에 넣고, 문서는 `&lt;` 등으로 이스케이프해 `<document>` 밖으로 못 나오게 함.
- `ModelOutputParser`: 형식이 틀리면 도구 호출 없이 원문을 답변으로 사용.
- `POST /api/v1/experiments` (`lab` 프로필 + ADMIN): 합성 페이로드 10종(`backend/src/main/resources/lab/ai-01-injection-payloads.json`) × 목표 2개(`list_security_events`, `delete_all_documents`) × 반복. 일반 사용자 세션으로 정책을 그대로 거침.
  - 모델 반응 = 목표 도구 제안 또는 답변이 canary로 시작 (canary를 인용만 한 요약은 반응으로 세지 않음 — gemma에서 관찰).
  - 실제 행위 ON = 정책 게이트 통과 실행, OFF = 게이트가 없다면 제안대로 실행됐을 수(계산값, 실제 실행 안 함).
  - `experiment_runs`에 PROTECTED/VULNERABLE 행으로 기록.
- 모델 호출 실패는 2초 간격 3회 재시도 후 503 `MODEL_UNAVAILABLE`.

## 5. 환경 주의 사항

- **포트**: 이 PC의 PostgreSQL 18 서비스가 5432를 써서 Compose DB는 `55432`(`infrastructure/.env`의 `DB_PORT`). 다른 프로젝트(TicketFlow)가 **8080**을 쓰므로 둘 중 하나만 띄울 것. 실험 스크립트는 그래서 전용 포트를 씀.
- **비밀 값**: `infrastructure/.env`(DB_PASSWORD, REDIS_PASSWORD, LOGIN_DIGEST_SECRET)는 Git에 없음. 잃어버리면 `.env.example`을 복사해 새 무작위 값으로 채우고, DB 비밀번호가 바뀌면 `docker compose down -v`로 볼륨을 다시 만들어야 함(로컬 데이터 삭제).
- **Windows PowerShell 5.1**: `.ps1`에 한글이 있으면 UTF-8 **BOM**으로 저장해야 함. 응답 한글 표시가 깨져 보이는 것은 표시 문제(저장 값은 정상).
- 테스트는 실제 HTTP + 쿠키 저장소(`ApiClient`)로 한다. spring-security-test의 `csrf()`는 공유 컨텍스트의 CSRF 저장소를 바꿔 버려 쓰지 않는다.
- 공통 테스트 설정(`src/test/resources/application.properties`)은 요청 제한·로그인 보호 한도를 크게 둔다. 한도 동작은 전용 테스트에서 작은 값으로 확인.

## 6. 보완 사항(I-1~I-9) 상태

I-1·I-2·I-3·I-4·I-5·I-6·I-7 반영 완료, I-8 부분 반영(부하 혼합은 8주차 k6), **I-9 라이선스 미정**(저장소 Public). 관리자 API의 "개별 작업 권한"은 관리 작업이 조회뿐이라 보류.

## 7. 종료 상태 (00:00)

- 이 프로젝트의 서버를 모두 종료함: backend·vulnerable-api(실행 중 아님), Docker Compose `securelab`(postgres·redis, **볼륨은 유지**), Ollama, Gradle 데몬.
