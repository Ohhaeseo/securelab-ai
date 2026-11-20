# API 명세 (초안)

> 상태: 초안. 구현 후에는 springdoc Swagger UI를 기준으로 하고, 이 문서는 공통 규칙과 설계 의도를 남긴다.

## 1. 공통 규칙

| 항목 | 규칙 |
| --- | --- |
| 기본 경로 | `/api/v1` |
| 인증 | 세션 쿠키 (HttpOnly, SameSite). HTTPS 배포 시 Secure 강제 |
| CSRF | 상태 변경 요청(POST/PATCH/DELETE)에 필요. `XSRF-TOKEN` 쿠키 값을 `X-XSRF-TOKEN` 헤더로 보낸다. 쿠키는 `GET /auth/csrf`나 아무 응답에서 받을 수 있고, 로그인하면 새 값으로 바뀐다 |
| 요청 제한 | `/api/**` — 로그인 사용자 ID별 120회/분, 익명 IP별 60회/분 (ADR-0004) |
| 요청 추적 | `X-Request-Id` 헤더. 없으면 서버가 발급하고 응답·로그·보안 이벤트에 같은 값을 남김 |
| 형식 | JSON (UTF-8) |

### 상태 코드

| 상황 | 코드 |
| --- | --- |
| 인증 없음 | 401 |
| 인증됐지만 권한 없음 | 403 |
| 입력 검증 실패 | 400 |
| 리소스 없음 | 404 |
| 중복 (이메일 등) | 409 |
| 요청 제한 초과 | 429 + `Retry-After` |
| 서버 오류 | 500 (스택 트레이스·쿼리 비공개) |

### 에러 응답 형식

```json
{
  "status": 403,
  "code": "ACCESS_DENIED",
  "message": "접근 권한이 없습니다.",
  "requestId": "req-local-0001"
}
```

### 에러 코드 (초안)

| 코드 | 상태 | 설명 |
| --- | --- | --- |
| `UNAUTHENTICATED` | 401 | 로그인 필요 |
| `INVALID_CREDENTIALS` | 401 | 로그인 실패 (계정 없음·비밀번호 틀림을 구분하지 않음) |
| `ACCESS_DENIED` | 403 | 권한 없음 |
| `CSRF_INVALID` | 403 | CSRF 토큰 없음 또는 불일치 |
| `VALIDATION_FAILED` | 400 | 입력 값 오류 (`errors[]`에 필드별 사유, 거부된 값은 돌려주지 않음) |
| `MALFORMED_REQUEST` | 400 | JSON 본문을 읽을 수 없음 |
| `RESOURCE_NOT_FOUND` | 404 | 리소스 없음 |
| `METHOD_NOT_ALLOWED` | 405 | 지원하지 않는 메서드 |
| `EMAIL_ALREADY_EXISTS` | 409 | 이메일 중복 |
| `UNSUPPORTED_MEDIA_TYPE` | 415 | 지원하지 않는 콘텐츠 형식 |
| `RATE_LIMITED` | 429 | 요청 한도 초과 (`Retry-After`, `X-RateLimit-Limit`·`X-RateLimit-Remaining` 헤더) |
| `LOGIN_TEMPORARILY_BLOCKED` | 429 | 로그인 실패 누적으로 일시 제한 (`Retry-After`). 차단 중에는 맞는 비밀번호도 거부 |
| `INTERNAL_ERROR` | 500 | 서버 오류 |

## 2. 엔드포인트

### 인증·사용자

| 메서드 | 경로 | 권한 | 설명 |
| --- | --- | --- | --- |
| GET | `/auth/csrf` | 익명 | CSRF 토큰 쿠키 발급 (204) |
| POST | `/auth/signup` | 익명 | 가입, 이메일 중복 검사 (201 + `Location`) |
| POST | `/auth/login` | 익명 | 로그인 — 세션 ID 교체, CSRF 토큰 재발급 |
| POST | `/auth/logout` | 로그인 | 세션 무효화 (204) |
| GET | `/users/me` | 로그인 | 내 프로필 |
| GET | `/users/{id}` | 본인 또는 관리자 | 프로필 접근 제어 확인 |

### 문서

