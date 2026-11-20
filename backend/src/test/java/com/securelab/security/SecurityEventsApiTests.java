package com.securelab.security;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import com.securelab.support.ApiClient;
import com.securelab.support.ApiIntegrationTest;
import com.securelab.support.ApiResponse;
import org.junit.jupiter.api.Test;

/**
 * 거부된 요청이 보안 이벤트로 남고, 관리자 API로 requestId까지 추적할 수 있는지 확인한다.
 */
class SecurityEventsApiTests extends ApiIntegrationTest {

	@Test
	void documentAccessDenialIsRecordedWithRequestId() {
		ApiClient alice = loggedInClient("alice");
		ApiClient bob = loggedInClient("bob");
		Integer bobId = bob.get("/api/v1/users/me").json("$.id");
		Integer documentId = alice.post("/api/v1/documents", "{\"title\":\"t\",\"content\":\"기밀 본문\"}").json("$.id");
		String requestId = uniqueRequestId("req-sec01");

		ApiResponse denied = bob.send("GET", "/api/v1/documents/" + documentId, null, false,
				Map.of("X-Request-Id", requestId));
		assertThat(denied.status()).isEqualTo(403);

		Map<String, Object> event = findEvent(loggedInAdmin(), "ACCESS_DENIED", requestId);
		assertThat(event).containsEntry("reasonCode", "DOCUMENT_READ")
			.containsEntry("resourceType", "DOCUMENT")
			.containsEntry("resourceId", String.valueOf(documentId))
			.containsEntry("userId", bobId)
			.containsEntry("severity", "HIGH")
			.containsEntry("action", "BLOCKED");
		assertThat(event.get("ipAddress")).isNotNull();
		assertThat(event.toString()).doesNotContain("기밀 본문");
	}

	@Test
	void csrfRejectionIsRecorded() {
		ApiClient alice = loggedInClient("alice");
		String requestId = uniqueRequestId("req-csrf");

		ApiResponse rejected = alice.send("POST", "/api/v1/documents", "{\"title\":\"t\",\"content\":\"c\"}", false,
				Map.of("X-Request-Id", requestId));
		assertThat(rejected.status()).isEqualTo(403);

		Map<String, Object> event = findEvent(loggedInAdmin(), "CSRF_REJECTED", requestId);
		assertThat(event).containsEntry("reasonCode", "CSRF_TOKEN").containsEntry("resourceType", "ENDPOINT");
	}

	@Test
	void roleViolationIsRecorded() {
		ApiClient user = loggedInClient("user");
		String requestId = uniqueRequestId("req-sec02");

		ApiResponse denied = user.send("GET", "/api/v1/security/events", null, false,
				Map.of("X-Request-Id", requestId));
		assertThat(denied.status()).isEqualTo(403);

		Map<String, Object> event = findEvent(loggedInAdmin(), "ACCESS_DENIED", requestId);
		assertThat(event).containsEntry("reasonCode", "ROLE_REQUIRED")
			.containsEntry("resourceId", "GET /api/v1/security/events");
	}

	@Test
	void metricsCountBlockedEventsByType() {
		ApiClient alice = loggedInClient("alice");
		alice.postWithoutCsrf("/api/v1/documents", "{\"title\":\"t\",\"content\":\"c\"}");

		ApiResponse metrics = loggedInAdmin().get("/api/v1/security/metrics?hours=1");

		assertThat(metrics.status()).isEqualTo(200);
		assertThat(metrics.<Integer>json("$.blockedByType.CSRF_REJECTED")).isGreaterThanOrEqualTo(1);
		assertThat(metrics.<Integer>json("$.totalBlocked")).isGreaterThanOrEqualTo(1);
		assertThat(metrics.<Map<String, Object>>json("$.blockedByType")).containsKeys("ACCESS_DENIED",
				"CSRF_REJECTED");
	}

	private static Map<String, Object> findEvent(ApiClient admin, String type, String requestId) {
		ApiResponse events = admin.get("/api/v1/security/events?type=" + type + "&size=200");
		assertThat(events.status()).as(events.toString()).isEqualTo(200);
		List<Map<String, Object>> matches = events.json("$.content[?(@.requestId == '" + requestId + "')]");
		assertThat(matches).as("requestId=%s 이벤트", requestId).hasSize(1);
		return matches.get(0);
	}

}
