package com.securelab.ai.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * @param question 질문 (신뢰하지 않는 입력)
 * @param documentIds 참고할 문서를 직접 지정 (생략하면 질문 키워드로 검색). 하나라도 읽을 권한이 없으면 403.
 */
public record AskRequest(

		@NotBlank(message = "질문을 입력해 주세요.") @Size(max = 1000, message = "질문은 1,000자 이하여야 합니다.") String question,

		@Size(max = 5, message = "문서는 5개까지 지정할 수 있습니다.") List<@NotNull @Positive Long> documentIds) {

	public List<Long> documentIdsOrEmpty() {
		return (this.documentIds != null) ? this.documentIds : List.of();
	}

}
