# 부하 테스트 (k6)

요청 제한(SEC-04)의 차단 효과, 오탐, 지연 증가를 **정상·공격 혼합 트래픽**으로 잰다 (I-8).

## 실행

```powershell
powershell -ExecutionPolicy Bypass -File experiments/load-tests/run-k6.ps1 -Duration 90s
```

- backend를 `local,lab` 프로필·전용 포트 18091로 두 번 띄운다: 요청 제한 OFF(`securelab.rate-limit.enabled=false`, lab 프로필에서만 허용) → ON(기본값 120회/60초).
- 응답한 서버가 SecureLab인지(`X-Request-Id`) 확인한 뒤에만 k6를 돌린다.
- 측정 전후로 Redis 실험 카운터를 지운다.

## 시나리오 (`scripts/mixed-traffic.js`)

| 트래픽 | 사용자 | 속도 | 요청 |
| --- | --- | --- | --- |
| 정상 | 10명 | 합계 초당 5건 (1인당 분당 약 30건) | 자기 문서 조회 / 목록 번갈아 |
| 공격 | 2명 | 합계 초당 10건 (1인당 분당 약 300건) | `GET /api/v1/users/me` 반복 |

- 사용자마다 가입·로그인 후 세션을 유지한다 (`noCookiesReset: true` — k6 기본값은 반복마다 쿠키를 비운다).
- 지표: `normal_latency`, `normal_blocked`(오탐), `normal_errors`, `attack_blocked`(차단률), `attack_latency`.

## 결과

- [k6-rate-limit-20261121-004807.md](results/k6-rate-limit-20261121-004807.md): 정상 오탐 0%, 공격 차단 46.2%(고정 윈도우 이론값과 일치), 정상 p50 5.8 → 8.3ms
- 해석과 한계: [docs/experiment-results.md](../../docs/experiment-results.md)
