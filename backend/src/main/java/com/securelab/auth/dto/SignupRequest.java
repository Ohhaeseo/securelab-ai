package com.securelab.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SignupRequest(

		@NotBlank(message = "이메일을 입력해 주세요.") @Email(message = "이메일 형식이 올바르지 않습니다.")
		@Size(max = 255, message = "이메일은 255자 이하여야 합니다.") String email,

		@NotBlank(message = "비밀번호를 입력해 주세요.")
		@Size(min = 8, max = 128, message = "비밀번호는 8자 이상 128자 이하여야 합니다.") String password) {

	@Override
	public String toString() {
		// 비밀번호가 로그에 남지 않도록 가린다.
		return "SignupRequest{email=" + this.email + ", password=***}";
	}

}
