package com.securelab.auth.controller;

import java.net.URI;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;

import com.securelab.auth.dto.LoginRequest;
import com.securelab.auth.dto.LoginResponse;
import com.securelab.auth.dto.SignupRequest;
import com.securelab.auth.security.AuthenticatedUser;
import com.securelab.auth.service.AuthService;
import com.securelab.auth.service.LoginService;
import com.securelab.user.dto.UserResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

	private final AuthService authService;

	private final LoginService loginService;

	public AuthController(AuthService authService, LoginService loginService) {
		this.authService = authService;
		this.loginService = loginService;
	}

	/**
	 * CSRF 토큰을 {@code XSRF-TOKEN} 쿠키로 내려준다. 클라이언트는 그 값을 {@code X-XSRF-TOKEN} 헤더로 보낸다.
	 */
	@GetMapping("/csrf")
	public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
		// 지연 로딩되는 토큰을 꺼내 쿠키가 확실히 발급되도록 한다.
		csrfToken.getToken();
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/signup")
	public ResponseEntity<UserResponse> signup(@Valid @RequestBody SignupRequest request) {
		UserResponse user = this.authService.signup(request);
		return ResponseEntity.created(URI.create("/api/v1/users/" + user.id())).body(user);
	}

	/**
	 * 로그인 성공 시 세션 ID가 바뀌고 새 CSRF 토큰 쿠키가 내려간다. 이후 요청은 새 {@code XSRF-TOKEN} 값을 써야 한다.
	 * 로그아웃은 Spring Security의 LogoutFilter가 {@code POST /api/v1/auth/logout}에서 처리한다 (204).
	 */
	@PostMapping("/login")
	public LoginResponse login(@Valid @RequestBody LoginRequest request, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		AuthenticatedUser user = this.loginService.login(request, httpRequest, httpResponse);
		return new LoginResponse(user.id(), user.email(), user.role());
	}

}
