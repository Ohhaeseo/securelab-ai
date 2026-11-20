package com.securelab.securitylab.ratelimit;

import java.time.Duration;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * API 요청 제한 판정. Redis를 쓸 수 없으면 <b>요청을 통과시킨다(fail-open)</b> — 일반 API의 가용성을 우선하고, 장애는 로그로 남긴다.
 * 로그인 보호는 다른 정책(DB 대체 집계)을 쓴다 (ADR-0004).
 */
@Component
public class FixedWindowRateLimiter {

	private static final Logger log = LoggerFactory.getLogger(FixedWindowRateLimiter.class);

	private final RedisWindowCounter counter;

	public FixedWindowRateLimiter(RedisWindowCounter counter) {
		this.counter = counter;
	}

	/**
	 * 요청 1건을 센다.
	 */
	public Decision acquire(String key, int limit, Duration window) {
		try {
			RedisWindowCounter.Count count = this.counter.increment(key, window);
			return new Decision(count.value() <= limit, count.value(), limit, count.retryAfterSeconds(), true);
		}
		catch (DataAccessException | IllegalStateException ex) {
			log.warn("rate-limit.unavailable key={} — Redis를 쓸 수 없어 요청을 통과시킵니다 (fail-open): {}", key,
					ex.getMessage());
			return new Decision(true, 0, limit, 0, false);
		}
	}

	/**
	 * @param allowed 허용 여부
	 * @param count 이번 윈도우에서 센 요청 수 (Redis 장애 시 0)
	 * @param retryAfterSeconds 윈도우가 끝날 때까지 남은 초
	 * @param enforced Redis로 실제 판정했는지 (false = fail-open)
	 */
	public record Decision(boolean allowed, long count, int limit, long retryAfterSeconds, boolean enforced) {

		public long remaining() {
			return Math.max(0, this.limit - this.count);
		}

		/** 이번 요청이 윈도우에서 처음으로 한도를 넘은 요청인지 (이벤트를 한 번만 남기기 위해). */
		public boolean firstRejection() {
			return this.count == this.limit + 1L;
		}

	}

}
