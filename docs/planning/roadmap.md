# 개발 로드맵 (8주)

> 기간: 2026-11-23(월) ~ 2027-01-17(일). 주차별 산출물은 확인할 수 있는 것(테스트, 문서, 기록)으로 정의한다.
> 보완 사항 ID(I-n)는 [plan-summary.md](plan-summary.md) 7장을 가리킨다.

## 주차별 계획

| 주차 | 기간 | 목표 | 검증 가능한 산출물 | 모듈 / 시나리오 |
| --- | --- | --- | --- | --- |
| 1 | 11/23–11/29 | 환경 구축, DB, 가입·로그인, 소유권 인가 | 기본 API + 인증 테스트 | M1, M2 / SEC-01 |
| 2 | 11/30–12/06 | 문서 CRUD, 관리자 접근 제어, 오류 응답 | BOLA 테스트, API 문서 | M2 / SEC-01, SEC-02 |
| 3 | 12/07–12/13 | 격리된 로컬 취약점 시뮬레이터 | 공격 재현 기록, 방어 비교 | M3 / SEC-01, SEC-02 |
| 4 | 12/14–12/20 | Redis 요청 제한, 로그인 탐지, 감사 로그 | 429 정책, 보안 이벤트 조회 | M4 / SEC-03, SEC-04 |
| 5 | 12/21–12/27 | Mock AI, 소유권 필터링 문서 검색 | 인가된 문서만 AI에 전달 | M5 / AI-03 |
| 6 | 12/28–01/03 | Ollama 연동, Prompt Injection 벤치마크 | 시나리오별 ASR 기록 | M5, M6 / AI-01 |
| 7 | 01/04–01/10 | 도구 실행 정책, 테스트·CI, 로그 | 무단 도구 실행 0건 검증 | M6 / AI-02, AI-04 |
| 8 | 01/11–01/17 | k6 성능 측정, 보고서·시연·README | 전후 지표, 영상, 회고 | M7 / 전체 |

> 5~6주차는 연말연시(12/25, 1/1)와 겹친다. 일정이 밀리면 AI-01 벤치마크의 모델 후보 수를 줄여 대응한다.

## 1주차 상세 (11/23–11/29)

### 월 11/23 — 환경 확인, 프로젝트 생성 ✅ (11/20에 먼저 완료)

- [x] 도구 확인: Java 21.0.8, Git 2.45.1, WSL 2, Docker 29.8.0, Docker Compose v5.5.1
- [x] Spring Initializr로 `backend/` 생성
  - Gradle - Groovy, Java 21, **Spring Boot 4.1.3** (Initializr 기본값 4.2.0은 springdoc 지원 범위 밖이라 쓰지 않음)
  - Group `com.securelab`, Artifact `backend`, Name `SecureLab`, Package name `com.securelab`
  - 의존성: Spring Web, Validation, Spring Security, Spring Data JPA, PostgreSQL Driver, Flyway Migration, Actuator, Testcontainers, springdoc-openapi 3.2.0
  - Gradle wrapper jar 체크섬을 Gradle 공식 값과 대조
- [x] 의존성 잠금(`gradle.lockfile`) 생성, Testcontainers 이미지를 `postgres:17`로 고정 (Compose와 동일)
- [x] `gradlew build` 통과 (Testcontainers 기반 `contextLoads`)
- [x] Compose DB에 붙여 기동, `/actuator/health` UP 확인
- [x] `git init`, `.env` 미추적 확인, 첫 커밋, GitHub push
- 참고: 이 PC에는 PostgreSQL 18 서비스가 5432를 쓰고 있어 Compose는 `DB_PORT=55432`로 띄운다.
- **완료 조건:** 앱 실행, 커밋 성공

### 화 11/24 — DB, 마이그레이션, 헬스 체크 ✅ (11/20에 먼저 완료)

