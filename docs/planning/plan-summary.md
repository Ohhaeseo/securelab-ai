# SecureLab AI — 기획 정리본

> 원본: [project-plan.md](project-plan.md) (2026-11-20 기획서 사본)
> 이 문서는 원본을 실행 관점으로 다시 묶고, 검토하며 찾은 보완 사항을 모은 것이다. 원본과 다르면 **원본이 우선**한다. 보완 사항은 결정되는 대로 원본과 관련 문서에 반영한다.

## 1. 한눈에 보기

| 항목 | 내용 |
| --- | --- |
| 무엇 | 가상 기업 내부 시스템(사용자·문서·AI 비서)에서 API 인가 취약점과 Prompt Injection을 재현·탐지·방어·측정하는 로컬 실험 플랫폼 |
| 왜 | 백엔드 보안 설계 역량을 "문제 → 재현 → 원인 → 개선 → 지표" 흐름과 실측 데이터로 보여 주기 위해 |
| 형태 | 독립 GitHub 저장소 `securelab-ai`, Spring Boot 모듈형 모놀리스 |
| 기간 | 8주 (2026-11-23 ~ 2027-01-17) |
| 비용 | 로컬 개발 0원 목표 (Docker·Ollama 모두 로컬 실행) |
| 환경 | Windows + WSL2 + Docker Compose, RAM 32GB, CPU Intel Core Ultra 7 258V, GPU 내장 Intel Arc 140V (2026-11-20 확인 — CUDA 없음, 6주차 Ollama는 CPU 우선 평가) |

## 2. 설계 원칙

1. **인가는 서버가 다시 확인한다.** URL의 ID, 클라이언트 요청, AI가 제안한 도구 호출 모두 서버의 정책 서비스가 인증 주체·소유권·권한을 재검증한다.
2. **모델 출력과 검색 문서는 믿지 않는다.** 문서 내용은 데이터로만 다루고, 모델은 도구를 "제안"만 한다. 실행 여부는 `ToolExecutionPolicyService`가 정한다.
3. **권한 필터링은 모델 입력 전에 한다.** DB에서 접근 가능한 문서만 골라 모델에 넘긴다. 응답 후 텍스트 필터는 보조 수단일 뿐이다.
4. **취약 로직은 격리한다.** 일반 API에 feature flag로 넣지 않고, 별도 앱·컨테이너로 localhost나 내부 네트워크에서만 돌린다.
5. **민감 정보는 남기지 않는다.** 평문 비밀번호, 토큰, API 키, LLM 입력 원문, 문서 전문을 로그나 DB에 저장하지 않는다. ID·requestId·판정 코드 위주로 기록한다.
6. **수치는 실측만 쓴다.** 측정 환경, 샘플 수, 실패 사례를 함께 남기고, 측정 전 수치는 문서나 이력서에 쓰지 않는다.

## 3. 범위

### MVP 모듈

| 모듈 | 내용 | 우선순위 | 주차 | 완료 기준 |
| --- | --- | --- | --- | --- |
| M1 인증/사용자 | 가입, 로그인, 로그아웃, USER/ADMIN 분리 | P0 | 1 | 비로그인 401, 무권한 403 |
| M2 문서 API | 비공개 문서 CRUD, 소유권 검사 | P0 | 1–2 | 타인 문서 조회·수정 불가 |
| M3 보안 실험 | 격리된 취약 API와 보호 버전 비교 | P0 | 3 | 공격 재현·방어 회귀 테스트 통과 |
| M4 탐지/제한 | 로그인 시도 기록, Redis 요청 제한, 감사 이벤트 | P0 | 4 | 한도 초과 시 429, 보안 이벤트 조회 |
| M5 AI 문서 보조 | 권한 필터링 검색·요약, 도구 호출 제안 | P1 | 5–6 | 접근 가능한 문서만 참조 |
| M6 AI 정책 엔진 | Prompt Injection 실험, 도구 실행 시 권한 재검사 | P1 | 6–7 | 불허 도구 실제 실행 0건 |
| M7 실험 리포트 | 이벤트, 시나리오 결과, 지표 요약 | P1 | 8 | 실험별 성공률·지연 통계 확인 |

