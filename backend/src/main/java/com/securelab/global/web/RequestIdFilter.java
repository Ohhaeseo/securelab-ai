package com.securelab.global.web;

import java.io.IOException;
import java.util.UUID;
import java.util.regex.Pattern;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * 요청마다 requestId를 정해 MDC와 응답 헤더에 넣는다. Spring Security보다 먼저 실행되어 401/403 응답에도 같은 값이 남는다.
 *
 * <p>
 * 클라이언트가 보낸 {@code X-Request-Id}는 안전한 문자만 있을 때 그대로 쓰고, 아니면 새로 만든다(로그 인젝션 방지).
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

	public static final String HEADER = "X-Request-Id";

	public static final String MDC_KEY = "requestId";

	private static final Pattern SAFE_REQUEST_ID = Pattern.compile("[A-Za-z0-9._-]{1,64}");

	@Override
	protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
			throws ServletException, IOException {
		String requestId = resolveRequestId(request.getHeader(HEADER));
		MDC.put(MDC_KEY, requestId);
		response.setHeader(HEADER, requestId);
		try {
			filterChain.doFilter(request, response);
		}
		finally {
			MDC.remove(MDC_KEY);
		}
	}

	static String resolveRequestId(String incoming) {
		if (incoming != null && SAFE_REQUEST_ID.matcher(incoming).matches()) {
			return incoming;
		}
		return "req-" + UUID.randomUUID();
	}

}
