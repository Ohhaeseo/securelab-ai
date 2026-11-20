package com.securelab.auth.security;

import java.util.Map;

import jakarta.servlet.DispatcherType;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.crypto.argon2.Argon2PasswordEncoder;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.DelegatingPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.savedrequest.NullRequestCache;

/**
 * API 보안 설정. 세션 쿠키 인증 + CSRF(쿠키 → 헤더) 방식이다. (docs/adr/0001-session-cookie-auth.md)
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

	@Bean
	SecurityFilterChain apiSecurityFilterChain(HttpSecurity http, CsrfTokenRepository csrfTokenRepository,
			JsonAuthenticationEntryPoint authenticationEntryPoint, JsonAccessDeniedHandler accessDeniedHandler)
			throws Exception {
		http.authorizeHttpRequests((authorize) -> authorize
			.dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
			.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
			.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
			.requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
			.requestMatchers(HttpMethod.POST, "/api/v1/auth/signup").permitAll()
			.requestMatchers("/api/v1/security/**").hasRole("ADMIN")
			.anyRequest().authenticated());

		// XSRF-TOKEN 쿠키 값을 X-XSRF-TOKEN 헤더로 보내는 방식. 가입·로그인에도 적용한다(로그인 CSRF 방지).
		http.csrf((csrf) -> csrf.spa().csrfTokenRepository(csrfTokenRepository));

		http.exceptionHandling((exceptions) -> exceptions.authenticationEntryPoint(authenticationEntryPoint)
			.accessDeniedHandler(accessDeniedHandler));

		// 401 때 요청을 세션에 저장하지 않는다(익명 요청이 세션을 만들지 않도록).
		http.requestCache((cache) -> cache.requestCache(new NullRequestCache()));
		http.httpBasic(AbstractHttpConfigurer::disable);
		http.formLogin(AbstractHttpConfigurer::disable);
		return http.build();
	}

	@Bean
	CsrfTokenRepository csrfTokenRepository() {
		CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repository.setCookieCustomizer((cookie) -> cookie.sameSite("Lax").path("/"));
		return repository;
	}

	/**
	 * 새 비밀번호는 Argon2id로 해시한다. 저장 값 앞의 {@code {id}}로 알고리즘을 구분하므로, 나중에 기본 알고리즘을 바꿔도 기존 해시를 검증할 수 있다.
	 */
	@Bean
	PasswordEncoder passwordEncoder() {
		String defaultId = "argon2";
		Map<String, PasswordEncoder> encoders = Map.of(defaultId, Argon2PasswordEncoder.defaultsForSpringSecurity_v5_8(),
				"bcrypt", new BCryptPasswordEncoder());
		return new DelegatingPasswordEncoder(defaultId, encoders);
	}

}
