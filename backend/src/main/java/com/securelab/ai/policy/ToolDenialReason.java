package com.securelab.ai.policy;

import com.securelab.securitylab.audit.Severity;

/**
 * 도구 호출 거부 사유. {@code ai_tool_calls.reason_code}와 보안 이벤트의 판정 코드로 쓴다.
 */
public enum ToolDenialReason {

	/** 허용 목록에 없는 도구 (AI-04). */
	NOT_IN_ALLOWLIST(Severity.HIGH),

	/** 선언되지 않은 인자, 타입·범위 오류. */
	SCHEMA_INVALID(Severity.MEDIUM),

	/** 관리자 도구를 일반 사용자 세션에서 제안 (AI-02). */
	ROLE_REQUIRED(Severity.HIGH),

	/** 실행 단계의 객체 단위 인가 실패 — 예: 남의 비공개 문서 조회. */
	NOT_PERMITTED(Severity.HIGH),

	NOT_FOUND(Severity.LOW),

	/** 한 답변의 도구 호출 수 상한 초과. */
	TOO_MANY_TOOL_CALLS(Severity.MEDIUM),

	TOOL_ERROR(Severity.LOW);

	private final Severity severity;

	ToolDenialReason(Severity severity) {
		this.severity = severity;
	}

	public Severity severity() {
		return this.severity;
	}

}
