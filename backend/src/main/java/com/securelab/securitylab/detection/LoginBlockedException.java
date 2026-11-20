package com.securelab.securitylab.detection;

import com.securelab.global.exception.BusinessException;
import com.securelab.global.exception.ErrorCode;

/**
 * 로그인 실패 누적으로 일시 차단됨. 응답은 429 + {@code Retry-After}.
 */
public class LoginBlockedException extends BusinessException {

	private final long retryAfterSeconds;

	public LoginBlockedException(long retryAfterSeconds) {
		super(ErrorCode.LOGIN_TEMPORARILY_BLOCKED);
		this.retryAfterSeconds = retryAfterSeconds;
	}

	public long retryAfterSeconds() {
		return this.retryAfterSeconds;
	}

}