**범위 규칙:** 1~4주는 M1~M4(P0)만 한다. AI(P1)는 인증·인가 테스트가 안정된 뒤에 시작한다.

### 비목표

- 실제 외부 사이트 공격, 실사용자 데이터 수집
- AI 판단만으로 영구 차단하는 기능
- 첫 버전의 MSA, Kubernetes, Kafka, 결제, 복잡한 프론트엔드
- 실서비스 배포를 전제로 한 공격 재현 엔드포인트

## 4. 기술 스택 도입 순서

| 시점 | 도입 | 비고 |
| --- | --- | --- |
| 1주 | Java 21, Spring Boot 4.1.x, Gradle, Web MVC, Validation, Security, Data JPA, Flyway, PostgreSQL, Actuator, JUnit, Testcontainers, springdoc-openapi | **확정(11/20):** Boot 4.1.3, Gradle 9.9.2, springdoc 3.2.0 — `backend/gradle.lockfile`로 고정. Boot 4.2.0은 springdoc 미지원이라 제외 |
| 4주 | Redis | 기간별 요청 횟수, 잠금 상태 |
| 5주 | `MockAiClient` | GPU 확인 전 정책 검증용 |
| 6주 | Ollama + Spring AI | 1~3B급 소형 모델, CPU 실행 평가 |
| 7주 | GitHub Actions | 통합 테스트 CI |
| 8주 | k6 | 정책 적용 전후 지연·오탐 측정 |

## 5. 실험 시나리오와 구현 위치

| 시나리오 | 공격 | 방어 위치 | 주차 | 핵심 지표 | 통과 기준 |
| --- | --- | --- | --- | --- | --- |
| SEC-01 BOLA/IDOR | 사용자 A가 B의 비공개 문서 ID 조회 | `document.service` 소유권 검사 | 1–3 | 무단 조회 성공 수 | 0건 (403) |
| SEC-02 역할 우회 | 일반 사용자가 관리 API 호출 | Security 설정 + 작업별 권한 | 2–3 | 관리자 기능 오남용 수 | 0건 (403) |
| SEC-03 로그인 반복 | 비밀번호 반복 오입력 | `securitylab.detection`, `login_attempts` | 4 | 제한 도달 시간, 오탐률 | 일시 제한 + 기록 |
| SEC-04 과도한 호출 | 윈도우를 넘는 API 요청 | `securitylab.ratelimit` (Redis) | 4, 8 | 차단 비율, p50/p95 | 429 + `Retry-After` |
| AI-01 악성 문서 지시 | 문서에 "이전 지시 무시" 삽입 | 프롬프트 구성 + 정책 게이트 | 6 | 모델 반응 / 실제 행위 (따로 기록) | 실제 행위 0건 |
| AI-02 권한 없는 도구 | AI가 관리자 도구 호출 제안 | `ai.policy.ToolExecutionPolicyService` | 7 | 무단 실제 실행 수 | 0건 |
| AI-03 문서 접근 우회 | 타인 비공개 문서 검색 요청 | `ai.service` 권한 필터링 검색 | 5 | 유출 수 | 0건, 검색 전 차단 |
| AI-04 허용 목록 밖 도구 | 등록되지 않은 도구 이름 요청 | `ai.tools` 허용 목록 + 감사 | 7 | 차단률 | 전부 차단 + 감사 기록 |

지표 정의(ASR, 차단률, 오탐률, 지연)는 [experiment-results.md](../experiment-results.md)에 있다.

## 6. 산출물과 완료 기준

| 완료 기준 (DoD) | 산출물 위치 |
| --- | --- |
| README만으로 로컬 실행·테스트·DB 초기화 가능 | [README.md](../../README.md) |
| 8개 시나리오(SEC 4 + AI 4)의 조건과 결과 문서화 | [experiment-results.md](../experiment-results.md), `experiments/` |
| 보호 버전 자동 테스트에서 사용자 간 유출·무단 도구 실행 0건 | `backend/src/test/` |
| 정책 전후 지연·오탐·방어 성공률을 실측 데이터로 기록 | [experiment-results.md](../experiment-results.md) |
| 위협 모델, ADR 2개 이상, 시연 영상 | [threat-model.md](../threat-model.md), [adr/](../adr/), README 링크 |
| 면접용 "문제 → 재현 → 원인 → 개선 → 지표" 스토리 | 8주차 회고 |

