package com.securelab.document.controller;

import java.net.URI;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import com.securelab.auth.security.AuthenticatedUser;
import com.securelab.document.dto.DocumentCreateRequest;
import com.securelab.document.dto.DocumentResponse;
import com.securelab.document.dto.DocumentSummaryResponse;
import com.securelab.document.dto.DocumentUpdateRequest;
import com.securelab.document.service.DocumentService;
import com.securelab.global.response.PageResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/documents")
public class DocumentController {

	private final DocumentService documentService;

	public DocumentController(DocumentService documentService) {
		this.documentService = documentService;
	}

	@PostMapping
	public ResponseEntity<DocumentResponse> create(@AuthenticationPrincipal AuthenticatedUser user,
			@Valid @RequestBody DocumentCreateRequest request) {
		DocumentResponse document = this.documentService.create(user, request);
		return ResponseEntity.created(URI.create("/api/v1/documents/" + document.id())).body(document);
	}

	/**
	 * 읽을 수 있는 문서 목록 (본인 문서 + 공개 문서), 최신순.
	 */
	@GetMapping
	public PageResponse<DocumentSummaryResponse> list(@AuthenticationPrincipal AuthenticatedUser user,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
		return this.documentService.list(user, page, size);
	}

	@GetMapping("/{id}")
	public DocumentResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
		return this.documentService.get(user, id);
	}

	@PatchMapping("/{id}")
	public DocumentResponse update(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
			@Valid @RequestBody DocumentUpdateRequest request) {
		return this.documentService.update(user, id, request);
	}

	@DeleteMapping("/{id}")
	public ResponseEntity<Void> delete(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
		this.documentService.delete(user, id);
		return ResponseEntity.noContent().build();
	}

}
