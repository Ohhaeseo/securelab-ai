package com.securelab.experiment;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.securelab.support.ApiIntegrationTest;
import com.securelab.support.ApiResponse;
import org.junit.jupiter.api.Test;

import org.springframework.test.context.ActiveProfiles;

/**
 * I-6: 실험 관리 API는 lab 프로필 + 관리자에서만 쓸 수 있다. (기본 프로필에서 404인 것은 AccessControlMatrixTests가 확인)
 */
@ActiveProfiles("lab")
class ScenarioLabProfileTests extends ApiIntegrationTest {

	@Test
	void adminCanListSyntheticScenarioCatalog() {
		ApiResponse response = loggedInAdmin().get("/api/v1/scenarios");

		assertThat(response.status()).as(response.toString()).isEqualTo(200);
		assertThat(response.<List<String>>json("$[*].code")).containsExactly("SEC-01", "SEC-02", "SEC-03", "SEC-04",
				"AI-01", "AI-02", "AI-03", "AI-04");
		assertThat(response.<List<String>>json("$[*].payload")).allSatisfy(
				(payload) -> assertThat(payload).startsWith("[SYNTHETIC]"));
	}

	@Test
	void regularUserAndAnonymousAreBlockedEvenInLabProfile() {
		assertThat(loggedInClient("user").get("/api/v1/scenarios").status()).isEqualTo(403);
		assertThat(newClient().get("/api/v1/scenarios").status()).isEqualTo(401);
	}

}
