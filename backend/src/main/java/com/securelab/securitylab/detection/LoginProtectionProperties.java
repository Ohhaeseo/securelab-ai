package com.securelab.securitylab.detection;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 로그인 보호 설정 (SEC-03, ADR-0004).
 *
 * @param maxFailuresPerAccountIp (계정 + IP) 조합의 윈도우당 최대 실패 수
 * @param maxFailuresPerIp IP의 윈도우당 최대 실패 수 (여러 계정을 도는 크리덴셜 스터핑 방어)
 * @param window 실패를 세는 기간이자 차단 기간
 * @param digestSecret 이메일 다이제스트 HMAC 키. 비우면 실행할 때마다 임시 키를 만든다.
 */
@ConfigurationProperties("securelab.login-protection")
public record LoginProtectionProperties(@DefaultValue("5") int maxFailuresPerAccountIp,
		@DefaultValue("20") int maxFailuresPerIp, @DefaultValue("15m") Duration window, String digestSecret) {

	@Override
	public String toString() {
		return "LoginProtectionProperties{maxFailuresPerAccountIp=" + this.maxFailuresPerAccountIp
				+ ", maxFailuresPerIp=" + this.maxFailuresPerIp + ", window=" + this.window + ", digestSecret=***}";
	}

}
