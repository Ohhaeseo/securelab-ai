package com.securelab.auth.service;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import com.securelab.auth.dto.LoginRequest;
import com.securelab.auth.security.AuthenticatedUser;
import com.securelab.user.service.EmailNormalizer;

import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import com.securelab.securitylab.detection.LoginProtectionService;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.context.SecurityContextHolderStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.stereotype.Service;

/**
 * JSON 로그인. 컨트롤러에서 직접 인증하므로 Spring Security 로그인 필터가 해 주던 일을 명시적으로 한다.
 * <ol>
 * <li>자격 증명 검증 ({@link AuthenticationManager})</li>
 * <li>세션 고정 방지(세션 ID 교체)와 CSRF 토큰 재발급 ({@link SessionAuthenticationStrategy})</li>
 * <li>SecurityContext를 세션에 저장 ({@link SecurityContextRepository})</li>
 * </ol>
 */
@Service
public class LoginService {

	private final SecurityContextHolderStrategy securityContextHolderStrategy = SecurityContextHolder
		.getContextHolderStrategy();

	private final AuthenticationManager authenticationManager;

	private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

	private final SecurityContextRepository securityContextRepository;

	private final LoginProtectionService loginProtection;

	public LoginService(AuthenticationManager authenticationManager,
			SessionAuthenticationStrategy sessionAuthenticationStrategy,
			SecurityContextRepository securityContextRepository, LoginProtectionService loginProtection) {
		this.authenticationManager = authenticationManager;
		this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
		this.securityContextRepository = securityContextRepository;
		this.loginProtection = loginProtection;
	}

	public AuthenticatedUser login(LoginRequest request, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		String email = EmailNormalizer.normalize(request.email());
		String ip = httpRequest.getRemoteAddr();

		// SEC-03: 차단 중이면 비밀번호를 검증하지 않고 429
		this.loginProtection.checkAllowed(email, ip);

		Authentication authentication;
		try {
			authentication = this.authenticationManager
				.authenticate(UsernamePasswordAuthenticationToken.unauthenticated(email, request.password()));
		}
		catch (AuthenticationException ex) {
			this.loginProtection.onFailure(email, ip);
			throw ex;
		}
		AuthenticatedUser user = (AuthenticatedUser) authentication.getPrincipal();
		this.loginProtection.onSuccess(user.id(), email, ip);

		this.sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);

		SecurityContext context = this.securityContextHolderStrategy.createEmptyContext();
		context.setAuthentication(authentication);
		this.securityContextHolderStrategy.setContext(context);
		this.securityContextRepository.saveContext(context, httpRequest, httpResponse);

		return user;
	}

}
