package com.securelab.vulnlab;

import java.io.IOException;
import java.util.List;
import java.util.function.Supplier;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.factory.PasswordEncoderFactories;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.logout.HttpStatusReturningLogoutSuccessHandler;
import org.springframework.security.web.authentication.session.ChangeSessionIdAuthenticationStrategy;
import org.springframework.security.web.authentication.session.CompositeSessionAuthenticationStrategy;
import org.springframework.security.web.authentication.session.SessionAuthenticationStrategy;
import org.springframework.security.web.context.DelegatingSecurityContextRepository;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.context.RequestAttributeSecurityContextRepository;
import org.springframework.security.web.context.SecurityContextRepository;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfAuthenticationStrategy;
import org.springframework.security.web.csrf.CsrfException;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.security.web.csrf.CsrfTokenRepository;
import org.springframework.security.web.csrf.CsrfTokenRequestHandler;
import org.springframework.security.web.csrf.XorCsrfTokenRequestAttributeHandler;
import org.springframework.security.web.savedrequest.NullRequestCache;

/**
 * 인증은 보호 버전과 같다: 세션 쿠키, CSRF(쿠키 → {@code X-XSRF-TOKEN} 헤더), 로그인 시 세션 ID 교체·CSRF 재발급.
 */
@Configuration
@EnableWebSecurity
public class LabSecurityConfig {

	/** 보호 버전과 같은 호스트에서 쿠키가 섞이지 않도록 이름만 다르게 둔다. 헤더 이름은 같다. */
	public static final String CSRF_COOKIE = "VULNLAB-XSRF-TOKEN";

	@Bean
	SecurityFilterChain labSecurityFilterChain(HttpSecurity http, CsrfTokenRepository csrfTokenRepository,
			SecurityContextRepository securityContextRepository) throws Exception {
		http.authorizeHttpRequests((authorize) -> authorize
			.requestMatchers(HttpMethod.GET, "/api/v1/auth/csrf").permitAll()
			.requestMatchers(HttpMethod.POST, "/api/v1/auth/signup", "/api/v1/auth/login").permitAll()
			// ⚠️ VULNERABLE (SEC-02): 보호 버전에 있는 `/api/v1/security/** → hasRole("ADMIN")` 규칙이 없다.
			.anyRequest().authenticated());

		http.csrf((csrf) -> csrf.spa().csrfTokenRepository(csrfTokenRepository));
		http.securityContext((context) -> context.securityContextRepository(securityContextRepository));
		http.logout((logout) -> logout.logoutUrl("/api/v1/auth/logout")
			.logoutSuccessHandler(new HttpStatusReturningLogoutSuccessHandler(HttpStatus.NO_CONTENT)));
		http.exceptionHandling((exceptions) -> exceptions
			.authenticationEntryPoint((request, response, ex) -> writeError(response, 401, "UNAUTHENTICATED"))
			.accessDeniedHandler((request, response, ex) -> writeError(response, 403,
					(ex instanceof CsrfException) ? "CSRF_INVALID" : "ACCESS_DENIED")));
		http.requestCache((cache) -> cache.requestCache(new NullRequestCache()));
		http.httpBasic(AbstractHttpConfigurer::disable);
		http.formLogin(AbstractHttpConfigurer::disable);
		return http.build();
	}

	private static void writeError(HttpServletResponse response, int status, String code) throws IOException {
		response.setStatus(status);
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.getWriter().write("{\"status\":" + status + ",\"code\":\"" + code + "\"}");
	}

	@Bean
	CsrfTokenRepository csrfTokenRepository() {
		CookieCsrfTokenRepository repository = CookieCsrfTokenRepository.withHttpOnlyFalse();
		repository.setCookieName(CSRF_COOKIE);
		repository.setCookieCustomizer((cookie) -> cookie.sameSite("Lax").path("/"));
		return repository;
	}

	@Bean
	PasswordEncoder passwordEncoder() {
		return PasswordEncoderFactories.createDelegatingPasswordEncoder();
	}

	@Bean
	UserDetailsService userDetailsService(LabStore store) {
		return (email) -> store.findUserByEmail(email)
			.map(LabUser::new)
			.orElseThrow(() -> new UsernameNotFoundException("user not found"));
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

	@Bean
	SessionAuthenticationStrategy sessionAuthenticationStrategy(CsrfTokenRepository csrfTokenRepository) {
		CsrfAuthenticationStrategy csrfStrategy = new CsrfAuthenticationStrategy(csrfTokenRepository);
		csrfStrategy.setRequestHandler(new EagerCsrfTokenRequestHandler());
		return new CompositeSessionAuthenticationStrategy(
				List.of(new ChangeSessionIdAuthenticationStrategy(), csrfStrategy));
	}

	static final class EagerCsrfTokenRequestHandler implements CsrfTokenRequestHandler {

		private final XorCsrfTokenRequestAttributeHandler delegate = new XorCsrfTokenRequestAttributeHandler();

		@Override
		public void handle(HttpServletRequest request, HttpServletResponse response,
				Supplier<CsrfToken> csrfToken) {
			this.delegate.handle(request, response, csrfToken);
			csrfToken.get();
		}

	}

}
