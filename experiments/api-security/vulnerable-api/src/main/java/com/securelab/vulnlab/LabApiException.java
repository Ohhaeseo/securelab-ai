package com.securelab.vulnlab;

import java.util.LinkedHashMap;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

/**
 * 오류 응답. 보호 버전과 같은 {@code status}/{@code code} 형식을 써서 시나리오 러너가 두 대상을 같은 방식으로 판정할 수 있게 한다.
 */
public class LabApiException extends RuntimeException {

	private final HttpStatus status;

	private final String code;

	public LabApiException(HttpStatus status, String code) {
		super(code);
		this.status = status;
		this.code = code;
	}

	static ResponseEntity<Map<String, Object>> body(HttpStatus status, String code) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("status", status.value());
		body.put("code", code);
		return ResponseEntity.status(status).body(body);
	}

	@RestControllerAdvice
	static class Handler {

		@ExceptionHandler(LabApiException.class)
		ResponseEntity<Map<String, Object>> handle(LabApiException ex) {
			return body(ex.status, ex.code);
		}

		@ExceptionHandler(AuthenticationException.class)
		ResponseEntity<Map<String, Object>> handle(AuthenticationException ex) {
			return body(HttpStatus.UNAUTHORIZED, "INVALID_CREDENTIALS");
		}

		@ExceptionHandler({ MethodArgumentNotValidException.class, HttpMessageNotReadableException.class,
				MethodArgumentTypeMismatchException.class })
		ResponseEntity<Map<String, Object>> handleBadRequest(Exception ex) {
			return body(HttpStatus.BAD_REQUEST, "VALIDATION_FAILED");
		}

		@ExceptionHandler(NoResourceFoundException.class)
		ResponseEntity<Map<String, Object>> handleNotFound(NoResourceFoundException ex) {
			return body(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
		}

	}

}
