package com.securelab.securitylab;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.securelab.support.ApiClient;
import com.securelab.support.ApiIntegrationTest;
import com.securelab.support.ApiResponse;
import org.junit.jupiter.api.Test;

import org.springframework.test.context.TestPropertySource;

/**
 * SEC-04 과도한 호출: 로그인 사용자별 한도를 넘으면 429 + Retry-After.
 */
@TestPropertySource(properties = { "securelab.rate-limit.authenticated-limit=5", "securelab.rate-limit.window=30s" })
class RateLimitApiTests extends ApiIntegrationTest {

	@Test
	void requestsOverLimitGet429WithRetryAfter() {
		ApiClient user = loggedInClient("flood");

		for (int i = 1; i <= 5; i++) {
			ApiResponse ok = user.get("/api/v1/users/me");
			assertThat(ok.status()).as("요청 %d", i).isEqualTo(200);
			assertThat(ok.header("X-RateLimit-Remaining")).isEqualTo(String.valueOf(5 - i));
		}
		ApiResponse limited = user.get("/api/v1/users/me");

		assertThat(limited.status()).isEqualTo(429);
		assertThat(limited.<String>json("$.code")).isEqualTo("RATE_LIMITED");
		assertThat(Integer.parseInt(limited.header("Retry-After"))).isBetween(1, 30);
		assertThat(limited.header("X-RateLimit-Remaining")).isEqualTo("0");
	}

	@Test
	void limitIsPerUser() {
		ApiClient noisy = loggedInClient("noisy");
		ApiClient quiet = loggedInClient("quiet");
		for (int i = 0; i < 6; i++) {
			noisy.get("/api/v1/users/me");
		}

		assertThat(noisy.get("/api/v1/users/me").status()).isEqualTo(429);
		assertThat(quiet.get("/api/v1/users/me").status()).isEqualTo(200);
	}

	@Test
	void limitAlsoCountsRejectedRequestsSoAttacksCannotProbeForever() {
		ApiClient attacker = loggedInClient("prober");
		for (int i = 0; i < 5; i++) {
			assertThat(attacker.get("/api/v1/documents/999999999").status()).isEqualTo(404);
		}

		assertThat(attacker.get("/api/v1/documents/999999998").status()).isEqualTo(429);
	}

	@Test
	void rateLimitEventIsRecordedOncePerWindow() {
		ApiClient user = loggedInClient("event");
		Integer userId = user.get("/api/v1/users/me").json("$.id");
		for (int i = 0; i < 10; i++) {
			user.get("/api/v1/users/me");
		}

		ApiResponse events = loggedInAdmin().get("/api/v1/security/events?type=RATE_LIMITED&size=200");
		List<Object> mine = events.json("$.content[?(@.resourceId == 'user:" + userId + "')]");

		assertThat(mine).hasSize(1);
	}

}
