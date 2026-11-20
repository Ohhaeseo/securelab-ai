package com.securelab.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record LoginRequest(

		@NotBlank(message = "이메일을 입력해 주세요.") @Size(max = 255, message = "이메일은 255자 이하여야 합니다.") String email,

		@NotBlank(message = "비밀번호를 입력해 주세요.") @Size(max = 128, message = "비밀번호는 128자 이하여야 합니다.") String password) {

	@Override
	public String toString() {
		return "LoginRequest{email=" + this.email + ", password=***}";
	}

}
