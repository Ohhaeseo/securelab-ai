package com.securelab.support;

import java.util.UUID;

import com.securelab.TestcontainersConfiguration;
import com.securelab.user.entity.Role;
import com.securelab.user.entity.User;
import com.securelab.user.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Import;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * 실제 서버(임의 포트) + Testcontainers PostgreSQL로 API를 검증하는 통합 테스트의 기반 클래스.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
public abstract class ApiIntegrationTest {

	protected static final String PASSWORD = "correct-horse-battery";

	@LocalServerPort
	int port;

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoder passwordEncoder;

	/** 쿠키 저장소가 따로 있는 새 클라이언트 (다른 브라우저 = 다른 사용자). */
	protected ApiClient newClient() {
		return new ApiClient(this.port);
	}

	/** 새 일반 사용자로 가입·로그인한 클라이언트. */
	protected ApiClient loggedInClient(String name) {
		ApiClient client = newClient();
		client.signupAndLogin(uniqueEmail(name), PASSWORD);
		return client;
	}

	/** 관리자는 가입 API로 만들 수 없으므로 DB에 직접 만든 뒤 로그인한다. */
	protected ApiClient loggedInAdmin() {
		String email = uniqueEmail("admin");
		this.userRepository.save(User.create(email, this.passwordEncoder.encode(PASSWORD), Role.ADMIN));
		ApiClient admin = newClient();
		ApiResponse login = admin.login(email, PASSWORD);
		if (login.status() != 200) {
			throw new IllegalStateException("관리자 로그인 실패: " + login);
		}
		return admin;
	}

	protected static String uniqueEmail(String prefix) {
		return prefix + "-" + UUID.randomUUID() + "@securelab.test";
	}

	protected static String uniqueRequestId(String prefix) {
		return prefix + "-" + UUID.randomUUID().toString().substring(0, 8);
	}

	protected static String credentials(String email, String password) {
		return "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}";
	}

}
