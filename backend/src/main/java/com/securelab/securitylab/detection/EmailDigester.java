package com.securelab.securitylab.detection;

import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.HexFormat;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.securelab.user.service.EmailNormalizer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Component;

/**
 * 이메일을 HMAC-SHA256으로 가명화한다 (I-5). 단순 해시는 이메일 목록으로 사전 대입해 되돌릴 수 있지만, 서버 비밀키가 없으면 HMAC은 되돌릴 수
 * 없다.
 */
@Component
public class EmailDigester {

	private static final Logger log = LoggerFactory.getLogger(EmailDigester.class);

	private static final String ALGORITHM = "HmacSHA256";

	private final SecretKeySpec key;

	public EmailDigester(LoginProtectionProperties properties) {
		byte[] secret;
		if (properties.digestSecret() == null || properties.digestSecret().isBlank()) {
			secret = new byte[32];
			new SecureRandom().nextBytes(secret);
			log.warn("LOGIN_DIGEST_SECRET이 없어 임시 키를 만들었습니다. 재시작하면 다이제스트가 바뀌어 이전 로그인 기록과 연결되지 않습니다.");
		}
		else {
			secret = properties.digestSecret().getBytes(StandardCharsets.UTF_8);
		}
		this.key = new SecretKeySpec(secret, ALGORITHM);
	}

	/**
	 * @return 정규화한 이메일의 HMAC-SHA256 (16진수 64자)
	 */
	public String digest(String email) {
		try {
			Mac mac = Mac.getInstance(ALGORITHM);
			mac.init(this.key);
			byte[] digest = mac.doFinal(EmailNormalizer.normalize(email).getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(digest);
		}
		catch (GeneralSecurityException ex) {
			throw new IllegalStateException("HMAC 계산 실패", ex);
		}
	}

}
