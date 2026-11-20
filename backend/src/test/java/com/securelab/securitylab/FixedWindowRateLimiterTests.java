package com.securelab.securitylab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Duration;

import com.securelab.securitylab.ratelimit.FixedWindowRateLimiter;
import org.junit.jupiter.api.Test;

import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

/**
 * Redis 장애 시 정책: API 요청 제한은 통과시킨다 (fail-open, ADR-0004).
 */
class FixedWindowRateLimiterTests {

	@Test
	void redisFailureFailsOpen() {
		StringRedisTemplate redis = mock(StringRedisTemplate.class);
		when(redis.execute(any(RedisScript.class), anyList(), any())).thenThrow(
				new RedisConnectionFailureException("connection refused"));

		FixedWindowRateLimiter.Decision decision = new FixedWindowRateLimiter(redis).acquire("rl:api:user:1", 5,
				Duration.ofSeconds(60));

		assertThat(decision.allowed()).isTrue();
		assertThat(decision.enforced()).isFalse();
	}

}
