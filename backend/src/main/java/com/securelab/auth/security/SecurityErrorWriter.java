package com.securelab.auth.security;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import jakarta.servlet.http.HttpServletResponse;

import com.securelab.global.exception.ErrorCode;
import com.securelab.global.response.ErrorResponse;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;

/**
 * Spring Security 필터 단계(컨트롤러 밖)에서 나는 401/403을 공통 에러 형식으로 쓴다.
 */
@Component
public class SecurityErrorWriter {

	private final JsonMapper jsonMapper;

	public SecurityErrorWriter(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	public void write(HttpServletResponse response, ErrorCode errorCode) throws IOException {
		response.setStatus(errorCode.status().value());
		response.setContentType(MediaType.APPLICATION_JSON_VALUE);
		response.setCharacterEncoding(StandardCharsets.UTF_8.name());
		this.jsonMapper.writeValue(response.getOutputStream(), ErrorResponse.of(errorCode));
	}

}
