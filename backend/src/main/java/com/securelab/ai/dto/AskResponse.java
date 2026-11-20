package com.securelab.ai.dto;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * @param answer 모델 답변 (신뢰하지 않는 출력 — 클라이언트는 텍스트로만 표시해야 한다)
 * @param citations 모델에 전달한 문서 (모두 인가 확인됨)
 * @param toolCalls 모델이 제안한 도구 호출과 서버 정책의 판정
 */
public record AskResponse(String answer, List<Citation> citations, List<ToolCallResult> toolCalls, String model) {

	public record Citation(Long documentId, String title) {
	}

	/**
	 * @param status {@code EXECUTED} 또는 {@code DENIED}
	 * @param reasonCode 거부 사유 (ToolDenialReason)
	 * @param output 실행 결과 (현재 사용자가 볼 수 있는 데이터만)
	 */
	@JsonInclude(JsonInclude.Include.NON_NULL)
	public record ToolCallResult(String name, String status, String reasonCode, Object output) {
	}

}
