package com.securelab.securitylab.ratelimit;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

/**
 * API 요청 제한 설정 (SEC-04). 고정 윈도우 방식 — docs/adr/0004-rate-limit-and-login-protection.md
 *
 * @param authenticatedLimit 로그인 사용자 1명이 윈도우마다 보낼 수 있는 요청 수
 * @param anonymousLimit 익명 요청의 IP별 한도 (가입·로그인·CSRF 토큰 등)
 * @param window 윈도우 길이
 * @param enabled 요청 제한 사용 여부. {@code false}는 <b>lab 프로필의 성능 측정(정책 적용 전후 비교)에서만</b> 허용한다 — 다른 프로필에서
 * 끄면 기동이 실패한다 (방어를 끄는 운영 스위치를 두지 않는다, ADR-0002).
 */
@ConfigurationProperties("securelab.rate-limit")
public record RateLimitProperties(@DefaultValue("120") int authenticatedLimit, @DefaultValue("60") int anonymousLimit,
		@DefaultValue("60s") Duration window, @DefaultValue("true") boolean enabled) {

	public void requireAllowed(Environment environment) {
		if (!this.enabled && !environment.acceptsProfiles(Profiles.of("lab"))) {
			throw new IllegalStateException(
					"securelab.rate-limit.enabled=false 는 lab 프로필의 성능 측정에서만 허용합니다. 운영 경로에서는 요청 제한을 끌 수 없습니다.");
		}
	}

}
