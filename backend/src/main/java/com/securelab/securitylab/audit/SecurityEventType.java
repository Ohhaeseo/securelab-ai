package com.securelab.securitylab.audit;

public enum SecurityEventType {

	/** 객체·역할 인가 실패 (SEC-01, SEC-02). */
	ACCESS_DENIED,

	/** CSRF 토큰 없음·불일치. */
	CSRF_REJECTED,

	/** API 요청 한도 초과 (SEC-04). 윈도우마다 처음 한 번만 기록한다. */
	RATE_LIMITED,

	/** 로그인 실패 누적으로 일시 차단 (SEC-03). 차단이 시작될 때 한 번 기록한다. */
	LOGIN_BLOCKED,

	/** AI가 제안한 도구 호출을 정책이 거부 (AI-01/02/04). */
	AI_TOOL_DENIED

}
