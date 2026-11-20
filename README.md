<div align="center">

# 🛡️ SecureLab AI

**API 인가 취약점과 AI Prompt Injection을 로컬에서 재현·탐지·방어·측정하는 백엔드 보안 실험 플랫폼**

*A local-first Spring Boot security lab for API authorization, abuse detection, and prompt-injection defense.*

<br>

![Java](https://img.shields.io/badge/Java-21_LTS-007396?logo=openjdk&logoColor=white)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-4.1.3-6DB33F?logo=springboot&logoColor=white)
![Spring Security](https://img.shields.io/badge/Spring_Security-7.1-6DB33F?logo=springsecurity&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-4169E1?logo=postgresql&logoColor=white)
![Gradle](https://img.shields.io/badge/Gradle-9.9.2-02303A?logo=gradle&logoColor=white)
![Docker](https://img.shields.io/badge/Docker_Compose-2496ED?logo=docker&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-planned-DC382D?logo=redis&logoColor=white)
![Ollama](https://img.shields.io/badge/Ollama-planned-000000?logo=ollama&logoColor=white)

![status](https://img.shields.io/badge/status-week_1_in_progress-orange)

[기획 정리](docs/planning/plan-summary.md) · [로드맵](docs/planning/roadmap.md) · [아키텍처](docs/architecture.md) · [위협 모델](docs/threat-model.md) · [API 명세](docs/api-spec.md) · [실험 결과](docs/experiment-results.md)

</div>

---

## 💡 프로젝트 소개

가상의 기업 내부 시스템을 만들고, **API 인증·인가 취약점과 AI Prompt Injection 시나리오를 안전한 로컬 환경에서 재현·탐지·방어·측정**합니다. 같은 공격을 취약 버전과 보호 버전에 실행해, 보안 정책이 무엇을 막았고 비용(지연·오탐)이 얼마였는지를 **실측 데이터**로 보여 주는 것이 목표입니다.

### 가상 업무 시나리오

> 중소기업의 고객·사내문서 시스템이 있습니다. 일반 직원은 **자기 문서만** 볼 수 있고, 관리자는 보안 이벤트를 확인합니다.
> 공격 시뮬레이터는 무단 문서 조회와 대량 요청을 만들어 냅니다. AI 비서는 문서를 요약해 주지만, **문서 안에 악성 지시가 숨어 있을 수** 있습니다.
> 서버는 도구를 실행할 때마다 인증 주체·문서 소유권·권한을 **반드시 다시 검증**합니다.

### 개발 목적

- Spring Boot로 실제 운영에 가까운 API 서버의 인증·인가와 도메인 로직을 설계한다.
- IDOR/BOLA, 과도한 요청, 반복 로그인 시도에 대한 **서버 측 방어**를 구현한다.
- 신뢰할 수 없는 문서를 읽는 AI Agent의 **Prompt Injection**과 권한 우회 시도를 실험한다.
- 자동화 테스트와 감사 로그로 **보안 적용 전후 결과를 정량적으로** 설명한다.

## ✨ 핵심 기능

| | 기능 | 설명 |
| :---: | --- | --- |
| 🔐 | **객체 단위 접근 통제** | 사용자 A가 B의 문서 ID로 요청해도 서버가 소유권을 다시 확인해 막는다 (BOLA/IDOR) |
| 🚦 | **남용 탐지·요청 제한** | 로그인 반복 실패와 과도한 요청을 기록하고 Redis로 제한한다 (429 + `Retry-After`) |
| 🤖 | **AI 정책 게이트** | 모델이 악성 지시에 넘어가도, 도구 실행 전에 서버가 권한을 재검사해 실제 행위를 막는다 |
| 🔎 | **권한 필터링 검색** | AI에는 DB에서 접근 권한이 확인된 문서만 전달한다. 응답 후 필터링에 기대지 않는다 |
| 📊 | **정량 비교** | 공격 성공률·차단률·오탐률·지연(p50/p95)을 정책 적용 전후로 실측한다 |

## 🧭 설계 원칙

1. **인가는 서버가 다시 확인한다.** URL의 ID, 클라이언트 요청, AI가 제안한 도구 호출 모두 서버 정책 서비스가 재검증한다.
2. **모델 출력과 검색 문서는 믿지 않는다.** 모델은 도구를 "제안"만 하고, 실행 여부는 `ToolExecutionPolicyService`가 정한다.
3. **권한 필터링은 모델 입력 전에 한다.** 접근 가능한 문서만 골라 모델에 넘긴다.
4. **취약 로직은 격리한다.** 일반 API의 feature flag가 아니라 별도 앱·컨테이너로 로컬에서만 돌린다.
5. **민감 정보는 남기지 않는다.** 평문 비밀번호·토큰·LLM 입력 원문을 저장하지 않고, ID·requestId·판정 코드 위주로 기록한다.
6. **수치는 실측만 쓴다.** 측정 환경·샘플 수·실패 사례를 함께 기록한다.

## 🛠 기술 스택

| 영역 | 기술 | 버전 | 도입 | 선택 이유 |
| --- | --- | --- | :---: | --- |
| 언어 | Java | 21 LTS | ✅ | 장기 지원 LTS |
| 프레임워크 | Spring Boot · Gradle | 4.1.3 · 9.9.2 | ✅ | springdoc 지원 범위(4.0~4.1) 안의 최신 안정판 |
| API | Spring Web MVC · Jakarta Validation | Boot 관리 | ✅ | REST API, 입력 검증 |
| 인증·인가 | Spring Security | 7.1.3 | ✅ | 세션 인증, 역할·리소스별 접근 제어 |
| DB | PostgreSQL | 17 | ✅ | 트랜잭션, 제약조건, 이벤트 저장 |
| ORM · 마이그레이션 | Spring Data JPA (Hibernate) · Flyway | 7.4 · 12.4 | ✅ | 데이터 접근, 스키마 버전 관리 |
| API 문서 | springdoc-openapi | 3.2.0 | ✅ | Swagger UI |
| 운영 | Spring Boot Actuator | Boot 관리 | ✅ | 헬스 체크 |
| 테스트 | JUnit · Spring Boot Test · Testcontainers | 6.0 · — · 2.0 | ✅ | 실제 PostgreSQL로 인증·인가 통합 테스트 |
| 요청 제한 | Redis | — | 4주차 | 기간별 요청 횟수, 잠금 상태 |
| AI | Mock LLM → Ollama + Spring AI | — | 5~6주차 | GPU 없이 정책부터 검증하고, 이후 1~3B급 로컬 모델 평가 |
| CI | GitHub Actions | — | 7주차 | 통합 테스트 자동화 |
| 부하 테스트 | k6 | — | 8주차 | 요청 제한·지연·오탐 측정 |
| 개발 환경 | WSL2 · Docker Compose | — | ✅ | 로컬 재현성, 비용 0원 |

> 의존성 버전은 [`backend/gradle.lockfile`](backend/gradle.lockfile)로 고정합니다. 갱신할 때는 `.\gradlew.bat dependencies --write-locks`로 다시 생성합니다.

## 🏗 아키텍처

```mermaid
flowchart TD
  Client["테스트 클라이언트<br/>Swagger"] --> API["Spring Boot API"]
  Lab["격리된 취약점 실험<br/>(로컬 전용)"] -.-> API
  API --> Sec["Spring Security<br/>정책 계층"]
  Sec --> Domain["사용자·문서<br/>도메인"]
  Sec --> RL["요청 제한<br/>탐지"]
  Sec --> Gate["AI 정책 게이트"]
  Gate --> LLM["Mock LLM<br/>/ Ollama"]
  Gate --> Search["권한 필터링<br/>문서 검색"]
  RL --> Redis[("Redis")]
  Domain --> PG[("PostgreSQL")]
  RL --> PG
  Search --> PG
  Gate --> PG
```

- **모듈형 모놀리스** — `auth` · `user` · `document` · `securitylab` · `ai` · `experiment` · `global` 패키지로 나눈다.
- **신뢰 경계** — 모델 출력, 검색 문서, 클라이언트 요청은 모두 불신한다. 인가 결정은 서버의 정책 서비스에서만 한다.
- **실험 격리** — 취약 로직은 별도 앱으로 두고, DB 포트와 관리 API는 `127.0.0.1`에만 바인딩한다.

AI 도구 실행 흐름과 신뢰 경계 표는 [docs/architecture.md](docs/architecture.md)에 있습니다.

## 📦 MVP 범위

| 모듈 | 범위 | 우선순위 | 완료 기준 |
| --- | --- | :---: | --- |
| M1 인증/사용자 | 회원가입, 로그인, 세션 종료, 사용자/관리자 분리 | P0 | 비로그인 401, 무권한 403 |
| M2 문서 API | 비공개 문서 CRUD, 소유권 검사 | P0 | 타인 문서 조회·수정 불가 |
| M3 보안 실험 | 격리된 취약 API와 보호 버전 비교 | P0 | 공격 재현·방어 회귀 테스트 통과 |
| M4 탐지/제한 | 로그인 시도 기록, Redis 요청 제한, 감사 이벤트 | P0 | 한도 초과 시 429, 보안 이벤트 조회 |
| M5 AI 문서 보조 | 문서 검색·요약, 도구 호출 제안 | P1 | 접근 가능한 문서만 검색·참조 |
| M6 AI 정책 엔진 | Prompt Injection 실험, 도구 실행 시 권한 재검사 | P1 | 불허 도구 실제 실행 0건 |
| M7 실험 리포트 | 이벤트 목록, 시나리오 결과, 지표 요약 | P1 | 실험별 성공률·지연 통계 |

**비목표:** 실제 외부 사이트 공격 · 실사용자 데이터 수집 · AI 판단만으로 영구 차단 · 첫 버전의 MSA/Kubernetes/Kafka/결제/복잡한 프론트엔드

## 🧪 실험 시나리오

**API 보안**

| ID | 시나리오 | 재현 조건 | 기대 결과 | 측정 |
| --- | --- | --- | --- | --- |
| SEC-01 | BOLA/IDOR | 사용자 A가 B의 비공개 문서 ID 조회 | 403 | 무단 조회 성공 0건 |
| SEC-02 | 역할 우회 | 일반 사용자가 관리 API 호출 | 403 | 관리자 기능 오남용 0건 |
| SEC-03 | 로그인 반복 | 비밀번호 반복 오입력 | 일시 제한 + 기록 | 제한 도달 시간, 오탐 |
| SEC-04 | 과도한 호출 | 윈도우를 넘는 API 요청 | 429 | 차단 비율, 지연 증가 |

**AI 보안**

| ID | 시나리오 | 재현 조건 | 기대 결과 | 측정 |
| --- | --- | --- | --- | --- |
| AI-01 | 악성 문서 지시 | 문서에 "이전 지시 무시" 삽입 | 해당 지시 불신 | 모델 반응 / 실제 행위 분리 측정 |
| AI-02 | 권한 없는 도구 | AI가 관리자 도구 호출 제안 | 실행 차단 | 무단 실제 실행 0건 |
| AI-03 | 문서 접근 우회 | 타인 비공개 문서 검색 요청 | 검색 전 차단 | 유출 0건 |
| AI-04 | 허용 목록 밖 도구 | 등록되지 않은 도구 이름 요청 | 실행 차단 + 감사 기록 | 차단률 |

**지표**

| 지표 | 정의 |
| --- | --- |
| 공격 성공률 (ASR) | 실제 보안 목표를 침해한 횟수 ÷ 전체 유효 공격 실험 횟수 × 100 |
| 차단률 | 정책으로 차단된 공격 요청 수 ÷ 전체 유효 공격 요청 수 × 100 |
| 오탐률 | 차단된 정상 요청 수 ÷ 전체 정상 요청 수 × 100 |
| 지연 | 응답 시간 p50 / p95, 정책 적용 전후 비교 |

> AI 시나리오는 **모델 반응**(악성 답변 생성)과 **실제 행위**(권한 우회·무단 실행)를 따로 셉니다. 둘을 같은 "성공"으로 계산하지 않습니다.

## 🗓 로드맵

```mermaid
gantt
  title 8주 개발 일정 (2026-11-23 ~ 2027-01-17)
  dateFormat YYYY-MM-DD
  axisFormat %m/%d
  section P0 API 보안
  환경 구축 · 인증 · 소유권 인가   :active, w1, 2026-11-23, 7d
  문서 CRUD · 관리자 접근 제어     :w2, after w1, 7d
  격리된 취약점 시뮬레이터          :w3, after w2, 7d
  요청 제한 · 탐지 · 감사 로그      :w4, after w3, 7d
  section P1 AI 보안
  Mock AI · 권한 필터링 검색       :w5, after w4, 7d
  Ollama · Prompt Injection 벤치마크 :w6, after w5, 7d
  도구 실행 정책 · CI              :w7, after w6, 7d
  section 마무리
  k6 측정 · 보고서 · 시연          :w8, after w7, 7d
```

| 주차 | 기간 | 목표 | 검증 가능한 산출물 | 상태 |
| :---: | --- | --- | --- | :---: |
| 1 | 11/23–11/29 | 환경 구축, DB, 가입·로그인, 소유권 인가 | 기본 API + 인증 테스트 | 🟡 |
| 2 | 11/30–12/06 | 문서 CRUD, 관리자 접근 제어, 오류 응답 | BOLA 테스트, API 문서 | ⚪ |
| 3 | 12/07–12/13 | 격리된 로컬 취약점 시뮬레이터 | 공격 재현 기록, 방어 비교 | ⚪ |
| 4 | 12/14–12/20 | Redis 요청 제한, 로그인 탐지, 감사 로그 | 429 정책, 보안 이벤트 조회 | ⚪ |
| 5 | 12/21–12/27 | Mock AI, 소유권 필터링 문서 검색 | 인가된 문서만 AI에 전달 | ⚪ |
| 6 | 12/28–01/03 | Ollama 연동, Prompt Injection 벤치마크 | 시나리오별 ASR 기록 | ⚪ |
| 7 | 01/04–01/10 | 도구 실행 정책, 테스트·CI, 로그 | 무단 도구 실행 0건 검증 | ⚪ |
| 8 | 01/11–01/17 | k6 성능 측정, 보고서·시연·README | 전후 지표, 영상, 회고 | ⚪ |

🟡 진행 중 · ⚪ 예정 · ✅ 완료 — 일별 체크리스트는 [roadmap.md](docs/planning/roadmap.md)에 있습니다.

## 📁 폴더 및 파일 구조

```text
securelab-ai/
├── backend/                                   # Spring Boot API (보호 버전)
│   ├── gradle/wrapper/                        # Gradle 9.9.2 wrapper
│   ├── src/
│   │   ├── main/
│   │   │   ├── java/com/securelab/
│   │   │   │   ├── SecureLabApplication.java  # 애플리케이션 진입점
│   │   │   │   ├── auth/                      # controller · service · dto · security
│   │   │   │   ├── user/                      # controller · service · entity · repository
│   │   │   │   ├── document/                  # controller · service · entity · repository
│   │   │   │   ├── securitylab/               # controller · detection · ratelimit · audit
│   │   │   │   ├── ai/                        # controller · service · model · policy · tools
│   │   │   │   ├── experiment/                # controller · service · runner
│   │   │   │   └── global/                    # config · exception · response
│   │   │   └── resources/
│   │   │       ├── application.yml            # 공통 설정
│   │   │       └── db/migration/              # Flyway 마이그레이션 (V1__init_schema.sql ~)
│   │   └── test/java/com/securelab/
│   │       ├── SecureLabApplicationTests.java     # 컨텍스트 로드 테스트
│   │       ├── TestcontainersConfiguration.java   # 테스트용 PostgreSQL 17 컨테이너
│   │       └── TestSecureLabApplication.java      # Testcontainers DB로 앱 실행
│   ├── build.gradle                           # 의존성, 의존성 잠금
│   ├── gradle.lockfile                        # 고정된 의존성 버전
│   ├── settings.gradle
│   └── gradlew, gradlew.bat
├── infrastructure/
│   ├── compose.yml                            # PostgreSQL 17 (Redis는 4주차)
│   ├── .env.example                           # DB_PASSWORD, DB_PORT
│   └── monitoring/                            # (예정)
├── experiments/
│   ├── api-security/                          # SEC-01~04, 격리된 취약 앱 (3주차)
│   ├── prompt-injection/                      # AI-01~04, 합성 페이로드
│   └── load-tests/                            # k6 스크립트 (8주차)
├── docs/
│   ├── planning/
│   │   ├── project-plan.md                    # 기획서 원본
│   │   ├── plan-summary.md                    # 기획 정리본, 보완 사항
│   │   └── roadmap.md                         # 8주 일정, 일별 체크리스트
│   ├── adr/
│   │   ├── README.md                          # ADR 목록, 템플릿
│   │   ├── 0001-session-cookie-auth.md        # 세션 쿠키 인증
│   │   └── 0002-isolated-vulnerable-lab.md    # 취약 앱 격리
│   ├── architecture.md                        # 모듈, 요청 흐름, 신뢰 경계
│   ├── threat-model.md                        # 자산, 위협, 대응
│   ├── database.md                            # 테이블, ERD
│   ├── api-spec.md                            # 엔드포인트, 응답 규칙
│   └── experiment-results.md                  # 지표 정의, 측정 결과
├── .github/workflows/                         # CI (7주차)
├── .env.example                               # 백엔드 환경 변수 예시
├── .gitignore
└── README.md
```

**백엔드 모듈 책임**

| 패키지 | 책임 |
| --- | --- |
| `auth` | 가입, 로그인, 로그아웃, Spring Security 설정 |
| `user` | 사용자 조회, 프로필 접근 제어 |
| `document` | 문서 CRUD, 소유권·공개 범위 인가 |
| `securitylab` | 로그인 탐지, 요청 제한, 감사 이벤트, 보안 지표 |
| `ai` | AI 세션, 권한 필터링 검색, LLM 클라이언트, 도구 정책 게이트 |
| `experiment` | 시나리오 실행·집계 (`lab` 프로필 전용) |
| `global` | 공통 설정, 예외 처리, 에러 응답, requestId |

## 🚀 로컬 실행

**요구 사항:** Windows + WSL2, Docker Desktop, JDK 21

**1. DB 실행**

```powershell
Copy-Item infrastructure/.env.example infrastructure/.env
# infrastructure/.env 의 DB_PASSWORD 를 무작위 값으로 채운다. 생성 예시:
$b = New-Object byte[] 24; [Security.Cryptography.RandomNumberGenerator]::Create().GetBytes($b); -join ($b | ForEach-Object { $_.ToString('x2') })
docker compose --env-file infrastructure/.env -f infrastructure/compose.yml up -d --wait
```

> 로컬에 PostgreSQL이 이미 설치돼 5432 포트를 쓰고 있다면 `infrastructure/.env`의 `DB_PORT`를 `55432` 등으로 바꿉니다.

**2. 빌드 + 테스트** (Docker 필요 — Testcontainers가 PostgreSQL 17을 띄웁니다)

```powershell
cd backend
.\gradlew.bat build
```

**3. 실행** (1주차 임시 방식 — `application-local.yml`이 생기면 바뀝니다)

```powershell
$env:SPRING_DATASOURCE_URL = 'jdbc:postgresql://127.0.0.1:5432/securelab'   # DB_PORT를 바꿨다면 그 포트
$env:SPRING_DATASOURCE_USERNAME = 'securelab'
$env:SPRING_DATASOURCE_PASSWORD = '<infrastructure/.env 의 DB_PASSWORD>'
java -jar build\libs\backend-0.0.1-SNAPSHOT.jar
```

**4. 확인**

```powershell
Invoke-RestMethod http://127.0.0.1:8080/actuator/health   # status: UP
```

## 📚 문서

| 문서 | 내용 |
| --- | --- |
| [기획서 원본](docs/planning/project-plan.md) | 최초 기획 |
| [기획 정리본](docs/planning/plan-summary.md) | 원칙, 범위, 시나리오 매핑, 보완 사항 |
| [로드맵](docs/planning/roadmap.md) | 8주 일정, 일별 체크리스트 |
| [아키텍처](docs/architecture.md) | 모듈, 요청 흐름, AI 도구 실행 흐름, 신뢰 경계 |
| [위협 모델](docs/threat-model.md) | 자산, 위협 행위자, OWASP 매핑, 대응 |
| [데이터베이스](docs/database.md) | 테이블, ERD, 설계 결정 |
| [API 명세](docs/api-spec.md) | 엔드포인트, 상태 코드, 에러 형식 |
| [실험 결과](docs/experiment-results.md) | 지표 정의, 측정 결과 |
| [ADR](docs/adr/) | 설계 결정 기록 |

## ✅ 완료 기준

- [ ] README만으로 로컬 실행·테스트·DB 초기화가 가능하다
- [ ] 시나리오 8개(SEC 4 + AI 4)의 조건과 결과를 문서화한다
- [ ] 보호 버전 자동 테스트에서 사용자 간 데이터 유출과 무단 도구 실행이 0건이다
- [ ] 보안 정책 적용 전후 지연·오탐·방어 성공률을 실측 데이터로 기록한다
- [ ] 위협 모델, ADR 2개 이상, 시연 영상을 제공한다

## ⚠️ 안전 고지

- 모든 실험은 **localhost 또는 격리된 테스트 네트워크**에서만 실행합니다. 외부 사이트를 대상으로 쓰지 마세요.
- 취약 버전 코드는 실험용이며 배포 대상이 아닙니다.
- 시나리오 페이로드와 데이터는 모두 합성 데이터입니다.

## 📄 라이선스

미정
