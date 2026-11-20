package com.securelab.securitylab.ratelimit;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * API 요청 제한 설정 (SEC-04). 고정 윈도우 방식 — docs/adr/0004-rate-limit-and-login-protection.md
 *
 * @param authenticatedLimit 로그인 사용자 1명이 윈도우마다 보낼 수 있는 요청 수
 * @param anonymousLimit 익명 요청의 IP별 한도 (가입·로그인·CSRF 토큰 등)
 * @param window 윈도우 길이
 */
@ConfigurationProperties("securelab.rate-limit")
public record RateLimitProperties(@DefaultValue("120") int authenticatedLimit, @DefaultValue("60") int anonymousLimit,
		@DefaultValue("60s") Duration window) {
}