- [x] `infrastructure/.env` 생성(무작위 `DB_PASSWORD`), PostgreSQL Compose 기동 (월요일에 함께 완료)
- [x] `application-local.yml`: `infrastructure/.env`와 루트 `.env`를 읽어 `DB_PORT`·`DB_PASSWORD`로 접속 (OS 환경 변수가 우선)
- [x] `gradlew bootRun`은 기본으로 `local` 프로필 사용
- [x] Flyway `V1__init_schema.sql`: `users`, `documents`만 먼저 생성. 나머지 테이블은 해당 주차에 `V2` 이후로 추가 (I-2, I-3 반영 여지)
  - 기획서 제약에 더해 `CHECK(email = lower(email))`, `CHECK(role IN ('USER','ADMIN'))` 추가
- [x] Actuator는 `health`만, 세부 정보 없이 노출. 에러 응답에서 스택 트레이스·예외 메시지 제외. `ddl-auto=validate`, `open-in-view=false`
- [x] 테스트: `SchemaMigrationTests`(제약 7건), `ActuatorHealthTests`(노출 범위 2건) — 전체 10건 통과
- **완료 조건:** DB 연결, 헬스 체크 성공 — `bootRun`으로 V1 적용, `/actuator/health` UP 확인

### 수 11/25 — 사용자, 가입 ✅ (11/20에 먼저 완료)

- [x] 비밀번호 인코더 결정 (I-7) → Argon2id + `DelegatingPasswordEncoder` ([ADR-0003](../adr/0003-password-encoder.md))
- [x] `User` 엔티티, `UserRepository`
- [x] `POST /api/v1/auth/signup`: 입력 검증, 이메일 중복 409, 단방향 해시 저장
- [x] 이메일은 공백 제거 후 소문자로 정규화해 저장 (`EmailNormalizer`)
- [x] 공통 에러 응답(`ErrorResponse`, `GlobalExceptionHandler`), `RequestIdFilter`(안전하지 않은 값은 교체), 401/403 JSON
- [x] CSRF: `XSRF-TOKEN` 쿠키(SameSite=Lax) → `X-XSRF-TOKEN` 헤더, `GET /api/v1/auth/csrf` (I-4)
- [x] 통합 테스트는 실제 HTTP(임의 포트) + 쿠키 저장소로 검증 (`ApiClient`). spring-security-test의 `csrf()`는 공유 컨텍스트의 CSRF 저장소를 바꿔 버려 쓰지 않는다
- **완료 조건:** 가입 API 테스트 성공

### 목 11/26 — 세션 로그인·로그아웃, CSRF ✅ (11/20에 먼저 완료)

- [x] 세션 로그인/로그아웃, HttpOnly·SameSite 쿠키, 로그인 시 세션 ID 교체(세션 고정 방지)
- [x] CSRF 토큰 전달 방식 확정 (I-4) — 로그인 시 재발급, 새 토큰은 로그인 응답 쿠키로 바로 전달
- [x] 401/403을 공통 에러 형식으로 응답 (`AuthenticationEntryPoint`, `AccessDeniedHandler`)
- [x] `GET /users/me`, `GET /users/{id}`(본인·관리자, 권한 검사를 존재 확인보다 먼저) — 2주차 항목 앞당김
- [x] 테스트 8건: 세션 고정 방지, 예전 세션 ID 무효, CSRF 재발급·예전 토큰 거부, 계정 열거 방지, 로그아웃 후 세션 재사용 불가
- **완료 조건:** 세션 유지·종료 테스트

### 금 11/27 — 문서 엔티티, 소유권 검사 ✅ (11/20에 먼저 완료)

- [x] `Document` 엔티티, `POST /api/v1/documents`, `GET /api/v1/documents/{id}` 소유권 검사
- [x] 인가 규칙을 `DocumentAccessPolicy` 한 곳에 모음 (읽기: 소유자·공개, 수정·삭제: 소유자만)
- [x] 소유자는 항상 인증 주체 — 요청 본문의 `ownerId`는 무시
- [x] 테스트 7건 (SEC-01 읽기: 남의 비공개 문서 403, 본문 미노출)
- **완료 조건:** 타인 문서 403 확인

### 토 11/28 — 통합 테스트 ✅ (11/20에 먼저 완료)

