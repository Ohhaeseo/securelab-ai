package com.securelab.securitylab;

import static org.assertj.core.api.Assertions.assertThat;

import com.securelab.support.ApiClient;
import com.securelab.support.ApiIntegrationTest;
import com.securelab.support.ApiResponse;
import org.junit.jupiter.api.Test;

import org.springframework.test.context.TestPropertySource;

/**
 * 익명 요청은 IP별로 센다 (가입·로그인 남용 방지). 이 컨텍스트의 모든 요청은 127.0.0.1에서 오므로 테스트를 하나만 둔다.
 */
@TestPropertySource(properties = { "securelab.rate-limit.anonymous-limit=3", "securelab.rate-limit.window=30s" })
class AnonymousRateLimitTests extends ApiIntegrationTest {

	@Test
	void anonymousRequestsAreLimitedPerIpButHealthIsNot() {
		ApiClient anonymous = newClient();
		for (int i = 0; i < 3; i++) {
			assertThat(anonymous.get("/api/v1/auth/csrf").status()).isEqualTo(204);
		}

		// 이미 CSRF 쿠키가 있는 같은 클라이언트로 네 번째 요청을 보낸다.
		ApiResponse limited = anonymous.post("/api/v1/auth/signup", credentials(uniqueEmail("x"), PASSWORD));

		assertThat(limited.status()).isEqualTo(429);
		assertThat(limited.<String>json("$.code")).isEqualTo("RATE_LIMITED");
		assertThat(newClient().get("/actuator/health").status()).isEqualTo(200);
	}

}
