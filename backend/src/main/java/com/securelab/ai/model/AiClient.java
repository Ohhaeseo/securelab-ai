package com.securelab.ai.model;

/**
 * LLM 호출 경계. 구현체: {@link MockAiClient}(기본, 5주차), Ollama(6주차).
 *
 * <p>
 * 이 경계를 넘어가는 {@link AiPrompt}에는 <b>이미 인가가 확인된 문서만</b> 들어 있어야 한다 (AI-03). 모델 출력은 신뢰하지 않는다 — 답변은
 * 데이터로만 쓰고, 도구 호출은 "제안"으로만 받는다.
 */
public interface AiClient {

	AiReply complete(AiPrompt prompt);

	String modelName();

}
