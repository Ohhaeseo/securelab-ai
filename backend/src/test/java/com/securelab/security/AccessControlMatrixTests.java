package com.securelab.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.securelab.support.ApiClient;
import com.securelab.support.ApiIntegrationTest;
import com.securelab.support.ApiResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * 엔드포인트별 접근 제어 표. 새 API를 추가하면 여기에도 한 줄 추가한다.
 */
class AccessControlMatrixTests extends ApiIntegrationTest {

	@ParameterizedTest(name = "익명 {0} {1} → 401")
	@CsvSource({ "GET, /api/v1/users/me", "GET, /api/v1/users/1", "GET, /api/v1/documents",
			"GET, /api/v1/documents/1", "POST, /api/v1/documents", "PATCH, /api/v1/documents/1",
			"DELETE, /api/v1/documents/1", "GET, /api/v1/security/events", "GET, /api/v1/security/metrics",
			"GET, /actuator/env" })
	void anonymousRequestsToProtectedEndpointsGet401(String method, String path) {
		ApiResponse response = newClient().send(method, path, null, true);

		assertThat(response.status()).as(response.toString()).isEqualTo(401);
		assertThat(response.<String>json("$.code")).isEqualTo("UNAUTHENTICATED");
	}

	@ParameterizedTest(name = "익명 {0} {1} → 허용")
	@CsvSource({ "GET, /actuator/health, 200", "GET, /api/v1/auth/csrf, 204" })
	void publicEndpointsAreReachableAnonymously(String method, String path, int expectedStatus) {
		assertThat(newClient().send(method, path, null, true).status()).isEqualTo(expectedStatus);
	}

	/**
	 * SEC-02 역할 우회: 일반 사용자가 관리 API를 호출하면 403.
	 */
	@ParameterizedTest(name = "일반 사용자 GET {0} → 403")
	@ValueSource(strings = { "/api/v1/security/events", "/api/v1/security/metrics" })
	void regularUserCannotCallAdminApi(String path) {
		ApiClient user = newClient();
		user.signupAndLogin(uniqueEmail("user"), PASSWORD);

		ApiResponse response = user.get(path);

		assertThat(response.status()).isEqualTo(403);
		assertThat(response.<String>json("$.code")).isEqualTo("ACCESS_DENIED");
	}

	@Test
	void apiDocsAreDisabledOutsideLocalProfile() {
		assertThat(newClient().get("/v3/api-docs").status()).isEqualTo(404);
		assertThat(newClient().get("/swagger-ui/index.html").status()).isEqualTo(404);
	}

	@Test
	void unknownPathForLoggedInUserIs404Json() {
		ApiClient user = newClient();
		user.signupAndLogin(uniqueEmail("user"), PASSWORD);

		ApiResponse response = user.get("/api/v1/does-not-exist");

		assertThat(response.status()).isEqualTo(404);
		assertThat(response.<String>json("$.code")).isEqualTo("RESOURCE_NOT_FOUND");
	}

	@Test
	void wrongMethodIs405Json() {
		ApiClient user = newClient();
		user.signupAndLogin(uniqueEmail("user"), PASSWORD);

		ApiResponse response = user.send("PUT", "/api/v1/users/me", "{}", true);

		assertThat(response.status()).isEqualTo(405);
		assertThat(response.<String>json("$.code")).isEqualTo("METHOD_NOT_ALLOWED");
	}

	@Test
	void happyPathFromSignupToLogout() {
		ApiClient client = newClient();
		String email = uniqueEmail("flow");

		assertThat(client.post("/api/v1/auth/signup", credentials(email, PASSWORD)).status()).isEqualTo(201);
		assertThat(client.login(email, PASSWORD).status()).isEqualTo(200);
		ApiResponse created = client.post("/api/v1/documents", "{\"title\":\"회의록\",\"content\":\"안건 1\"}");
		assertThat(created.status()).isEqualTo(201);
		assertThat(client.get(created.header("Location")).<String>json("$.title")).isEqualTo("회의록");
		assertThat(client.post("/api/v1/auth/logout", null).status()).isEqualTo(204);
		assertThat(client.get(created.header("Location")).status()).isEqualTo(401);
	}

}
