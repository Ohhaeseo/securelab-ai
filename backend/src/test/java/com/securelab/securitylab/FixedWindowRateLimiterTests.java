package com.securelab.securitylab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;

import com.securelab.securitylab.ratelimit.FixedWindowRateLimiter;
import com.securelab.securitylab.ratelimit.RedisWindowCounter;
import org.junit.jupiter.api.Test;

import org.springframework.data.redis.RedisConnectionFailureException;

/**
 * Redis 장애 시 정책: API 요청 제한은 통과시킨다 (fail-open, ADR-0004).
 */
class FixedWindowRateLimiterTests {

	@Test
	void redisFailureFailsOpen() {
		RedisWindowCounter counter = mock(RedisWindowCounter.class);
		when(counter.increment(anyString(), any(Duration.class)))
			.thenThrow(new RedisConnectionFailureException("connection refused"));

		FixedWindowRateLimiter.Decision decision = new FixedWindowRateLimiter(counter).acquire("rl:api:user:1", 5,
				Duration.ofSeconds(60));

		assertThat(decision.allowed()).isTrue();
		assertThat(decision.enforced()).isFalse();
	}

	@Test
	void countOverLimitIsRejectedAndFirstRejectionIsFlagged() {
		RedisWindowCounter counter = mock(RedisWindowCounter.class);
		when(counter.increment(anyString(), any(Duration.class))).thenReturn(new RedisWindowCounter.Count(6, 2500));

		FixedWindowRateLimiter.Decision decision = new FixedWindowRateLimiter(counter).acquire("k", 5,
				Duration.ofSeconds(60));

		assertThat(decision.allowed()).isFalse();
		assertThat(decision.firstRejection()).isTrue();
		assertThat(decision.retryAfterSeconds()).isEqualTo(3);
	}

}
