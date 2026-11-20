package com.securelab.securitylab.audit;

public enum SecurityEventType {

	/** 객체·역할 인가 실패 (SEC-01, SEC-02). */
	ACCESS_DENIED,

	/** CSRF 토큰 없음·불일치. */
	CSRF_REJECTED

}
