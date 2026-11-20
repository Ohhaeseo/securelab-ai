package com.securelab.document.controller;

import java.net.URI;

import jakarta.validation.Valid;

import com.securelab.auth.security.AuthenticatedUser;
import com.securelab.document.dto.DocumentCreateRequest;
import com.securelab.document.dto.DocumentResponse;
import com.securelab.document.service.DocumentService;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
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

	@GetMapping("/{id}")
	public DocumentResponse get(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id) {
		return this.documentService.get(user, id);
	}

}
