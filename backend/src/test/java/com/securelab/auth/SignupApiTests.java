package com.securelab.auth;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import com.securelab.support.ApiClient;
import com.securelab.support.ApiIntegrationTest;
import com.securelab.support.ApiResponse;
import com.securelab.user.repository.UserRepository;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;

class SignupApiTests extends ApiIntegrationTest {

	@Autowired
	UserRepository userRepository;

	@Test
	void signupCreatesUserWithNormalizedEmailAndArgon2Hash() {
		String local = "new-" + UUID.randomUUID();

		ApiResponse response = newClient().post("/api/v1/auth/signup",
				credentials(local.toUpperCase() + "@SecureLab.TEST", PASSWORD));

		assertThat(response.status()).as(response.toString()).isEqualTo(201);
		assertThat(response.header("Location")).startsWith("/api/v1/users/");
		assertThat(response.<String>json("$.email")).isEqualTo(local + "@securelab.test");
		assertThat(response.<String>json("$.role")).isEqualTo("USER");
		assertThat(response.body()).doesNotContain("password").doesNotContain(PASSWORD);

		String hash = this.userRepository.findByEmail(local + "@securelab.test").orElseThrow().getPasswordHash();
		assertThat(hash).startsWith("{argon2}$argon2id$");
	}

	@Test
	void duplicateEmailIsRejectedIgnoringCase() {
		String email = uniqueEmail("dup");
		ApiClient client = newClient();
		assertThat(client.post("/api/v1/auth/signup", credentials(email, PASSWORD)).status()).isEqualTo(201);

		ApiResponse duplicate = client.post("/api/v1/auth/signup", credentials(email.toUpperCase(), PASSWORD));

		assertThat(duplicate.status()).isEqualTo(409);
		assertThat(duplicate.<String>json("$.code")).isEqualTo("EMAIL_ALREADY_EXISTS");
		assertThat(duplicate.<String>json("$.requestId")).isEqualTo(duplicate.header("X-Request-Id"));
	}

	@Test
	void invalidInputReturnsFieldErrorsWithoutEchoingValues() {
		ApiResponse response = newClient().post("/api/v1/auth/signup", credentials("not-an-email", "short"));

		assertThat(response.status()).isEqualTo(400);
		assertThat(response.<String>json("$.code")).isEqualTo("VALIDATION_FAILED");
		assertThat(response.<List<String>>json("$.errors[*].field")).contains("email", "password");
		assertThat(response.body()).doesNotContain("short").doesNotContain("not-an-email");
	}

	@Test
	void malformedJsonIsRejected() {
		ApiResponse response = newClient().post("/api/v1/auth/signup", "{oops");

		assertThat(response.status()).isEqualTo(400);
		assertThat(response.<String>json("$.code")).isEqualTo("MALFORMED_REQUEST");
	}

	@Test
	void signupWithoutCsrfTokenIsForbidden() {
		ApiResponse response = newClient().postWithoutCsrf("/api/v1/auth/signup",
				credentials(uniqueEmail("csrf"), PASSWORD));

		assertThat(response.status()).isEqualTo(403);
		assertThat(response.<String>json("$.code")).isEqualTo("CSRF_INVALID");
	}

	@Test
	void csrfCookieIsReadableBySameSiteClients() {
		ApiResponse response = newClient().get("/api/v1/auth/csrf");

		assertThat(response.status()).isEqualTo(204);
		assertThat(response.setCookies()).anySatisfy((cookie) -> assertThat(cookie).startsWith("XSRF-TOKEN=")
			.contains("SameSite=Lax")
			.doesNotContain("HttpOnly"));
	}

	@Test
	void protectedApiWithoutLoginReturns401Json() {
		ApiResponse response = newClient().get("/api/v1/users/me");

		assertThat(response.status()).isEqualTo(401);
		assertThat(response.<String>json("$.code")).isEqualTo("UNAUTHENTICATED");
		assertThat(response.setCookies()).noneMatch((cookie) -> cookie.startsWith("JSESSIONID="));
	}

	@Test
	void requestIdIsGeneratedEchoedWhenSafeAndReplacedWhenNot() {
		ApiClient client = newClient();

		assertThat(client.get("/api/v1/users/me").header("X-Request-Id")).startsWith("req-");

		ApiResponse echoed = client.getWithHeader("/api/v1/users/me", "X-Request-Id", "req-test-0001");
		assertThat(echoed.header("X-Request-Id")).isEqualTo("req-test-0001");
		assertThat(echoed.<String>json("$.requestId")).isEqualTo("req-test-0001");

		ApiResponse replaced = client.getWithHeader("/api/v1/users/me", "X-Request-Id", "bad id <script>");
		assertThat(replaced.header("X-Request-Id")).startsWith("req-").doesNotContain("script");
	}

}
