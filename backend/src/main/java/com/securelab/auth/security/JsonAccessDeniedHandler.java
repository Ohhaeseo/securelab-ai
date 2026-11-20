package com.securelab.auth.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.securelab.global.exception.ErrorCode;
import com.securelab.securitylab.audit.SecurityEventRecorder;
import com.securelab.securitylab.audit.SecurityEventType;
import com.securelab.securitylab.audit.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

/**
 * 권한 부족(역할 규칙)과 CSRF 토큰 오류를 구분해 403 JSON을 돌려주고 보안 이벤트로 남긴다.
 */
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

	private static final Logger log = LoggerFactory.getLogger(JsonAccessDeniedHandler.class);

	private final SecurityErrorWriter errorWriter;

	private final SecurityEventRecorder securityEvents;

	public JsonAccessDeniedHandler(SecurityErrorWriter errorWriter, SecurityEventRecorder securityEvents) {
		this.errorWriter = errorWriter;
		this.securityEvents = securityEvents;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		boolean csrf = accessDeniedException instanceof CsrfException;
		ErrorCode errorCode = csrf ? ErrorCode.CSRF_INVALID : ErrorCode.ACCESS_DENIED;
		String endpoint = request.getMethod() + " " + request.getRequestURI();
		log.warn("security.denied code={} endpoint={}", errorCode, endpoint);

		this.securityEvents.blocked(csrf ? SecurityEventType.CSRF_REJECTED : SecurityEventType.ACCESS_DENIED,
				csrf ? Severity.MEDIUM : Severity.HIGH, currentUserId(), csrf ? "CSRF_TOKEN" : "ROLE_REQUIRED",
				"ENDPOINT", endpoint);
		this.errorWriter.write(response, errorCode);
	}

	private static Long currentUserId() {
		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
		return (authentication != null && authentication.getPrincipal() instanceof AuthenticatedUser user)
				? user.id() : null;
	}

}
