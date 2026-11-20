package com.securelab.securitylab.detection;

import com.securelab.securitylab.audit.SecurityEventRecorder;
import com.securelab.securitylab.audit.SecurityEventType;
import com.securelab.securitylab.audit.Severity;
import com.securelab.user.entity.User;
import com.securelab.user.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;

/**
 * 로그인 반복 실패 탐지·차단 (SEC-03). 로그인 처리 전후에 호출한다.
 * <ol>
 * <li>{@link #checkAllowed} — 차단 중이면 비밀번호를 검증하지 않고 거부 (Argon2 비용 절약)</li>
 * <li>{@link #onFailure} / {@link #onSuccess} — 시도를 기록하고 카운터를 갱신</li>
 * </ol>
 * 이메일은 HMAC 다이제스트로만 다룬다.
 */
@Service
public class LoginProtectionService {

	private static final Logger log = LoggerFactory.getLogger(LoginProtectionService.class);

	private final EmailDigester digester;

	private final LoginAttemptGuard guard;

	private final LoginAttemptRepository attempts;

	private final UserRepository users;

	private final SecurityEventRecorder securityEvents;

	public LoginProtectionService(EmailDigester digester, LoginAttemptGuard guard, LoginAttemptRepository attempts,
			UserRepository users, SecurityEventRecorder securityEvents) {
		this.digester = digester;
		this.guard = guard;
		this.attempts = attempts;
		this.users = users;
		this.securityEvents = securityEvents;
	}

	public void checkAllowed(String email, String ip) {
		String digest = this.digester.digest(email);
		LoginAttemptGuard.Decision decision = this.guard.check(digest, ip);
		if (decision.blocked()) {
			this.attempts.save(LoginAttempt.blocked(null, digest, ip));
			throw new LoginBlockedException(decision.retryAfterSeconds());
		}
	}

	public void onFailure(String email, String ip) {
		String digest = this.digester.digest(email);
		Long userId = this.users.findByEmail(email).map(User::getId).orElse(null);
		this.attempts.save(LoginAttempt.failure(userId, digest, ip));
		this.guard.recordFailure(digest, ip).ifPresent((reason) -> {
			String subject = LoginAttemptGuard.REASON_IP.equals(reason) ? "ip:" + ip
					: "account:" + digest.substring(0, 16) + "@" + ip;
			log.warn("security.login-blocked reason={} subject={}", reason, subject);
			this.securityEvents.blocked(SecurityEventType.LOGIN_BLOCKED, Severity.HIGH, userId, reason, "LOGIN",
					subject);
		});
	}

	public void onSuccess(Long userId, String email, String ip) {
		String digest = this.digester.digest(email);
		this.attempts.save(LoginAttempt.success(userId, digest, ip));
		this.guard.recordSuccess(digest, ip);
	}

}