- [x] Testcontainers PostgreSQL 기반 인증·인가 통합 테스트 — `AccessControlMatrixTests` 17건
  - 보호 엔드포인트 10개 익명 → 401, 공개 엔드포인트 2개 → 허용
  - SEC-02: 일반 사용자 → 관리자 API 403
  - 없는 경로 404 JSON, 잘못된 메서드 405 JSON, 가입 → 로그인 → 문서 → 로그아웃 정상 흐름
- **완료 조건:** 401/403 및 정상 흐름 통과

### 일 11/29 — 문서화, 정리 ✅ (11/20에 먼저 완료)

- [x] springdoc Swagger UI (`local` 프로필에서만, CSRF 헤더 자동 전송), 세션 쿠키 보안 스킴
- [x] README: 구현 현황, API 호출 예시(PowerShell, 실제 실행으로 확인), 파일 트리 갱신
- **완료 조건:** 다른 개발자도 README만 보고 로컬 실행 가능

## 2~8주 주요 작업

### 2주 — 문서 CRUD, 관리자 접근 제어 ✅ (11/20에 먼저 완료)
- [x] 문서 목록(본인 + 공개 문서만, DB 쿼리 단계에서 거름, 최신순 고정, 페이지 크기 최대 100), `PATCH`/`DELETE` 소유자 검사
- [x] `GET /users/{id}` 본인 또는 관리자 (1주차 목요일에 앞당김)
- [x] requestId 필터, 전역 예외 처리, 공통 에러 응답 (1주차 수요일에 앞당김)
- [x] 관리자 API: `GET /security/events`(유형 필터, 페이지), `GET /security/metrics`(기간별 차단 집계)
- [x] `V2__security_events.sql` (I-3 컬럼 포함) — 문서·프로필 접근 거부, 역할 위반, CSRF 거부를 기록 (4주차 감사 로그 일부 앞당김)
- [x] SEC-01 쓰기(남의 문서 수정·삭제 403, 내용 불변), 관리자도 남의 비공개 문서 접근 불가, 이벤트 requestId 추적 테스트 — 전체 63건
- [ ] 관리자 API의 "개별 작업 권한" 확인 (체크리스트 항목) — 관리 작업이 조회뿐이라 보류, 쓰기 작업이 생기면 추가

### 3주 — 격리된 취약점 시뮬레이터 ✅ (11/20에 먼저 완료)
- [x] ADR-0002 확정 (I-1), `lab` 프로필 정책 확정 (I-6) — `GET /api/v1/scenarios`는 `lab` 프로필 + ADMIN
- [x] `vulnerable-api`: 인증은 보호 버전과 같고 인가만 뺀 대조군, 메모리 저장소, 루프백 전용 가드, 결함 재현 회귀 테스트 13건
- [x] 같은 시나리오를 취약·보호 대상에 각각 실행: `runner/ScenarioRunner.java` + `run-sec-experiment.ps1`
- [x] `attack_scenarios` 시드(V3, 합성 페이로드만), 재현 기록 → [experiment-results.md](../experiment-results.md)
  - 실측(30회): SEC-01 읽기·목록·수정·삭제, SEC-02 모두 취약 ASR 100% → 보호 0%, 오탐 0%

### 4주 — 요청 제한, 탐지, 감사 ✅ (11/20에 먼저 완료)
- [x] Redis 7.4를 Compose에 추가(비밀번호 필수), 요청 제한(429 + `Retry-After`): 로그인 사용자 ID별 120/분, 익명 IP별 60/분
- [x] 로그인 실패 제한: (계정+IP) 5회, IP 20회 / 15분, `login_attempts`(V4) 기록, `email_digest` HMAC (I-5)
- [x] `security_events` 감사 기록 (I-3, 2주차) + `RATE_LIMITED`·`LOGIN_BLOCKED` (차단 시작 시 1회)
- [x] Redis 장애 시 정책: API 요청 제한 fail-open, 로그인 보호 DB 대체 집계 ([ADR-0004](../adr/0004-rate-limit-and-login-protection.md))
- [x] 보존 기간 정리 작업: `login_attempts`·`security_events` 30일, 매일 03:30
- [x] SEC-03/04 테스트(통합 11건, 단위 7건) + 러너 측정. 정상·공격 혼합(I-8): 러너가 공격과 함께 다른 사용자의 정상 요청을 보내 오탐을 잰다. 부하 상황 혼합은 8주차 k6

