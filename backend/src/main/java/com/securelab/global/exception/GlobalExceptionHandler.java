package com.securelab.global.exception;

import java.util.List;

import com.securelab.global.response.ErrorResponse;
import com.securelab.securitylab.detection.LoginBlockedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.client.RestClientException;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

/**
 * 컨트롤러에서 나온 예외를 공통 에러 형식으로 바꾼다. 내부 예외 메시지·스택 트레이스는 응답에 넣지 않고 서버 로그에만 남긴다.
 */
@RestControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

	private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

	@ExceptionHandler(BusinessException.class)
	ResponseEntity<ErrorResponse> handleBusiness(BusinessException ex) {
		return toResponse(ex.errorCode());
	}

	@ExceptionHandler(LoginBlockedException.class)
	ResponseEntity<ErrorResponse> handleLoginBlocked(LoginBlockedException ex) {
		return ResponseEntity.status(ex.errorCode().status())
			.header(HttpHeaders.RETRY_AFTER, String.valueOf(ex.retryAfterSeconds()))
			.body(ErrorResponse.of(ex.errorCode()));
	}

	@ExceptionHandler(AuthenticationException.class)
	ResponseEntity<ErrorResponse> handleAuthentication(AuthenticationException ex) {
		// 존재하지 않는 계정, 틀린 비밀번호, 비활성 계정을 구분하지 않는다(계정 존재 여부 노출 방지).
		return toResponse(ErrorCode.INVALID_CREDENTIALS);
	}

	/** 로컬 모델(Ollama) 연결 실패 등 외부 HTTP 호출 오류. 내부 주소·메시지는 응답에 넣지 않는다. */
	@ExceptionHandler(RestClientException.class)
	ResponseEntity<ErrorResponse> handleModelUnavailable(RestClientException ex) {
		log.warn("model.unavailable: {}", ex.getClass().getSimpleName());
		return toResponse(ErrorCode.MODEL_UNAVAILABLE);
	}

	@ExceptionHandler(AccessDeniedException.class)
	ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex) {
		return toResponse(ErrorCode.ACCESS_DENIED);
	}

	@ExceptionHandler(Exception.class)
	ResponseEntity<ErrorResponse> handleUnexpected(Exception ex) {
		log.error("Unhandled exception", ex);
		return toResponse(ErrorCode.INTERNAL_ERROR);
	}

	@Override
	protected ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		List<ErrorResponse.FieldError> errors = ex.getBindingResult()
			.getFieldErrors()
			.stream()
			.map(error -> new ErrorResponse.FieldError(error.getField(), error.getDefaultMessage()))
			.toList();
		return ResponseEntity.status(status).body(ErrorResponse.of(ErrorCode.VALIDATION_FAILED, errors));
	}

	@Override
	protected ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex,
			HttpHeaders headers, HttpStatusCode status, WebRequest request) {
		return ResponseEntity.status(status).body(ErrorResponse.of(ErrorCode.MALFORMED_REQUEST));
	}

	/**
	 * 그 밖의 Spring MVC 표준 예외(404, 405, 415, 타입 불일치 등)를 상태 코드에 맞는 에러 코드로 바꾼다.
	 */
	@Override
	protected ResponseEntity<Object> handleExceptionInternal(Exception ex, Object body, HttpHeaders headers,
			HttpStatusCode statusCode, WebRequest request) {
		ErrorCode errorCode = switch (statusCode.value()) {
			case 400 -> ErrorCode.VALIDATION_FAILED;
			case 404 -> ErrorCode.RESOURCE_NOT_FOUND;
			case 405 -> ErrorCode.METHOD_NOT_ALLOWED;
			case 415 -> ErrorCode.UNSUPPORTED_MEDIA_TYPE;
			default -> statusCode.is5xxServerError() ? ErrorCode.INTERNAL_ERROR : ErrorCode.REQUEST_FAILED;
		};
		if (statusCode.is5xxServerError()) {
			log.error("Request failed with status {}", statusCode.value(), ex);
		}
		return ResponseEntity.status(statusCode)
			.headers(headers)
			.body(ErrorResponse.of(statusCode.value(), errorCode));
	}

	private static ResponseEntity<ErrorResponse> toResponse(ErrorCode errorCode) {
		HttpStatus status = errorCode.status();
		return ResponseEntity.status(status).body(ErrorResponse.of(errorCode));
	}

}
