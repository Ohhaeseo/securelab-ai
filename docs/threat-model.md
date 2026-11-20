# 위협 모델

> 상태: 초안. 각 위협은 실험 시나리오(SEC-xx, AI-xx)로 검증한다. 구현하면서 갱신한다.
> 참고: [OWASP API Security Top 10 (2023)](https://owasp.org/API-Security/editions/2023/en/0x11-t10/), [OWASP Top 10 for LLM Applications](https://genai.owasp.org/llm-top-10/)

## 1. 범위와 가정

- 대상: 가상 중소기업의 사용자·문서 API와 문서 기반 AI 비서.
- 실행 환경: 단일 로컬 머신(Windows + WSL2 + Docker). 외부에 공개하지 않는다.
- 모든 데이터는 합성 데이터이며 실사용자 데이터는 없다.
- 관리자 계정은 신뢰하되, 관리자 API도 역할과 개별 작업 권한을 함께 확인한다.

## 2. 보호 자산

| 자산 | 설명 | 침해 시 영향 |
| --- | --- | --- |
| 비공개 문서 | 직원 개인 문서 | 사용자 간 데이터 유출 |
| 계정 자격 증명 | 이메일, 비밀번호 해시 | 계정 탈취 |
| 세션 | 로그인 세션 쿠키 | 다른 사용자로 행동 |
| 관리자 기능 | 보안 이벤트·지표 조회, 실험 관리 | 권한 상승 |
| 보안 이벤트 | 감사 기록 | 공격 흔적 은폐, 개인정보 노출 |
| AI 도구 | AI가 제안할 수 있는 서버 작업 | 사용자 대신 무단 작업 실행 |

## 3. 위협 행위자

| 행위자 | 능력 |
| --- | --- |
| 익명 사용자 | 공개 엔드포인트 호출, 로그인 대입 |
| 악의적인 일반 사용자 | 정상 로그인 후 ID 변조, 관리 API 호출, 대량 요청 |
| 악성 문서 작성자 | 문서에 지시문을 심어 다른 사용자의 AI 세션을 조종 시도 (간접 Prompt Injection) |
| 모델 자체 | 인젝션에 넘어가거나 잘못된 도구 호출을 제안 (신뢰하지 않음) |

## 4. 위협과 대응

| ID | 위협 | OWASP | 대응 | 검증 |
| --- | --- | --- | --- | --- |
| T1 | 문서 ID를 바꿔 타인 문서 조회·수정 | API1:2023 BOLA | 서비스 계층에서 `owner_id`와 인증 주체 비교 | SEC-01 |
| T2 | 일반 사용자가 관리 API 호출 | API5:2023 BFLA | URL 역할 검사 + 작업별 권한 | SEC-02 |
| T3 | 비밀번호 대입, 크리덴셜 스터핑 | API2:2023 Broken Authentication | 로그인 실패 기록, 일시 제한 | SEC-03 |
| T4 | 과도한 요청으로 자원 고갈 | API4:2023 Unrestricted Resource Consumption | Redis 요청 제한, 429 + `Retry-After` | SEC-04 |
| T5 | 문서 속 지시문으로 모델 조종 | LLM01 Prompt Injection | 문서를 데이터로 구분, 실제 행위는 정책 게이트가 결정 | AI-01 |
| T6 | 모델이 권한 밖 도구를 실행하도록 유도 | LLM06 Excessive Agency | `ToolExecutionPolicyService`의 주체·권한 재검사 | AI-02 |
| T7 | AI 검색으로 타인 비공개 문서 유출 | LLM02 Sensitive Information Disclosure | 모델 입력 전 DB 단계에서 권한 필터링 | AI-03 |
| T8 | 모델이 만든 도구 이름·인자를 그대로 실행 | LLM05 Improper Output Handling | 허용 목록, 인자 스키마 검증, 감사 기록 | AI-04 |
| T9 | 세션 쿠키를 이용한 요청 위조 | — (CSRF) | CSRF 토큰, SameSite, HttpOnly | 인증 통합 테스트 |
| T10 | 에러 응답으로 내부 정보 노출 | API8:2023 Security Misconfiguration | 공통 에러 형식, 스택 트레이스 비공개 | 에러 응답 테스트 |
| T11 | 로그·이벤트에 민감 정보 저장 | — | 평문 비밀번호·토큰·LLM 원문 미저장, 보존 기간 | 로그 정책 점검 (7주) |
| T12 | 취약 실험 코드가 외부에 노출 | API9:2023 Improper Inventory Management | 별도 앱으로 격리, localhost 바인딩, `lab` 프로필 | 배포 설정 점검 |

## 5. 범위 밖

- 호스트 OS, Docker 엔진, 모델 가중치 자체의 취약점
- 모델 공급망(LLM03), 학습 데이터 오염(LLM04)
- 물리적 접근, 사회공학
- 분산 환경에서의 세션 공유, 고가용성