## 7. 기획서 검토: 보완이 필요한 부분

원본을 읽으며 서로 맞지 않거나 비어 있는 곳을 모았다. 권장안은 제안이며, 결정 시점 전에 확정한다.

| ID | 항목 | 문제 | 권장안 | 결정 시점 |
| --- | --- | --- | --- | --- |
| I-1 | 취약 앱 위치 | 4장은 "격리된 별도 앱/컨테이너"인데 8장 저장소 구조에 그 자리가 없다. `experiment_runs.defense_enabled=false` 실험을 어디서 돌리는지도 정해지지 않았다. | **반영됨:** `experiments/api-security/vulnerable-api/` 별도 앱(인증은 동일, 인가만 제거, 메모리 저장소, 루프백 전용) → [ADR-0002](../adr/0002-isolated-vulnerable-lab.md) | ✅ 2026-11-20 |
| I-2 | 실험 결과 스키마 | 7장은 "모델 반응과 도구 실행 결과를 따로 기록"하라고 하지만 `experiment_runs`에는 `success` 하나뿐이다. AI-04의 "감사 기록"을 남길 테이블도 없다. | **반영됨(V6):** `experiment_runs`(target, model_complied, action_executed, policy_code, http_status) + `ai_tool_calls`(인자 SHA-256) | ✅ 2026-11-20 |
| I-3 | `security_events` 컬럼 | 5장 원칙(판정 코드 중심 기록)에 비해 IP, 대상 리소스, 판정 코드 컬럼이 없다. | **반영됨(V2):** `ip_address`, `resource_type`, `resource_id`, `reason_code` 추가 | ✅ 2026-11-20 |
| I-4 | CSRF 토큰 전달 | 세션 + CSRF로 정했지만 Swagger·k6·테스트 클라이언트가 토큰을 받는 방법이 API 명세에 없다. | **반영됨:** `XSRF-TOKEN` 쿠키 → `X-XSRF-TOKEN` 헤더 + `GET /api/v1/auth/csrf`, 로그인 시 재발급 → [ADR-0001](../adr/0001-session-cookie-auth.md) | ✅ 2026-11-20 |
| I-5 | `email_digest`, 보존 기간 | 단순 해시는 이메일 사전 대입으로 되돌릴 수 있다. 보존 기간(7~30일) 삭제 작업이 일정에 없다. | **반영됨:** HMAC-SHA256(`LOGIN_DIGEST_SECRET`), 다이제스트 형식 CHECK, 30일 보존 정리 작업(매일 03:30) | ✅ 2026-11-20 |
| I-6 | "로컬 실험" 권한 | `/scenarios`, `/experiments`의 "관리자 + 로컬 실험" 조건을 어떻게 강제할지 없다. | **반영됨:** `@Profile("lab")` 컨트롤러 + `/api/v1/scenarios/**`·`/api/v1/experiments/**` ADMIN 규칙 | ✅ 2026-11-20 |
| I-7 | 비밀번호 인코더 | Spring Security의 `Argon2PasswordEncoder`는 BouncyCastle 의존성이 필요하다. | **반영됨:** BouncyCastle 1.87 + Argon2id 기본 `DelegatingPasswordEncoder` → [ADR-0003](../adr/0003-password-encoder.md) | ✅ 2026-11-20 |
| I-8 | 오탐률 기준선 | 오탐률은 "차단된 정상 요청 ÷ 정상 요청"인데 SEC-03/04에 정상 트래픽 정의가 없다. | **부분 반영:** 러너가 공격마다 다른 사용자의 정상 요청을 함께 보내 오탐률을 잰다(SEC-01~04 오탐 0%). 부하 상황 혼합은 k6로 | 8주차 측정 |
| I-9 | 라이선스 | 저장소 구조에 `LICENSE`가 있지만 종류가 없다. | MIT 등으로 결정 | 첫 공개 커밋 전 |
