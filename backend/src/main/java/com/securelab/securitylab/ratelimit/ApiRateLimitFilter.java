package com.securelab.securitylab.ratelimit;

import java.io.IOException;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.securelab.auth.security.AuthenticatedUser;
import com.securelab.auth.security.SecurityErrorWriter;
import com.securelab.global.exception.ErrorCode;
import com.securelab.securitylab.audit.SecurityEventRecorder;
import com.securelab.securitylab.audit.SecurityEventType;
import com.securelab.securitylab.audit.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * {@code /api/**} 요청 제한 (SEC-04). 로그인 사용자는 사용자 ID별, 익명 요청은 IP별로 센다.
 *
 * <p>
 * Spring Security 필터 체인 안(세션 인증 로드 직후, CSRF·인가 검사 전)에 들어가므로 CSRF 오류나 인가 실패 요청도 한도에 포함된다. 서블릿
 * 필터로 자동 등록되지 않도록 빈으로 만들지 않고 SecurityConfig에서 직접 생성한다.
 */
public class ApiRateLimitFilter extends OncePerRequestFilter {

	private static final Logger log = LoggerFactory.getLogger(ApiRateLimitFilter.class);

	private final FixedWindowRateLimiter limiter;

	private final RateLimitProperties properties;

	private final SecurityEventRecorder securityEvents;

	private final SecurityErrorWriter errorWriter;

	public ApiRateLimitFilter(FixedWindowRateLimiter limiter, RateLimitProperties properties,
			SecurityEventRecorder securityEvents, SecurityErrorWriter errorWriter) {
		this.limiter = limiter;
		this.properties = properties;
		this.securityEvents = securityEvents;
		this.errorWriter = errorWriter;
	}

	@Override
	protected boolean shouldNotFilter(HttpServletRequest request) {
		return !request.getRequestURI().startsWith("/api/");
	}

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
			throws ServletException, IOException {
		Long userId = currentUserId();
		String subject = (userId != null) ? "user:" + userId : "ip:" + request.getRemoteAddr();
		int limit = (userId != null) ? this.properties.authenticatedLimit() : this.properties.anonymousLimit();

		FixedWindowRateLimiter.Decision decision = this.limiter.acquire("rl:api:" + subject, limit,
				this.properties.window());
		if (decision.enforced()) {
			response.setHeader("X-RateLimit-Limit", String.valueOf(limit));
			response.setHeader("X-RateLimit-Remaining", String.valueOf(decision.remaining()));
		}
		if (decision.allowed()) {
			chain.doFilter(request, response);
			return;
		}
		if (decision.firstRejection()) {
			log.warn("security.rate-limited subject={} limit={} window={}", subject, limit, this.properties.window());
			this.securityEvents.blocked(SecurityEventType.RATE_LIMITED, Severity.MEDIUM, userId, "API_RATE_LIMIT",
					"SUBJECT", subject);
		}
		response.setHeader("Retry-After", String.valueOf(decision.retryAfterSeconds()));
		this.errorWriter.write(response, ErrorCode.RATE_LIMITED);
	}

	private static Long currentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user)
				? user.id() : null;
	}

}
