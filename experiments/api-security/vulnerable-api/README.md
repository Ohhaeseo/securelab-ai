# ⚠️ vulnerable-api — 의도적으로 취약한 실험 대상

SEC-01(BOLA/IDOR)·SEC-02(역할 우회) 비교 실험의 **대조군**입니다. 보호 버전(`backend`)과 인증 방식·API 형식은 같고, **인가 검사만** 일부러 뺐습니다. ([ADR-0002](../../../docs/adr/0002-isolated-vulnerable-lab.md))

> **로컬 전용.** `server.address`가 루프백 주소(127.0.0.1, ::1)가 아니면 기동하지 않습니다. 배포하거나 외부에 공개하지 마세요.

## 보호 버전과의 차이

| 항목 | backend (보호) | vulnerable-api (취약) |
| --- | --- | --- |
| 세션 쿠키 인증, 로그인 시 세션 ID 교체 | ✅ | ✅ 같음 |
| CSRF (`X-XSRF-TOKEN` 헤더) | ✅ `XSRF-TOKEN` 쿠키 | ✅ 같음 (쿠키 이름만 `VULNLAB-XSRF-TOKEN`) |
| `GET/PATCH/DELETE /api/v1/documents/{id}` 소유권 검사 | ✅ | ❌ **SEC-01** |
| `GET /api/v1/documents` 목록 | 본인 + 공개 문서 | ❌ 남의 비공개 문서까지 |
| `/api/v1/security/**` 관리자 역할 | ✅ | ❌ **SEC-02** (응답에 다른 사용자 이메일·IP) |
| 저장소 | PostgreSQL | 메모리 (재시작하면 비워짐) |
| 비밀번호 해시 | Argon2id | bcrypt (로그인 지연은 비교 대상이 아님) |

세션 쿠키 이름도 `VULNLAB_SESSION`으로 다릅니다. 쿠키는 포트가 달라도 같은 호스트(127.0.0.1)에서 공유되므로, 한 브라우저로 두 앱을 같이 써도 섞이지 않게 했습니다.

## 실행

```powershell
cd experiments/api-security/vulnerable-api
.\gradlew.bat bootRun        # http://127.0.0.1:18080
```

## 테스트

```powershell
.\gradlew.bat test
```

- `VulnerabilityReproductionTests` — 결함이 **계속 재현되는지** 확인합니다. 이 테스트가 깨지면 비교 실험의 대조군이 사라진 것입니다. 인증·CSRF가 유지되는지도 확인합니다.
- `LoopbackOnlyGuardTests` — 루프백이 아닌 주소에서는 기동을 거부하는지 확인합니다.
