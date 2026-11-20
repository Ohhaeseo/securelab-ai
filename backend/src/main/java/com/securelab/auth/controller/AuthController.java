package com.securelab.auth.controller;

import java.net.URI;

import jakarta.validation.Valid;

import com.securelab.auth.dto.SignupRequest;
import com.securelab.auth.service.AuthService;
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

	public AuthController(AuthService authService) {
		this.authService = authService;
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

}
