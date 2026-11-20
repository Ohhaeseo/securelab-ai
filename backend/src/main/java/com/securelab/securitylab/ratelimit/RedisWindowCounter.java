package com.securelab.securitylab.ratelimit;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

/**
 * Redis 고정 윈도우 카운터. 첫 증가 때 만료가 걸리고, 만료되면 0부터 다시 센다. Redis 오류는 그대로 던진다 — 장애 정책은 호출하는 쪽이
 * 정한다(ADR-0004).
 */
@Component
public class RedisWindowCounter {

	/** INCR과 만료 설정을 원자적으로 처리. 반환: {카운트, 남은 TTL(ms)} */
	@SuppressWarnings("rawtypes")
	private static final RedisScript<List> INCREMENT = RedisScript.of("""
			local count = redis.call('INCR', KEYS[1])
			if count == 1 then redis.call('PEXPIRE', KEYS[1], ARGV[1]) end
			local ttl = redis.call('PTTL', KEYS[1])
			if ttl < 0 then redis.call('PEXPIRE', KEYS[1], ARGV[1]); ttl = tonumber(ARGV[1]) end
			return {count, ttl}
			""", List.class);

	@SuppressWarnings("rawtypes")
	private static final RedisScript<List> PEEK = RedisScript.of("""
			local count = tonumber(redis.call('GET', KEYS[1]) or '0')
			return {count, redis.call('PTTL', KEYS[1])}
			""", List.class);

	private final StringRedisTemplate redis;

	public RedisWindowCounter(StringRedisTemplate redis) {
		this.redis = redis;
	}

	public Count increment(String key, Duration window) {
		return toCount(this.redis.execute(INCREMENT, List.of(key), String.valueOf(window.toMillis())));
	}

	public Count peek(String key) {
		return toCount(this.redis.execute(PEEK, List.of(key)));
	}

	public void reset(String key) {
		this.redis.delete(key);
	}

	private static Count toCount(List<?> result) {
		if (result == null || result.size() < 2) {
			throw new IllegalStateException("Redis 스크립트 응답이 올바르지 않습니다: " + result);
		}
		return new Count(((Number) result.get(0)).longValue(), ((Number) result.get(1)).longValue());
	}

	/**
	 * @param ttlMillis 윈도우 남은 시간(ms). 키가 없으면 음수.
	 */
	public record Count(long value, long ttlMillis) {

		public long retryAfterSeconds() {
			return Math.max(1, (this.ttlMillis + 999) / 1000);
		}

	}

}
