package com.securelab.ai.model;

import java.util.List;

/**
 * 모델 입력. 문서는 지시가 아니라 데이터로 구분해 담는다.
 *
 * @param systemInstruction 서버가 정한 지시
 * @param documents 인가가 확인된 문서 (DB 단계에서 거른 결과)
 * @param question 사용자 질문 (신뢰하지 않는 입력)
 */
public record AiPrompt(String systemInstruction, List<DocumentContext> documents, String question) {

	public AiPrompt {
		documents = List.copyOf(documents);
	}

	public record DocumentContext(Long id, String title, String content) {

		@Override
		public String toString() {
			// 프롬프트 객체가 로그에 찍혀도 문서 본문은 남지 않게 한다.
			return "DocumentContext{id=" + this.id + "}";
		}

	}

	@Override
	public String toString() {
		return "AiPrompt{documents=" + this.documents + "}";
	}

}
