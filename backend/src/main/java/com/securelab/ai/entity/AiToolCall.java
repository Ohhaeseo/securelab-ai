package com.securelab.ai.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 모델이 제안한 도구 호출 감사 기록. 인자는 원문 대신 SHA-256만 남긴다.
 */
@Entity
@Table(name = "ai_tool_calls")
public class AiToolCall {

	/** 실행 정책 도입 전(5주차): 기록만 하고 실행하지 않는다. */
	public static final String PROPOSED = "PROPOSED";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "session_id", nullable = false, updatable = false)
	private Long sessionId;

	@Column(name = "tool_name", nullable = false, length = 100)
	private String toolName;

	@Column(name = "args_digest", length = 64)
	private String argsDigest;

	@Column(nullable = false, length = 10)
	private String decision;

	@Column(name = "reason_code", length = 40)
	private String reasonCode;

	@Column(name = "request_id", length = 64)
	private String requestId;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected AiToolCall() {
	}

	public AiToolCall(Long sessionId, String toolName, String argsDigest, String decision, String reasonCode,
			String requestId) {
		this.sessionId = sessionId;
		this.toolName = (toolName.length() > 100) ? toolName.substring(0, 100) : toolName;
		this.argsDigest = argsDigest;
		this.decision = decision;
		this.reasonCode = reasonCode;
		this.requestId = requestId;
		this.createdAt = Instant.now();
	}

	public String getToolName() {
		return this.toolName;
	}

	public String getDecision() {
		return this.decision;
	}

}
