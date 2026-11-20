package com.securelab.securitylab;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import com.securelab.securitylab.ratelimit.RateLimitProperties;
import org.junit.jupiter.api.Test;

import org.springframework.mock.env.MockEnvironment;

/**
 * 요청 제한을 끄는 측정 스위치는 lab 프로필에서만 허용한다. 운영 경로에서 방어를 끌 수 없다 (ADR-0002).
 */
class RateLimitSwitchTests {

	@Test
	void disablingOutsideLabProfileFailsStartup() {
		MockEnvironment local = new MockEnvironment();
		local.setActiveProfiles("local");

		assertThatThrownBy(() -> properties(false).requireAllowed(local)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("lab");
		assertThatThrownBy(() -> properties(false).requireAllowed(new MockEnvironment()))
			.isInstanceOf(IllegalStateException.class);
	}

	@Test
	void disablingInLabProfileAndEnabledAnywhereAreAllowed() {
		MockEnvironment lab = new MockEnvironment();
		lab.setActiveProfiles("local", "lab");

		assertThatCode(() -> properties(false).requireAllowed(lab)).doesNotThrowAnyException();
		assertThatCode(() -> properties(true).requireAllowed(new MockEnvironment())).doesNotThrowAnyException();
	}

	private static RateLimitProperties properties(boolean enabled) {
		return new RateLimitProperties(120, 60, Duration.ofSeconds(60), enabled);
	}

}
