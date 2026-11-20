package com.securelab.global.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Swagger UI / OpenAPI 문서. 기본 프로필에서는 꺼져 있고 local 프로필에서만 열린다 (application-local.yml).
 */
@Configuration
public class OpenApiConfig {

	private static final String SESSION_COOKIE = "sessionCookie";

	@Bean
	OpenAPI secureLabOpenApi() {
		return new OpenAPI()
			.info(new Info().title("SecureLab AI API")
				.version("v1")
				.description("""
						로컬 보안 실험용 API. 세션 쿠키로 인증한다.
						상태 변경 요청에는 XSRF-TOKEN 쿠키 값을 X-XSRF-TOKEN 헤더로 보낸다 (Swagger UI는 자동 처리).
						순서: GET /api/v1/auth/csrf → POST /api/v1/auth/signup → POST /api/v1/auth/login"""))
			.components(new Components().addSecuritySchemes(SESSION_COOKIE,
					new SecurityScheme().type(SecurityScheme.Type.APIKEY)
						.in(SecurityScheme.In.COOKIE)
						.name("JSESSIONID")))
			.addSecurityItem(new SecurityRequirement().addList(SESSION_COOKIE));
	}

}
