package com.securelab.document.dto;

import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import com.securelab.document.entity.Visibility;

/**
 * 부분 수정. null인 항목은 바꾸지 않는다. 소유자는 바꿀 수 없다.
 */
public record DocumentUpdateRequest(

		@Size(max = 200, message = "제목은 200자 이하여야 합니다.")
		@Pattern(regexp = "(?s).*\\S.*", message = "제목은 공백만으로 이루어질 수 없습니다.") String title,

		@Size(max = 100_000, message = "본문은 100,000자 이하여야 합니다.") String content,

		Visibility visibility) {
}
