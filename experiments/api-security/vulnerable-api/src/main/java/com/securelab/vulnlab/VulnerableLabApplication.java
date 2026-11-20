package com.securelab.vulnlab;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * ⚠️ 의도적으로 취약한 실험 대상 (SEC-01 BOLA, SEC-02 역할 우회).
 *
 * <p>
 * 보호 버전(backend)과 인증 방식·API 형식은 같고, 인가 검사만 빠져 있다. 루프백 주소에서만 실행되며 배포 대상이 아니다.
 * (docs/adr/0002-isolated-vulnerable-lab.md)
 */
@SpringBootApplication
public class VulnerableLabApplication {

	public static void main(String[] args) {
		SpringApplication.run(VulnerableLabApplication.class, args);
	}

}
