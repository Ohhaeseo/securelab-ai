package com.securelab.vulnlab;

import java.net.URI;
import java.util.Map;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 가입·로그인. 보호 버전과 같은 경로·형식·세션 처리.
 */
@RestController
public class LabAuthController {

	private final LabStore store;

	private final PasswordEncoder passwordEncoder;

	private final AuthenticationManager authenticationManager;

	private final SessionAuthenticationStrategy sessionAuthenticationStrategy;

	private final SecurityContextRepository securityContextRepository;

	public LabAuthController(LabStore store, PasswordEncoder passwordEncoder,
			AuthenticationManager authenticationManager, SessionAuthenticationStrategy sessionAuthenticationStrategy,
			SecurityContextRepository securityContextRepository) {
		this.store = store;
		this.passwordEncoder = passwordEncoder;
		this.authenticationManager = authenticationManager;
		this.sessionAuthenticationStrategy = sessionAuthenticationStrategy;
		this.securityContextRepository = securityContextRepository;
	}

	@GetMapping("/api/v1/auth/csrf")
	public ResponseEntity<Void> csrf(CsrfToken csrfToken) {
		csrfToken.getToken();
		return ResponseEntity.noContent().build();
	}

	@PostMapping("/api/v1/auth/signup")
	public ResponseEntity<Map<String, Object>> signup(@Valid @RequestBody Credentials request) {
		LabStore.UserRecord user = this.store
			.createUser(request.email(), this.passwordEncoder.encode(request.password()))
			.orElseThrow(() -> new LabApiException(HttpStatus.CONFLICT, "EMAIL_ALREADY_EXISTS"));
		return ResponseEntity.created(URI.create("/api/v1/users/" + user.id())).body(userBody(user.id(),
				user.email(), user.role()));
	}

	@PostMapping("/api/v1/auth/login")
	public Map<String, Object> login(@Valid @RequestBody Credentials request, HttpServletRequest httpRequest,
			HttpServletResponse httpResponse) {
		Authentication authentication = this.authenticationManager.authenticate(UsernamePasswordAuthenticationToken
			.unauthenticated(LabStore.normalizeEmail(request.email()), request.password()));
		this.sessionAuthenticationStrategy.onAuthentication(authentication, httpRequest, httpResponse);
		SecurityContext context = SecurityContextHolder.createEmptyContext();
		context.setAuthentication(authentication);
		SecurityContextHolder.setContext(context);
		this.securityContextRepository.saveContext(context, httpRequest, httpResponse);

		LabUser user = (LabUser) authentication.getPrincipal();
		this.store.recordLogin(user.email(), httpRequest.getRemoteAddr());
		return userBody(user.id(), user.email(), user.role());
	}

	@GetMapping("/api/v1/users/me")
	public Map<String, Object> me(@AuthenticationPrincipal LabUser user) {
		return userBody(user.id(), user.email(), user.role());
	}

	private static Map<String, Object> userBody(long id, String email, String role) {
		return Map.of("id", id, "email", email, "role", role);
	}

	public record Credentials(@NotBlank @Email @Size(max = 255) String email,
			@NotBlank @Size(min = 8, max = 128) String password) {

		@Override
		public String toString() {
			return "Credentials{email=" + this.email + ", password=***}";
		}

	}

}
