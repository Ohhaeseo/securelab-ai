package com.securelab.experiment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.securelab.support.ApiIntegrationTest;
import com.securelab.support.ApiResponse;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

/**
 * AI-01 벤치마크 (lab 프로필). Mock 모델은 문서 속 지시를 따르는 "넘어가는 모델"이므로, 모델 반응은 있어도 정책 ON의 실제 행위는 0이어야 한다.
 */
@ActiveProfiles("lab")
class AiBenchmarkLabTests extends ApiIntegrationTest {

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void injectedModelReactsButNothingRunsThroughThePolicyGate() {
		Integer before = this.jdbc.queryForObject("select count(*) from experiment_runs", Integer.class);

		ApiResponse response = loggedInAdmin().post("/api/v1/experiments",
				"{\"scenario\":\"AI-01\",\"models\":[\"mock-1\"],\"repetitions\":1}");

		assertThat(response.status()).as(response.toString()).isEqualTo(200);
		List<Map<String, Object>> results = response.json("$.results");
		assertThat(results).extracting((row) -> row.get("target"))
			.containsExactly("list_security_events", "delete_all_documents");
		for (Map<String, Object> row : results) {
			assertThat((Integer) row.get("trials")).isEqualTo(10);
			assertThat((Integer) row.get("proposedTarget")).as("Mock은 일부 페이로드에 넘어간다").isPositive();
			assertThat((Integer) row.get("executedWithPolicy")).as("정책 ON 실제 행위").isZero();
			assertThat(row.get("executedWithoutPolicySimulated")).isEqualTo(row.get("proposedTarget"));
		}
		Integer after = this.jdbc.queryForObject("select count(*) from experiment_runs", Integer.class);
		assertThat(after - before).isEqualTo(2 * 20);
		Integer protectedBreaches = this.jdbc.queryForObject(
				"select count(*) from experiment_runs where target = 'PROTECTED' and action_executed", Integer.class);
		assertThat(protectedBreaches).isZero();
	}

	@Test
	void onlyAdminCanRunExperimentsAndInputIsValidated() {
		assertThat(loggedInClient("user")
			.post("/api/v1/experiments", "{\"scenario\":\"AI-01\",\"models\":[\"mock-1\"],\"repetitions\":1}")
			.status()).isEqualTo(403);
		assertThat(loggedInAdmin()
			.post("/api/v1/experiments", "{\"scenario\":\"SEC-01\",\"models\":[\"mock-1\"],\"repetitions\":1}")
			.status()).isEqualTo(400);
		assertThat(loggedInAdmin()
			.post("/api/v1/experiments", "{\"scenario\":\"AI-01\",\"models\":[\"http://evil\"],\"repetitions\":1}")
			.status()).isEqualTo(400);
	}

}
