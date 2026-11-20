package com.securelab.document;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import com.securelab.support.ApiClient;
import com.securelab.support.ApiIntegrationTest;
import com.securelab.support.ApiResponse;
import org.junit.jupiter.api.Test;

/**
 * 문서 목록·수정·삭제와 SEC-01 쓰기 공격(남의 문서 수정·삭제) 회귀 테스트.
 */
class DocumentCrudApiTests extends ApiIntegrationTest {

	@Test
	void listShowsOwnAndPublicDocumentsOnlyWithoutContent() {
		ApiClient alice = loggedInClient("alice");
		ApiClient bob = loggedInClient("bob");
		Integer alicePrivate = create(alice, "alice 비공개", "PRIVATE");
		Integer bobPrivate = create(bob, "bob 비공개", "PRIVATE");
		Integer bobPublic = create(bob, "bob 공개", "PUBLIC");

		ApiResponse list = alice.get("/api/v1/documents?size=100");

		assertThat(list.status()).isEqualTo(200);
		List<Integer> ids = list.json("$.content[*].id");
		assertThat(ids).contains(alicePrivate, bobPublic).doesNotContain(bobPrivate);
		assertThat(list.body()).doesNotContain("\"content\":\"본문");
		assertThat(list.<Integer>json("$.size")).isEqualTo(100);
	}

	@Test
	void listPageSizeIsCapped() {
		ApiClient alice = loggedInClient("alice");

		ApiResponse response = alice.get("/api/v1/documents?size=1000");

		assertThat(response.status()).isEqualTo(400);
		assertThat(response.<String>json("$.code")).isEqualTo("VALIDATION_FAILED");
	}

	@Test
	void ownerCanPartiallyUpdate() {
		ApiClient alice = loggedInClient("alice");
		Integer id = create(alice, "초안", "PRIVATE");

		ApiResponse updated = alice.patch("/api/v1/documents/" + id, "{\"title\":\"최종\",\"visibility\":\"PUBLIC\"}");

		assertThat(updated.status()).as(updated.toString()).isEqualTo(200);
		assertThat(updated.<String>json("$.title")).isEqualTo("최종");
		assertThat(updated.<String>json("$.visibility")).isEqualTo("PUBLIC");
		assertThat(updated.<String>json("$.content")).isEqualTo("본문 초안");
	}

	@Test
	void blankTitleUpdateIsRejected() {
		ApiClient alice = loggedInClient("alice");
		Integer id = create(alice, "제목", "PRIVATE");

		assertThat(alice.patch("/api/v1/documents/" + id, "{\"title\":\"   \"}").status()).isEqualTo(400);
	}

	@Test
	void otherUserCannotUpdateOrDeletePrivateDocument() {
		ApiClient alice = loggedInClient("alice");
		ApiClient bob = loggedInClient("bob");
		Integer id = create(alice, "alice 문서", "PRIVATE");

		assertThat(bob.patch("/api/v1/documents/" + id, "{\"title\":\"탈취\"}").status()).isEqualTo(403);
		assertThat(bob.delete("/api/v1/documents/" + id).status()).isEqualTo(403);

		ApiResponse unchanged = alice.get("/api/v1/documents/" + id);
		assertThat(unchanged.status()).isEqualTo(200);
		assertThat(unchanged.<String>json("$.title")).isEqualTo("alice 문서");
	}

	@Test
	void publicDocumentIsReadOnlyForOthers() {
		ApiClient alice = loggedInClient("alice");
		ApiClient bob = loggedInClient("bob");
		Integer id = create(alice, "공지", "PUBLIC");

		assertThat(bob.get("/api/v1/documents/" + id).status()).isEqualTo(200);
		assertThat(bob.patch("/api/v1/documents/" + id, "{\"visibility\":\"PRIVATE\"}").status()).isEqualTo(403);
		assertThat(bob.delete("/api/v1/documents/" + id).status()).isEqualTo(403);
	}

	@Test
	void adminHasNoSpecialAccessToPrivateDocuments() {
		ApiClient alice = loggedInClient("alice");
		ApiClient admin = loggedInAdmin();
		Integer id = create(alice, "개인 문서", "PRIVATE");

		assertThat(admin.get("/api/v1/documents/" + id).status()).isEqualTo(403);
		assertThat(admin.delete("/api/v1/documents/" + id).status()).isEqualTo(403);
	}

	@Test
	void ownerCanDelete() {
		ApiClient alice = loggedInClient("alice");
		Integer id = create(alice, "지울 문서", "PRIVATE");

		assertThat(alice.delete("/api/v1/documents/" + id).status()).isEqualTo(204);
		assertThat(alice.get("/api/v1/documents/" + id).status()).isEqualTo(404);
	}

	private static Integer create(ApiClient client, String title, String visibility) {
		ApiResponse created = client.post("/api/v1/documents",
				"{\"title\":\"" + title + "\",\"content\":\"본문 " + title + "\",\"visibility\":\"" + visibility + "\"}");
		assertThat(created.status()).as(created.toString()).isEqualTo(201);
		return created.json("$.id");
	}

}
