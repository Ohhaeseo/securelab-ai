package com.securelab.ai.model;

import java.util.List;
import java.util.Map;

/**
 * 모델 출력. 신뢰하지 않는다.
 *
 * @param answer 답변 텍스트
 * @param toolCalls 모델이 제안한 도구 호출 — 서버 정책을 거치기 전에는 실행하지 않는다
 * @param formatValid 모델이 요청한 출력 형식(JSON)을 지켰는지 (실험 기록용)
 */
public record AiReply(String answer, List<ToolCallProposal> toolCalls, boolean formatValid) {

	public AiReply {
		toolCalls = List.copyOf(toolCalls);
	}

	public AiReply(String answer, List<ToolCallProposal> toolCalls) {
		this(answer, toolCalls, true);
	}

	/**
	 * @param name 도구 이름 (허용 목록 밖일 수 있음 — AI-04)
	 * @param arguments 인자 (스키마 검증 전)
	 */
	public record ToolCallProposal(String name, Map<String, Object> arguments) {
	}

}
