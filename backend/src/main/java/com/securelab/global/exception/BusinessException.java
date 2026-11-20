package com.securelab.global.exception;

/**
 * 도메인 규칙 위반을 {@link ErrorCode}로 알리는 예외.
 */
public class BusinessException extends RuntimeException {

	private final ErrorCode errorCode;

	public BusinessException(ErrorCode errorCode) {
		super(errorCode.name());
		this.errorCode = errorCode;
	}

	public ErrorCode errorCode() {
		return this.errorCode;
	}

}
