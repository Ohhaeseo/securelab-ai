package com.securelab.ai.dto;

import java.util.List;

/**
 * @param answer 모델 답변 (신뢰하지 않는 출력 — 클라이언트는 텍스트로만 표시해야 한다)
 * @param citations 모델에 전달한 문서 (모두 인가 확인됨)
 * @param toolCalls 모델이 제안한 도구 호출과 처리 결과
 */
public record AskResponse(String answer, List<Citation> citations, List<ToolCallResult> toolCalls, String model) {

	public record Citation(Long documentId, String title) {
	}

	/**
	 * @param status {@code NOT_EXECUTED} — 실행 정책(7주차) 도입 전에는 어떤 도구도 실행하지 않는다
	 */
	public record ToolCallResult(String name, String status) {
	}

}
