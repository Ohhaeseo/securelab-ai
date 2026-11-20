package com.securelab.global.response;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.securelab.global.exception.ErrorCode;
import com.securelab.global.web.RequestIdFilter;

import org.slf4j.MDC;

/**
 * 공통 에러 응답 형식. {@code requestId}는 로그·보안 이벤트와 같은 값이다.
 */
@JsonInclude(JsonInclude.Include.NON_NULL)
public record ErrorResponse(int status, String code, String message, String requestId, List<FieldError> errors) {

	public static ErrorResponse of(ErrorCode errorCode) {
		return new ErrorResponse(errorCode.status().value(), errorCode.name(), errorCode.message(), currentRequestId(),
				null);
	}

	/**
	 * 상태 코드는 실제 응답 값을 쓰고, 코드·메시지는 {@code errorCode}에서 가져온다 (Spring MVC 표준 예외용).
	 */
	public static ErrorResponse of(int status, ErrorCode errorCode) {
		return new ErrorResponse(status, errorCode.name(), errorCode.message(), currentRequestId(), null);
	}

	public static ErrorResponse of(ErrorCode errorCode, List<FieldError> errors) {
		return new ErrorResponse(errorCode.status().value(), errorCode.name(), errorCode.message(), currentRequestId(),
				errors);
	}

	private static String currentRequestId() {
		return MDC.get(RequestIdFilter.MDC_KEY);
	}

	/**
	 * 입력 검증 실패 항목. 거부된 값은 돌려주지 않는다(비밀번호 등이 응답·로그에 남지 않도록).
	 */
	public record FieldError(String field, String message) {
	}

}
