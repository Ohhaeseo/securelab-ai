package com.securelab.securitylab.detection;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.time.Instant;

import com.securelab.securitylab.ratelimit.RedisWindowCounter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.data.redis.RedisConnectionFailureException;

/**
 * Redis 장애 시 로그인 보호는 열리지 않고 login_attempts로 같은 기준을 대신 판정한다 (ADR-0004).
 */
class LoginAttemptGuardTests {

	private final RedisWindowCounter counter = mock(RedisWindowCounter.class);

	private final LoginAttemptRepository attempts = mock(LoginAttemptRepository.class);

	private final LoginAttemptGuard guard = new LoginAttemptGuard(this.counter, this.attempts,
			new LoginProtectionProperties(5, 20, Duration.ofMinutes(15), null));

	@BeforeEach
	void redisIsDown() {
		RedisConnectionFailureException down = new RedisConnectionFailureException("connection refused");
		when(this.counter.peek(anyString())).thenThrow(down);
		when(this.counter.increment(anyString(), any(Duration.class))).thenThrow(down);
	}

	@Test
	void accountIpFailuresFromDatabaseBlock() {
		when(this.attempts.countFailures(eq("d"), eq("1.2.3.4"), any(Instant.class))).thenReturn(5L);

		LoginAttemptGuard.Decision decision = this.guard.check("d", "1.2.3.4");

		assertThat(decision.blocked()).isTrue();
		assertThat(decision.reason()).isEqualTo("ACCOUNT_IP");
		assertThat(decision.retryAfterSeconds()).isEqualTo(900);
	}

	@Test
	void ipFailuresFromDatabaseBlock() {
		when(this.attempts.countFailures(anyString(), anyString(), any(Instant.class))).thenReturn(1L);
		when(this.attempts.countFailuresByIp(eq("1.2.3.4"), any(Instant.class))).thenReturn(20L);

		assertThat(this.guard.check("d", "1.2.3.4").reason()).isEqualTo("IP");
	}

	@Test
	void belowThresholdsIsAllowed() {
		when(this.attempts.countFailures(anyString(), anyString(), any(Instant.class))).thenReturn(4L);
		when(this.attempts.countFailuresByIp(anyString(), any(Instant.class))).thenReturn(19L);

		assertThat(this.guard.check("d", "1.2.3.4").blocked()).isFalse();
	}

	@Test
	void accountCountStartsAfterLastSuccessLikeRedisReset() {
		Instant lastSuccess = Instant.now().minusSeconds(60);
		when(this.attempts.lastSuccessAt("d", "1.2.3.4")).thenReturn(lastSuccess);

		this.guard.check("d", "1.2.3.4");

		verify(this.attempts).countFailures("d", "1.2.3.4", lastSuccess);
	}

	@Test
	void recordFailureReportsBlockStartFromDatabaseWhenRedisIsDown() {
		when(this.attempts.countFailures(anyString(), anyString(), any(Instant.class))).thenReturn(5L);

		assertThat(this.guard.recordFailure("d", "1.2.3.4")).contains("ACCOUNT_IP");
	}

}
