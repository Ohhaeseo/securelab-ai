package com.securelab.support;

import java.util.UUID;

import com.securelab.TestcontainersConfiguration;

import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;

/**
 * 실제 서버(임의 포트) + Testcontainers PostgreSQL로 API를 검증하는 통합 테스트의 기반 클래스.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ApiIntegrationTest {

	protected static final String PASSWORD = "correct-horse-battery";

	@LocalServerPort
	int port;

	/** 쿠키 저장소가 따로 있는 새 클라이언트 (다른 브라우저 = 다른 사용자). */
	protected ApiClient newClient() {
		return new ApiClient(this.port);
	}

	protected static String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@securelab.test";
	}

	protected static String credentials(String email, String password) {
		return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
	}

}
