package com.securelab.auth.security;

import java.io.IOException;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.securelab.global.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.stereotype.Component;

/**
 * 권한 부족과 CSRF 토큰 오류를 구분해 403 JSON을 돌려준다.
 */
@Component
public class JsonAccessDeniedHandler implements AccessDeniedHandler {

	private static final Logger log = LoggerFactory.getLogger(JsonAccessDeniedHandler.class);

	private final SecurityErrorWriter errorWriter;

	public JsonAccessDeniedHandler(SecurityErrorWriter errorWriter) {
		this.errorWriter = errorWriter;
	}

	@Override
	public void handle(HttpServletRequest request, HttpServletResponse response,
			AccessDeniedException accessDeniedException) throws IOException {
		ErrorCode errorCode = (accessDeniedException instanceof CsrfException) ? ErrorCode.CSRF_INVALID
				: ErrorCode.ACCESS_DENIED;
		log.warn("security.denied code={} method={} path={}", errorCode, request.getMethod(),
				request.getRequestURI());
		this.errorWriter.write(response, errorCode);
	}

}
