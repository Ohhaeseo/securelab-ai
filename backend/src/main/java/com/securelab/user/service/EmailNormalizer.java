package com.securelab.user.service;

import java.util.Locale;

/**
 * 이메일을 앞뒤 공백 제거 + 소문자로 정규화한다. 가입·로그인·조회 모두 같은 규칙을 써야 대소문자만 다른 중복 계정이 생기지 않는다.
 */
public final class EmailNormalizer {

	private EmailNormalizer() {
	}

	public static String normalize(String email) {
		return (email != null) ? email.strip().toLowerCase(Locale.ROOT) : null;
	}

}
