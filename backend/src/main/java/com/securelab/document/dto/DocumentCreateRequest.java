package com.securelab.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import com.securelab.document.entity.Visibility;

/**
 * @param visibility 생략하면 PRIVATE
 */
public record DocumentCreateRequest(

		@NotBlank(message = "제목을 입력해 주세요.") @Size(max = 200, message = "제목은 200자 이하여야 합니다.") String title,

		@NotNull(message = "본문을 입력해 주세요.") @Size(max = 100_000, message = "본문은 100,000자 이하여야 합니다.") String content,

		Visibility visibility) {
}
