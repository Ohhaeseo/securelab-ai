package com.securelab.securitylab.detection;

import java.time.Instant;
import java.util.Optional;

import com.securelab.securitylab.ratelimit.RedisWindowCounter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.dao.DataAccessException;
import org.springframework.stereotype.Component;

/**
 * 로그인 실패 카운터. (계정 다이제스트 + IP)와 IP 두 키로 센다. Redis를 쓸 수 없으면 {@code login_attempts}로 같은 기준을 대신
 * 집계한다 — 로그인 보호는 장애 때도 열리지 않는다 (ADR-0004).
 */
@Component
public class LoginAttemptGuard {

	private static final Logger log = LoggerFactory.getLogger(LoginAttemptGuard.class);

	static final String REASON_ACCOUNT_IP = "ACCOUNT_IP";

	static final String REASON_IP = "IP";

	private final RedisWindowCounter counter;

	private final LoginAttemptRepository attempts;

	private final LoginProtectionProperties properties;

	public LoginAttemptGuard(RedisWindowCounter counter, LoginAttemptRepository attempts,
			LoginProtectionProperties properties) {
		this.counter = counter;
		this.attempts = attempts;
		this.properties = properties;
	}

	public Decision check(String digest, String ip) {
		try {
			RedisWindowCounter.Count account = this.counter.peek(accountKey(digest, ip));
			if (account.value() >= this.properties.maxFailuresPerAccountIp()) {
				return Decision.blocked(account.retryAfterSeconds(), REASON_ACCOUNT_IP);
			}
			RedisWindowCounter.Count byIp = this.counter.peek(ipKey(ip));
			if (byIp.value() >= this.properties.maxFailuresPerIp()) {
				return Decision.blocked(byIp.retryAfterSeconds(), REASON_IP);
			}
			return Decision.ALLOWED;
		}
		catch (DataAccessException | IllegalStateException ex) {
			log.warn("login-protection.redis-unavailable — DB 기록으로 대신 판정합니다: {}", ex.getMessage());
			return checkFromDatabase(digest, ip);
		}
	}

	/**
	 * 실패 1건을 센다. 호출 전에 {@code login_attempts}에 이 실패가 저장돼 있어야 DB 대체 집계와 결과가 같다.
	 * @return 이번 실패로 차단이 시작됐으면 그 이유
	 */
	public Optional<String> recordFailure(String digest, String ip) {
		try {
			long account = this.counter.increment(accountKey(digest, ip), this.properties.window()).value();
			long byIp = this.counter.increment(ipKey(ip), this.properties.window()).value();
			if (account == this.properties.maxFailuresPerAccountIp()) {
				return Optional.of(REASON_ACCOUNT_IP);
			}
			return (byIp == this.properties.maxFailuresPerIp()) ? Optional.of(REASON_IP) : Optional.empty();
		}
		catch (DataAccessException | IllegalStateException ex) {
			log.warn("login-protection.redis-unavailable — 실패는 DB에만 기록합니다: {}", ex.getMessage());
			Decision decision = checkFromDatabase(digest, ip);
			return decision.blocked() ? Optional.of(decision.reason()) : Optional.empty();
		}
	}

	/** 성공하면 (계정 + IP) 카운터를 지운다. IP 카운터는 두어 여러 계정 대입을 계속 센다. */
	public void recordSuccess(String digest, String ip) {
		try {
			this.counter.reset(accountKey(digest, ip));
		}
		catch (DataAccessException ex) {
			log.warn("login-protection.redis-unavailable — 카운터 초기화 생략 (DB 집계는 마지막 성공 이후만 센다): {}",
					ex.getMessage());
		}
	}

	/**
	 * DB 대체 집계. 차단 해제 시각을 정확히 알 수 없어 Retry-After는 윈도우 전체 길이로 보수적으로 준다.
	 */
	Decision checkFromDatabase(String digest, String ip) {
		Instant windowStart = Instant.now().minus(this.properties.window());
		Instant lastSuccess = this.attempts.lastSuccessAt(digest, ip);
		Instant accountSince = (lastSuccess != null && lastSuccess.isAfter(windowStart)) ? lastSuccess : windowStart;
		long retryAfter = this.properties.window().toSeconds();
		if (this.attempts.countFailures(digest, ip, accountSince) >= this.properties.maxFailuresPerAccountIp()) {
			return Decision.blocked(retryAfter, REASON_ACCOUNT_IP);
		}
		if (this.attempts.countFailuresByIp(ip, windowStart) >= this.properties.maxFailuresPerIp()) {
			return Decision.blocked(retryAfter, REASON_IP);
		}
		return Decision.ALLOWED;
	}

	private static String accountKey(String digest, String ip) {
		return "login:fail:acct:" + digest + ":" + ip;
	}

	private static String ipKey(String ip) {
		return "login:fail:ip:" + ip;
	}

	public record Decision(boolean blocked, long retryAfterSeconds, String reason) {

		static final Decision ALLOWED = new Decision(false, 0, null);

		static Decision blocked(long retryAfterSeconds, String reason) {
			return new Decision(true, retryAfterSeconds, reason);
		}

	}

}