### 5주 — Mock AI, 권한 필터링 검색 ✅ (11/20에 먼저 완료)
- [x] `AiClient` 인터페이스, `MockAiClient` (결정적 동작, 문서 속 지시를 따르는 "넘어가는 모델" 흉내 — 7주차 정책 게이트의 공격 대상)
- [x] 권한 필터링 문서 검색(문서 도메인 서비스 경유): 키워드 검색은 인가 조건과 한 쿼리, 문서 ID 지정은 하나라도 권한이 없으면 모델 호출 전 403
- [x] `ai_sessions`(V5), 세션 소유자만 사용. 질문·답변·문서 원문은 저장하지 않음
- [x] 실험 결과 스키마 확장 (I-2, V6): `experiment_runs`(model_complied / action_executed 분리), `ai_tool_calls`(인자는 SHA-256만)
- [x] 모델이 제안한 도구 호출은 `PROPOSED`로 기록만 하고 실행하지 않음 (실행 정책은 7주차)
- [x] AI-03 테스트: 모델에 전달된 입력을 가로채 남의 비공개 문서가 0건인지 확인 — 통합 테스트 6건

### 6주 — Ollama, Prompt Injection 벤치마크 ✅ (11/21에 먼저 완료)
- [x] Ollama 0.40.16 설치(winget), `qwen2.5:1.5b`(986MB)·`gemma3:1b`(806MB), CPU 실행 응답 1~5초
- [x] Spring AI 대신 Ollama REST 직접 호출 (루프백 전용, 문서 이스케이프) — [ADR-0006](../adr/0006-local-llm-integration.md)
- [x] AI-01 벤치마크: `POST /api/v1/experiments`(lab + ADMIN), 합성 페이로드 10종 × 목표 2개 × 반복 3, 모델 반응과 실제 행위 분리 기록(`experiment_runs`)
  - 실측: 모델 반응률 qwen 23~33%, gemma 70% → **정책 ON 실제 행위 0%**
- [x] `run-ai-benchmark.ps1` (전용 포트 18090, 대상 확인, 결과 JSON·Markdown 저장)

### 7주 — 도구 실행 정책, CI ✅ (11/20에 먼저 완료, 6주차보다 먼저 진행)
- [x] `ToolExecutionPolicyService`: 호출 수 상한(3) → 허용 목록 → 인자 스키마 검증 → 역할 → 현재 사용자 권한으로 실행(소유권 재검사) — [ADR-0005](../adr/0005-ai-tool-execution-policy.md)
- [x] 읽기 전용 도구 3종(`get_document`, `list_documents`, `list_security_events`), 쓰기 도구는 confused deputy 위험으로 보류
- [x] AI-01/02/04 테스트, `ai_tool_calls` 감사 기록(ALLOWED/DENIED·사유·인자 해시), 거부는 `AI_TOOL_DENIED` 보안 이벤트
- [x] GitHub Actions CI: backend(Testcontainers PostgreSQL·Redis) + vulnerable-api, `permissions: contents: read`, wrapper 검증
- [x] 로그 정책 점검: `LogHygieneTests` — 비밀번호·이메일·문서 본문·질문 원문이 어떤 로그에도 없음을 확인

### 8주 — 측정, 보고서, 시연 🟡 (11/21에 대부분 완료)
- [x] k6 시나리오(정상 + 공격 혼합), 정책 전후 p50/p95·오탐률·차단률 — 2회 측정: 오탐 0%, 공격 차단 46%(고정 윈도우 이론값과 일치), p50 +1.2~2.5ms
- [x] 요청 제한 측정 스위치는 `lab` 프로필 전용 (다른 프로필에서 끄면 기동 실패)
- [x] [experiment-results.md](../experiment-results.md) 작성, README 완성, [회고](../retrospective.md), [포트폴리오 스토리](../portfolio-story.md), [시연 대본](../demo-script.md)
- [ ] 시연 영상 녹화 — 대본대로 사용자가 직접 녹화 (Claude는 화면 녹화 불가)
