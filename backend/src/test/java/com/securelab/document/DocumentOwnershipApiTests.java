package com.securelab.document;

import static org.assertj.core.api.Assertions.assertThat;

import com.securelab.support.ApiClient;
import com.securelab.support.ApiIntegrationTest;
import com.securelab.support.ApiResponse;
import org.junit.jupiter.api.Test;

/**
 * SEC-01 BOLA/IDOR: 문서 ID를 바꿔 남의 비공개 문서를 읽을 수 없어야 한다.
 */
class DocumentOwnershipApiTests extends ApiIntegrationTest {

	@Test
	void ownerCreatesPrivateDocumentByDefaultAndCanReadIt() {
		ApiClient alice = loggedInClient("alice");

		ApiResponse created = alice.post("/api/v1/documents", "{\"title\":\"급여 명세\",\"content\":\"기밀\"}");

		assertThat(created.status()).as(created.toString()).isEqualTo(201);
		assertThat(created.<String>json("$.visibility")).isEqualTo("PRIVATE");
		assertThat(created.header("Location")).isEqualTo("/api/v1/documents/" + created.json("$.id"));
		assertThat(alice.get(created.header("Location")).status()).isEqualTo(200);
	}

	@Test
	void otherUserCannotReadPrivateDocumentById() {
		ApiClient alice = loggedInClient("alice");
		ApiClient bob = loggedInClient("bob");
		Integer documentId = alice.post("/api/v1/documents", "{\"title\":\"비공개\",\"content\":\"alice만\"}")
			.json("$.id");

		ApiResponse response = bob.get("/api/v1/documents/" + documentId);

		assertThat(response.status()).isEqualTo(403);
		assertThat(response.<String>json("$.code")).isEqualTo("ACCESS_DENIED");
		assertThat(response.body()).doesNotContain("alice만");
	}

	@Test
	void otherUserCanReadPublicDocument() {
		ApiClient alice = loggedInClient("alice");
		ApiClient bob = loggedInClient("bob");
		Integer documentId = alice
			.post("/api/v1/documents", "{\"title\":\"공지\",\"content\":\"전체 공개\",\"visibility\":\"PUBLIC\"}")
			.json("$.id");

		ApiResponse response = bob.get("/api/v1/documents/" + documentId);

		assertThat(response.status()).isEqualTo(200);
		assertThat(response.<String>json("$.content")).isEqualTo("전체 공개");
	}

	@Test
	void ownerIsAlwaysTheAuthenticatedUserNotTheRequestBody() {
		ApiClient alice = loggedInClient("alice");
		Integer aliceId = alice.get("/api/v1/users/me").json("$.id");

		ApiResponse created = alice.post("/api/v1/documents",
				"{\"title\":\"t\",\"content\":\"c\",\"ownerId\":1}");

		assertThat(created.status()).isEqualTo(201);
		assertThat(created.<Integer>json("$.ownerId")).isEqualTo(aliceId);
	}

	@Test
	void missingDocumentIs404AndInvalidIdIs400() {
		ApiClient alice = loggedInClient("alice");

		assertThat(alice.get("/api/v1/documents/999999999").status()).isEqualTo(404);
		assertThat(alice.get("/api/v1/documents/abc").status()).isEqualTo(400);
	}

	@Test
	void invalidDocumentInputIsRejected() {
		ApiClient alice = loggedInClient("alice");

		ApiResponse blankTitle = alice.post("/api/v1/documents", "{\"title\":\" \",\"content\":\"c\"}");
		ApiResponse badVisibility = alice.post("/api/v1/documents",
				"{\"title\":\"t\",\"content\":\"c\",\"visibility\":\"SECRET\"}");

		assertThat(blankTitle.status()).isEqualTo(400);
		assertThat(blankTitle.<String>json("$.errors[0].field")).isEqualTo("title");
		assertThat(badVisibility.status()).isEqualTo(400);
	}

	@Test
	void anonymousUserCannotCreateOrRead() {
		ApiClient anonymous = newClient();

		assertThat(anonymous.post("/api/v1/documents", "{\"title\":\"t\",\"content\":\"c\"}").status())
			.isEqualTo(401);
		assertThat(anonymous.get("/api/v1/documents/1").status()).isEqualTo(401);
	}

}
