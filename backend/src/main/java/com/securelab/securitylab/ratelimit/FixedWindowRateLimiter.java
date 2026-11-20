package com.securelab.securitylab.ratelimit;

import java.time.Duration;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Redis 고정 윈도우 카운터. INCR과 만료 설정을 Lua 스크립트 한 번으로 처리해, 중간에 실패해도 만료 없는 키가 남지 않는다.
 *
 * <p>
 * Redis를 쓸 수 없으면 <b>요청을 통과시킨다(fail-open)</b>. 일반 API의 가용성을 우선하고, 장애는 로그로 남긴다. 로그인 보호는 다른 정책(DB
 * 대체 집계)을 쓴다 — ADR-0004.
 */
@Component
public class FixedWindowRateLimiter {

	private static final Logger log = LoggerFactory.getLogger(FixedWindowRateLimiter.class);

	/** 반환: {현재 카운트, 남은 TTL(ms)} */
	private static final RedisScript<List> INCREMENT_SCRIPT = RedisScript.of("""
			local count = redis.call('INCR', KEYS[1])
			if count == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
			local ttl = redis.call('PTTL', KEYS[1])
			if ttl < 0 then redis.call('PEXPIRE', KEYS[1], ARGV[1]); ttl = tonumber(ARGV[1]) end
			return {count, ttl}
			""", List.class);

	private final StringRedisTemplate redis;

	public FixedWindowRateLimiter(StringRedisTemplate redis) {
		this.redis = redis;
	}

	/**
	 * 요청 1건을 센다.
	 */
	public Decision acquire(String key, int limit, Duration window) {
		try {
			List<?> result = this.redis.execute(INCREMENT_SCRIPT, List.of(key), String.valueOf(window.toMillis()));
			long count = ((Number) result.get(0)).longValue();
			long ttlMillis = ((Number) result.get(1)).longValue();
			return new Decision(count <= limit, count, limit, Math.max(1, (ttlMillis + 999) / 1000), true);
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
