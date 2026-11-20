# API 명세 (초안)

> 상태: 초안. 구현 후에는 springdoc Swagger UI를 기준으로 하고, 이 문서는 공통 규칙과 설계 의도를 남긴다.

## 1. 공통 규칙

| 항목 | 규칙 |
| --- | --- |
| 기본 경로 | `/api/v1` |
| 인증 | 세션 쿠키 (HttpOnly, SameSite). HTTPS 배포 시 Secure 강제 |
| CSRF | 상태 변경 요청(POST/PATCH/DELETE)에 CSRF 토큰 필요. 전달 방식은 아래 "미정" 참고 |
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
| `ACCESS_DENIED` | 403 | 권한 없음 |
| `VALIDATION_FAILED` | 400 | 입력 값 오류 |
| `RESOURCE_NOT_FOUND` | 404 | 리소스 없음 |
| `EMAIL_ALREADY_EXISTS` | 409 | 이메일 중복 |
| `RATE_LIMITED` | 429 | 요청 한도 초과 |
| `LOGIN_TEMPORARILY_BLOCKED` | 429 | 로그인 실패 누적으로 일시 제한 |
| `INTERNAL_ERROR` | 500 | 서버 오류 |

## 2. 엔드포인트

### 인증·사용자

| 메서드 | 경로 | 권한 | 설명 |
| --- | --- | --- | --- |
| POST | `/auth/signup` | 익명 | 가입, 이메일 중복 검사 |
| POST | `/auth/login` | 익명 | 로그인, 실패 기록 |
| POST | `/auth/logout` | 로그인 | 세션 무효화 |
| GET | `/users/me` | 로그인 | 내 프로필 |
| GET | `/users/{id}` | 본인 또는 관리자 | 프로필 접근 제어 확인 |

### 문서

| 메서드 | 경로 | 권한 | 설명 |
| --- | --- | --- | --- |
| POST | `/documents` | 로그인 | 문서 작성 |
| GET | `/documents` | 로그인 | 접근 가능한 문서 목록 |
| GET | `/documents/{id}` | 소유자 또는 공개 문서 | 문서 조회 |
| PATCH | `/documents/{id}` | 소유자 | 문서 수정 |
| DELETE | `/documents/{id}` | 소유자 | 문서 삭제 |

### 보안 관리 (관리자)

| 메서드 | 경로 | 권한 | 설명 |
| --- | --- | --- | --- |
| GET | `/security/events` | 관리자 | 보안 이벤트 목록 |
| GET | `/security/metrics` | 관리자 | 차단·오탐 등 지표 |

### 실험 (관리자 + `lab` 프로필)

| 메서드 | 경로 | 권한 | 설명 |
| --- | --- | --- | --- |
| GET | `/scenarios` | 관리자 + 로컬 실험 | 시나리오 목록 |
| POST | `/experiments` | 관리자 + 로컬 실험 | 허가된 실험 실행 |
| GET | `/experiments/{id}` | 관리자 + 로컬 실험 | 결과 조회 |

### AI

| 메서드 | 경로 | 권한 | 설명 |
| --- | --- | --- | --- |
| POST | `/ai/sessions` | 로그인 | AI 세션 생성 |
| POST | `/ai/sessions/{id}/messages` | 세션 소유자 | 문서 기반 AI 질문 |

> AI 도구 실행은 공개 API(`/ai/tools/execute`)로 열지 않는다. 서버 내부 `ToolExecutionPolicyService`로만 실행하고, 관리자 실험 API도 이 정책을 건너뛸 수 없다.

## 3. 미정 사항

| 항목 | 내용 | 관련 |
| --- | --- | --- |
| CSRF 토큰 전달 | 권장: `CookieCsrfTokenRepository`(`XSRF-TOKEN` 쿠키 → `X-XSRF-TOKEN` 헤더) + `GET /auth/csrf` 추가 | I-4, ADR-0001 |
| 로그인 요청 형식 | JSON 바디(커스텀 컨트롤러) 또는 Spring Security form login | 11/26 결정 |
| 타인 문서 응답 코드 | 기획서는 403. 403은 "문서가 존재함"을 알려 주므로 ID 열거에 쓰일 수 있다. 404로 숨길지 검토하되, 바꾸면 SEC-01 기대값도 함께 바꾼다 | SEC-01 |
| 실험 API 노출 방식 | `lab` 프로필에서만 빈 등록, 기본 프로필에서는 404 | I-6 |
