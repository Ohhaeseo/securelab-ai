# 0003. 비밀번호 해시: Argon2id + DelegatingPasswordEncoder

- 상태: 채택
- 날짜: 2026-11-20

## 배경

기획서는 "Argon2id 또는 검증된 Spring Security PasswordEncoder"로 단방향 해시를 저장하라고 한다. Spring Security의 `Argon2PasswordEncoder`는 BouncyCastle 구현이 필요하다 (I-7).

## 결정

- `DelegatingPasswordEncoder`를 쓰고 기본 알고리즘을 `argon2`로 둔다. 인코더는 `Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8()`(Argon2id, 메모리 16MiB, 반복 2, 병렬 1)이다.
- `org.bouncycastle:bcprov-jdk18on:1.87`을 `runtimeOnly`로 추가하고 lockfile로 고정한다.
- 저장 값은 `{argon2}$argon2id$...` 형식이다. 앞의 `{id}`로 알고리즘을 구분하므로 `bcrypt`도 검증할 수 있게 함께 등록한다.

## 검토한 대안

- **bcrypt 단독:** 추가 의존성은 없지만 메모리 하드 함수가 아니고, 입력이 72바이트에서 잘린다.
- **Argon2 단독(위임 없이):** 나중에 파라미터나 알고리즘을 바꿀 때 기존 해시와 구분할 방법이 없다.

## 결과

- 로그인 1회마다 Argon2 해시 계산(수십 ms, 16MiB)이 든다. 로그인 반복 시도 제한(4주차)이 자원 고갈 방어도 겸한다.
- 비밀번호 길이는 8~128자로 제한해 과도하게 긴 입력으로 해시 비용을 키우는 것을 막는다.
- `DaoAuthenticationProvider`는 없는 계정에도 더미 해시를 계산해 응답 시간으로 계정 존재 여부가 드러나지 않게 한다.
