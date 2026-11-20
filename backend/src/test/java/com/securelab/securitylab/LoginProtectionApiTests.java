package com.securelab.securitylab;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.securelab.securitylab.detection.EmailDigester;
import com.securelab.support.ApiClient;
import com.securelab.support.ApiIntegrationTest;
import com.securelab.support.ApiResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;

/**
 * SEC-03 로그인 반복 실패: (계정 + IP) 3회, IP 6회 실패 / 60초 → 차단. 모든 요청이 127.0.0.1에서 오므로 테스트마다 Redis를 비운다.
 */
@TestPropertySource(properties = { "securelab.login-protection.max-failures-per-account-ip=3",
		"securelab.login-protection.max-failures-per-ip=6", "securelab.login-protection.window=60s" })
class LoginProtectionApiTests extends ApiIntegrationTest {

	private static final String WRONG = "wrong-password-123";

	@Autowired
	StringRedisTemplate redis;

	@Autowired
	JdbcTemplate jdbc;

	@Autowired
	EmailDigester digester;

	@BeforeEach
	void resetCounters() {
		this.redis.execute((RedisCallback<Object>) (connection) -> {
			connection.serverCommands().flushAll();
			return null;
		});
	}

	@Test
	void accountIsBlockedAfterRepeatedFailuresEvenWithCorrectPassword() {
		ApiClient client = newClient();
		String email = signup(client, "victim");
		for (int i = 0; i < 3; i++) {
			assertThat(client.login(email, WRONG).status()).isEqualTo(401);
		}

		ApiResponse blocked = client.login(email, PASSWORD);

		assertThat(blocked.status()).isEqualTo(429);
		assertThat(blocked.<String>json("$.code")).isEqualTo("LOGIN_TEMPORARILY_BLOCKED");
		assertThat(Integer.parseInt(blocked.header("Retry-After"))).isBetween(1, 60);
		assertThat(client.cookie("JSESSIONID")).isEmpty();
	}

	@Test
	void otherAccountFromSameIpIsNotLockedOutByAccountLimit() {
		ApiClient client = newClient();
		String victim = signup(client, "victim");
		String bystander = signup(client, "bystander");
		for (int i = 0; i < 3; i++) {
			client.login(victim, WRONG);
		}

		assertThat(client.login(victim, PASSWORD).status()).isEqualTo(429);
		assertThat(client.login(bystander, PASSWORD).status()).isEqualTo(200);
	}

	@Test
	void ipLimitStopsCredentialStuffingAcrossAccounts() {
		ApiClient attacker = newClient();
		for (int i = 0; i < 6; i++) {
			assertThat(attacker.login(uniqueEmail("stuffed"), WRONG).status()).isEqualTo(401);
		}

		ApiResponse blocked = attacker.login(signup(newClient(), "fresh"), PASSWORD);

		assertThat(blocked.status()).isEqualTo(429);
	}

	@Test
	void successfulLoginResetsAccountCounter() {
		ApiClient client = newClient();
		String email = signup(client, "forgetful");
		for (int round = 0; round < 2; round++) {
			client.login(email, WRONG);
			client.login(email, WRONG);
			assertThat(client.login(email, PASSWORD).status()).as("round %d", round).isEqualTo(200);
		}
	}

	@Test
	void attemptsAreStoredWithHmacDigestOnly() {
		ApiClient client = newClient();
		String email = signup(client, "audited");
		for (int i = 0; i < 4; i++) {
			client.login(email, WRONG);
		}

		List<Boolean> blockedFlags = this.jdbc.queryForList(
				"select blocked from login_attempts where email_digest = ? and success = false order by id",
				Boolean.class, this.digester.digest(email));
		Integer rawEmailRows = this.jdbc.queryForObject(
				"select count(*) from login_attempts where email_digest like '%@%'", Integer.class);

		assertThat(blockedFlags).containsExactly(false, false, false, true);
		assertThat(rawEmailRows).isZero();
	}

	@Test
	void loginBlockedEventIsRecordedOnceWhenBlockStarts() {
		ApiClient client = newClient();
		String email = signup(client, "evented");
		for (int i = 0; i < 5; i++) {
			client.login(email, WRONG);
		}
		String subject = "account:" + this.digester.digest(email).substring(0, 16) + "@127.0.0.1";

		ApiResponse events = loggedInAdmin().get("/api/v1/security/events?type=LOGIN_BLOCKED&size=200");
		List<Object> matches = events.json("$.content[?(@.resourceId == '" + subject + "')]");

		assertThat(matches).hasSize(1);
		assertThat(events.body()).doesNotContain(email);
	}

	private static String signup(ApiClient client, String name) {
		String email = uniqueEmail(name);
		ApiResponse response = client.post("/api/v1/auth/signup", credentials(email, PASSWORD));
		assertThat(response.status()).as(response.toString()).isEqualTo(201);
		return email;
	}

}