| 메서드 | 경로 | 권한 | 설명 |
| --- | --- | --- | --- |
| POST | `/documents` | 로그인 | 문서 작성 (201, 소유자 = 인증 주체, `visibility` 생략 시 PRIVATE) |
| GET | `/documents?page=0&size=20` | 로그인 | 접근 가능한 문서 목록 (본인 + 공개, 최신순, `size` 최대 100, 본문 제외) |
| GET | `/documents/{id}` | 소유자 또는 공개 문서 | 문서 조회 |
| PATCH | `/documents/{id}` | 소유자 | 부분 수정 (`title`, `content`, `visibility`) |
| DELETE | `/documents/{id}` | 소유자 | 문서 삭제 (204) |

> 관리자도 남의 비공개 문서를 읽거나 바꿀 수 없다 (최소 권한).

### 보안 관리 (관리자)

| 메서드 | 경로 | 권한 | 설명 |
| --- | --- | --- | --- |
| GET | `/security/events?type=&page=0&size=50` | 관리자 | 보안 이벤트 목록 (`type`: `ACCESS_DENIED`·`CSRF_REJECTED`, 최신순, `size` 최대 200) |
| GET | `/security/metrics?hours=24` | 관리자 | 기간 내 차단 이벤트 유형별 집계 (`hours` 1~720) |

### 실험 (관리자 + `lab` 프로필)

| 메서드 | 경로 | 권한 | 설명 |
| --- | --- | --- | --- |
| GET | `/scenarios` | 관리자 + 로컬 실험 | 시나리오 목록 |
| POST | `/experiments` | 관리자 + 로컬 실험 | 허가된 실험 실행 |
| GET | `/experiments/{id}` | 관리자 + 로컬 실험 | 결과 조회 |

### AI

| 메서드 | 경로 | 권한 | 설명 |
| --- | --- | --- | --- |
| POST | `/ai/sessions` | 로그인 | AI 세션 생성 (201) |
| POST | `/ai/sessions/{id}/messages` | 세션 소유자 | 문서 기반 AI 질문. 본문 `{"question", "documentIds"?}` |

- `documentIds`를 주면 그 문서만 참고한다. **하나라도 읽을 권한이 없으면 모델을 부르기 전에 403** (AI-03). 최대 5개.
- 생략하면 질문에서 뽑은 키워드로 **읽을 수 있는 문서(본인 + 공개) 안에서만** 검색한다 (최대 5건, 건당 2,000자).
- 응답: `answer`(모델 출력 — 텍스트로만 표시), `citations`(모델에 전달한 문서), `toolCalls`(모델이 제안한 도구와 처리 결과), `model`.
- 모델이 제안한 도구는 서버 정책(`ToolExecutionPolicyService`)이 판정한다. `toolCalls[].status`: `EXECUTED`(현재 사용자 권한으로 실행, `output` 포함) / `DENIED`(`reasonCode`: `NOT_IN_ALLOWLIST`, `SCHEMA_INVALID`, `ROLE_REQUIRED`, `NOT_PERMITTED`, `NOT_FOUND`, `TOO_MANY_TOOL_CALLS`). 허용 도구: `get_document(documentId)`, `list_documents(limit?)`, `list_security_events(limit?)` — 관리자 전용. ADR-0005.
- 질문·답변·문서 원문은 저장하지 않는다. 도구 제안은 `ai_tool_calls`에 이름과 인자 해시만 남는다.

> AI 도구 실행은 공개 API(`/ai/tools/execute`)로 열지 않는다. 서버 내부 `ToolExecutionPolicyService`로만 실행하고, 관리자 실험 API도 이 정책을 건너뛸 수 없다.

## 3. 미정 사항

| 항목 | 내용 | 관련 |
| --- | --- | --- |
| ~~CSRF 토큰 전달~~ | **결정:** `XSRF-TOKEN` 쿠키 → `X-XSRF-TOKEN` 헤더, `GET /auth/csrf` | I-4, ADR-0001 |
| ~~로그인 요청 형식~~ | **결정:** JSON 바디 `{"email","password"}` → `LoginService` | ADR-0001 |
| 타인 문서 응답 코드 | 기획서는 403. 403은 "문서가 존재함"을 알려 주므로 ID 열거에 쓰일 수 있다. 404로 숨길지 검토하되, 바꾸면 SEC-01 기대값도 함께 바꾼다 | SEC-01 |
| 실험 API 노출 방식 | `lab` 프로필에서만 빈 등록, 기본 프로필에서는 404 | I-6 |
