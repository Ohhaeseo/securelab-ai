package com.securelab.auth.security;

import java.util.List;
import java.util.Map;
import java.util.function.Supplier;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
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
			SecurityContextRepository securityContextRepository,
			JsonAuthenticationEntryPoint authenticationEntryPoint, JsonAccessDeniedHandler accessDeniedHandler)
			throws Exception {
		http.authorizeHttpRequests((authorize) -> authorize
			.dispatcherTypeMatchers(DispatcherType.ERROR, DispatcherType.FORWARD).permitAll()
			.requestMatchers("/actuator/health", "/actuator/health/**").permitAll()
			.requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
			.requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
			.requestMatchers(HttpMethod.POST, "/api/v1/auth/signup", "/api/v1/auth/login").permitAll()
			.requestMatchers("/api/v1/security/**", "/api/v1/scenarios/**", "/api/v1/experiments/**")
			.hasRole("ADMIN")
			.anyRequest().authenticated());

		// XSRF-TOKEN 쿠키 값을 X-XSRF-TOKEN 헤더로 보내는 방식. 가입·로그인에도 적용한다(로그인 CSRF 방지).
		http.csrf((csrf) -> csrf.spa().csrfTokenRepository(csrfTokenRepository));

		http.securityContext((context) -> context.securityContextRepository(securityContextRepository));

		http.logout((logout) -> logout.logoutUrl("/api/v1/auth/logout")
			.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT))
			.deleteCookies("JSESSIONID"));

		http.exceptionHandling((exceptions) -> exceptions.authenticationEntryPoint(authenticationEntryPoint)
			.accessDeniedHandler(accessDeniedHandler));

		// 401 때 요청을 세션에 저장하지 않는다(익명 요청이 세션을 만들지 않도록).
		http.requestCache((cache) -> cache.requestCache(new NullRequestCache()));
		http.httpBasic(AbstractHttpConfigurer::disable);
		http.formLogin(AbstractHttpConfigurer::disable);
		return http.build();
	}

	@Bean
	AuthenticationManager authenticationManager(UserDetailsService userDetailsService,
			PasswordEncoder passwordEncoder) {
		DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
		provider.setPasswordEncoder(passwordEncoder);
		return new ProviderManager(provider);
	}

	@Bean
	SecurityContextRepository securityContextRepository() {
		return new DelegatingSecurityContextRepository(new RequestAttributeSecurityContextRepository(),
				new HttpSessionSecurityContextRepository());
	}

	/**
	 * 로그인 직후: 세션 ID 교체(세션 고정 방지) → CSRF 토큰 재발급. 새 토큰은 로그인 응답의 쿠키로 바로 내려간다.
	 */
	@Bean
	SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrfTokenRepository) {
		CsrfAuthenticationStrategy csrfStrategy = new CsrfAuthenticationStrategy(csrfTokenRepository);
		csrfStrategy.setRequestHandler(new EagerCsrfTokenRequestHandler());
		return new CompositeSessionAuthenticationStrategy(
				List.of(new ChangeSessionIdAuthenticationStrategy(), csrfStrategy));
	}

	/**
	 * 기본 처리기는 새 토큰을 지연 생성해 로그인 응답에 쿠키가 실리지 않는다. 토큰을 바로 꺼내 쿠키로 내려보낸다.
	 */
	static final class EagerCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

		private final XorCsrfTokenRequestAttributeHandler delegate = new XorCsrfTokenRequestAttributeHandler();

		@Override
		public void handle(HttpServletRequest request, HttpServletResponse response,
				Supplier<CsrfToken> csrfToken) {
			this.delegate.handle(request, response, csrfToken);
			csrfToken.get();
		}

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
