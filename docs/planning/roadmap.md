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

### 목 11/26 — 세션 로그인·로그아웃, CSRF

- [ ] 세션 로그인/로그아웃, HttpOnly·SameSite 쿠키, 로그인 시 세션 ID 교체(세션 고정 방지)
- [ ] CSRF 토큰 전달 방식 확정 (I-4)
- [ ] 401/403을 공통 에러 형식으로 응답 (`AuthenticationEntryPoint`, `AccessDeniedHandler`)
- **완료 조건:** 세션 유지·종료 테스트

### 금 11/27 — 문서 엔티티, 소유권 검사

- [ ] `Document` 엔티티, `GET /api/v1/documents/{id}` 소유권 검사
- **완료 조건:** 타인 문서 403 확인

### 토 11/28 — 통합 테스트

- [ ] Testcontainers PostgreSQL 기반 인증·인가 통합 테스트
- **완료 조건:** 401/403 및 정상 흐름 통과

### 일 11/29 — 문서화, 정리

- [ ] springdoc Swagger UI, README 실행 방법 갱신, 시나리오 표, GitHub 저장소 정리
- **완료 조건:** 다른 개발자도 README만 보고 로컬 실행 가능

## 2~8주 주요 작업

### 2주 — 문서 CRUD, 관리자 접근 제어
- 문서 목록(접근 가능한 문서만), `PATCH`/`DELETE` 소유자 검사
- `GET /users/{id}` 본인 또는 관리자
- requestId 필터, 전역 예외 처리, 공통 에러 응답
- 관리자 API 골격(`/security/events`), SEC-01/02 회귀 테스트

### 3주 — 격리된 취약점 시뮬레이터
- ADR-0002 확정 (I-1), `lab` 프로필 정책 확정 (I-6)
- `vulnerable-api` 작성(소유권 검사가 빠진 버전)
- 같은 시나리오를 취약·보호 대상에 각각 실행하는 테스트
- `attack_scenarios` 시드(합성 페이로드만), 재현 기록

### 4주 — 요청 제한, 탐지, 감사
- Redis를 Compose에 추가, 요청 제한(429 + `Retry-After`)
- 로그인 실패 제한, `login_attempts` 기록, `email_digest` HMAC (I-5)
- `security_events` 감사 기록 (I-3)
- Redis 장애 시 허용/거부 정책 결정 및 기록
- 보존 기간 만료 데이터 정리 작업
- SEC-03/04 테스트, 정상·공격 혼합 트래픽 설계 (I-8)

### 5주 — Mock AI, 권한 필터링 검색
- `AiClient` 인터페이스, `MockAiClient`
- 권한 필터링 문서 검색(문서 도메인 서비스 경유), `ai_sessions`
- 실험 결과 스키마 확장 (I-2), AI-03 테스트

### 6주 — Ollama, Prompt Injection 벤치마크
- Ollama 설치, 1~3B급 모델 후보별 RAM·응답 속도 측정
- Spring AI 연동 (Boot 4.1 호환 버전 확인)
- AI-01 벤치마크: 모델 반응과 실제 행위를 따로 기록, ASR 기록

### 7주 — 도구 실행 정책, CI
- `ToolExecutionPolicyService`: 허용 목록 → 인자 스키마 검증 → 주체·소유권·역할 재검사
- AI-02/04 테스트, `ai_tool_calls` 감사 기록
- GitHub Actions CI (Testcontainers 포함), 로그 정책 점검

### 8주 — 측정, 보고서, 시연
- k6 시나리오(정상 + 공격 혼합), 정책 전후 p50/p95·오탐률·차단률
- [experiment-results.md](../experiment-results.md) 작성, 시연 영상, README 완성, 회고
