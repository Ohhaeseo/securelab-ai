# 의사결정 기록 (ADR)

중요한 설계 결정과 그 이유를 남긴다. 완료 기준상 최소 2개가 필요하다.

| 번호 | 제목 | 상태 |
| --- | --- | --- |
| [0001](0001-session-cookie-auth.md) | 세션 쿠키 기반 인증 | 채택 |
| [0002](0002-isolated-vulnerable-lab.md) | 취약 실험 대상을 별도 앱으로 격리 | 채택 |
| [0003](0003-password-encoder.md) | 비밀번호 해시: Argon2id + DelegatingPasswordEncoder | 채택 |
| [0004](0004-rate-limit-and-login-protection.md) | 요청 제한·로그인 보호와 Redis 장애 정책 | 채택 |

**작성 후보:** Mock LLM 우선 개발, AI 도구 실행 정책

## 템플릿

```markdown
# NNNN. 제목

- 상태: 제안 | 채택 | 폐기 | 대체됨(→ NNNN)
- 날짜: YYYY-MM-DD

## 배경
무엇을 정해야 하고, 어떤 제약이 있는가.

## 결정
무엇을 하기로 했는가.

## 검토한 대안
다른 선택지와 고르지 않은 이유.

## 결과
이 결정으로 생기는 장점, 비용, 후속 작업.
```
