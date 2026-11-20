package com.securelab.auth;

import static org.assertj.core.api.Assertions.assertThat;

import com.securelab.support.ApiClient;
import com.securelab.support.ApiIntegrationTest;
import com.securelab.support.ApiResponse;
import org.junit.jupiter.api.Test;


class LoginLogoutApiTests extends ApiIntegrationTest {

	@Test
	void loginCreatesHardenedSessionAndMeReturnsCurrentUser() {
		String email = uniqueEmail("login");
		ApiClient client = newClient();

		ApiResponse login = client.signupAndLogin(email, PASSWORD);

		assertThat(login.<String>json("$.email")).isEqualTo(email);
		assertThat(login.<String>json("$.role")).isEqualTo("USER");
		assertThat(login.setCookies()).anySatisfy((cookie) -> assertThat(cookie).startsWith("JSESSIONID=")
			.contains("HttpOnly")
			.contains("SameSite=Lax"));

		ApiResponse me = client.get("/api/v1/users/me");
		assertThat(me.status()).isEqualTo(200);
		assertThat(me.<String>json("$.email")).isEqualTo(email);
	}

	@Test
	void emailIsCaseInsensitiveAtLogin() {
		String email = uniqueEmail("case");
		ApiClient client = newClient();
		client.post("/api/v1/auth/signup", credentials(email, PASSWORD));

		assertThat(client.login(email.toUpperCase(), PASSWORD).status()).isEqualTo(200);
	}

	@Test
	void wrongPasswordAndUnknownEmailGetTheSameResponse() {
		String email = uniqueEmail("enum");
		ApiClient client = newClient();
		client.post("/api/v1/auth/signup", credentials(email, PASSWORD));

		ApiResponse wrongPassword = client.login(email, "wrong-password-123");
		ApiResponse unknownEmail = client.login(uniqueEmail("nobody"), "wrong-password-123");

		assertThat(wrongPassword.status()).isEqualTo(401);
		assertThat(unknownEmail.status()).isEqualTo(401);
		assertThat(wrongPassword.<String>json("$.code")).isEqualTo("INVALID_CREDENTIALS");
		assertThat(unknownEmail.<String>json("$.code")).isEqualTo("INVALID_CREDENTIALS");
		assertThat(wrongPassword.<String>json("$.message")).isEqualTo(unknownEmail.<String>json("$.message"));
		assertThat(client.cookie("JSESSIONID")).isEmpty();
	}

	@Test
	void sessionIdChangesOnLoginAndOldIdIsUseless() {
		String email = uniqueEmail("fixation");
		ApiClient client = newClient();
		client.signupAndLogin(email, PASSWORD);
		String firstSessionId = client.cookie("JSESSIONID").orElseThrow();

		assertThat(client.login(email, PASSWORD).status()).isEqualTo(200);
		String secondSessionId = client.cookie("JSESSIONID").orElseThrow();

		assertThat(secondSessionId).isNotEqualTo(firstSessionId);
		ApiClient attacker = newClient();
		attacker.setCookie("JSESSIONID", firstSessionId);
		assertThat(attacker.get("/api/v1/users/me").status()).isEqualTo(401);
	}

	@Test
	void csrfTokenIsRotatedOnLogin() {
		String email = uniqueEmail("rotate");
		ApiClient client = newClient();
		client.post("/api/v1/auth/signup", credentials(email, PASSWORD));
		String tokenBeforeLogin = client.cookie("XSRF-TOKEN").orElseThrow();

		client.login(email, PASSWORD);

		String tokenAfterLogin = client.cookie("XSRF-TOKEN").orElseThrow();
		assertThat(tokenAfterLogin).isNotEqualTo(tokenBeforeLogin);
		ApiResponse withOldToken = client.postWithCsrfToken("/api/v1/auth/logout", null, tokenBeforeLogin);
		assertThat(withOldToken.status()).isEqualTo(403);
		assertThat(withOldToken.<String>json("$.code")).isEqualTo("CSRF_INVALID");
	}

	@Test
	void loginAndLogoutRequireCsrfToken() {
		String email = uniqueEmail("csrf-login");
		ApiClient client = newClient();
		client.post("/api/v1/auth/signup", credentials(email, PASSWORD));

		assertThat(client.postWithoutCsrf("/api/v1/auth/login", credentials(email, PASSWORD)).status())
			.isEqualTo(403);

		client.login(email, PASSWORD);
		assertThat(client.postWithoutCsrf("/api/v1/auth/logout", null).status()).isEqualTo(403);
		assertThat(client.get("/api/v1/users/me").status()).isEqualTo(200);
	}

	@Test
	void logoutInvalidatesSession() {
		ApiClient client = newClient();
		client.signupAndLogin(uniqueEmail("logout"), PASSWORD);
		String sessionId = client.cookie("JSESSIONID").orElseThrow();

		assertThat(client.post("/api/v1/auth/logout", null).status()).isEqualTo(204);

		assertThat(client.get("/api/v1/users/me").status()).isEqualTo(401);
		ApiClient replay = newClient();
		replay.setCookie("JSESSIONID", sessionId);
		assertThat(replay.get("/api/v1/users/me").status()).isEqualTo(401);
	}

	@Test
	void userProfileIsVisibleOnlyToSelfOrAdmin() {
		ApiClient alice = newClient();
		Integer aliceId = alice.signupAndLogin(uniqueEmail("alice"), PASSWORD).json("$.id");
		ApiClient bob = newClient();
		Integer bobId = bob.signupAndLogin(uniqueEmail("bob"), PASSWORD).json("$.id");

		assertThat(alice.get("/api/v1/users/" + aliceId).status()).isEqualTo(200);
		ApiResponse denied = alice.get("/api/v1/users/" + bobId);
		assertThat(denied.status()).isEqualTo(403);
		assertThat(denied.<String>json("$.code")).isEqualTo("ACCESS_DENIED");
		// 없는 ID도 403: 권한 검사를 먼저 해서 계정 존재 여부를 숨긴다.
		assertThat(alice.get("/api/v1/users/999999999").status()).isEqualTo(403);

		ApiClient admin = loggedInAdmin();
		assertThat(admin.get("/api/v1/users/" + bobId).status()).isEqualTo(200);
		assertThat(admin.get("/api/v1/users/999999999").status()).isEqualTo(404);
	}

}
