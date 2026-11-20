package com.securelab.vulnlab;

import java.net.URI;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ⚠️ VULNERABLE (SEC-01 BOLA/IDOR): 로그인만 확인하고 문서 소유권은 확인하지 않는다.
 *
 * <p>
 * 보호 버전은 {@code DocumentAccessPolicy}로 읽기(소유자·공개)·수정/삭제(소유자)를 검사한다. 이 앱은 그 검사를 뺐다.
 */
@RestController
@RequestMapping("/api/v1/documents")
public class LabDocumentController {

	private final LabStore store;

	public LabDocumentController(LabStore store) {
		this.store = store;
	}

	@PostMapping
	public ResponseEntity<Map<String, Object>> create(@AuthenticationPrincipal LabUser user,
			@Valid @RequestBody CreateRequest request) {
		LabStore.DocumentRecord document = this.store.createDocument(user.id(), request.title(), request.content(),
				request.visibility());
		return ResponseEntity.created(URI.create("/api/v1/documents/" + document.id())).body(body(document));
	}

	/** ⚠️ VULNERABLE: 남의 비공개 문서까지 모두 돌려준다 (보호 버전은 본인 + 공개만). */
	@GetMapping
	public Map<String, Object> list() {
		List<Map<String, Object>> content = this.store.allDocumentsNewestFirst()
			.stream()
			.map(LabDocumentController::summary)
			.toList();
		return Map.of("content", content, "totalElements", content.size());
	}

	/** ⚠️ VULNERABLE: 소유권 검사 없음. */
	@GetMapping("/{id}")
	public Map<String, Object> get(@PathVariable long id) {
		return body(find(id));
	}

	/** ⚠️ VULNERABLE: 소유권 검사 없음. */
	@PatchMapping("/{id}")
	public Map<String, Object> update(@PathVariable long id, @Valid @RequestBody UpdateRequest request) {
		LabStore.DocumentRecord updated = find(id).update(request.title(), request.content(), request.visibility());
		this.store.saveDocument(updated);
		return body(updated);
	}

	/** ⚠️ VULNERABLE: 소유권 검사 없음. */
	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@PathVariable long id) {
		if (!this.store.deleteDocument(id)) {
			throw new LabApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND");
		}
		return ResponseEntity.noContent().build();
	}

	private LabStore.DocumentRecord find(long id) {
		return this.store.findDocument(id)
			.orElseThrow(() -> new LabApiException(HttpStatus.NOT_FOUND, "RESOURCE_NOT_FOUND"));
	}

	private static Map<String, Object> body(LabStore.DocumentRecord document) {
		Map<String, Object> body = summary(document);
		body.put("content", document.content());
		body.put("createdAt", document.createdAt().toString());
		return body;
	}

	private static Map<String, Object> summary(LabStore.DocumentRecord document) {
		Map<String, Object> body = new LinkedHashMap<>();
		body.put("id", document.id());
		body.put("ownerId", document.ownerId());
		body.put("title", document.title());
		body.put("visibility", document.visibility());
		body.put("updatedAt", document.updatedAt().toString());
		return body;
	}

	public record CreateRequest(@NotBlank @Size(max = 200) String title, @NotNull @Size(max = 100_000) String content,
			@Pattern(regexp = "PRIVATE|PUBLIC") String visibility) {
	}

	public record UpdateRequest(@Size(max = 200) @Pattern(regexp = "(?s).*\\S.*") String title,
			@Size(max = 100_000) String content, @Pattern(regexp = "PRIVATE|PUBLIC") String visibility) {
	}

}
