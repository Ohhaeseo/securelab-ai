package com.securelab.document.dto;

import java.time.Instant;

import com.securelab.document.entity.Document;
import com.securelab.document.entity.Visibility;

public record DocumentResponse(Long id, Long ownerId, String title, String content, Visibility visibility,
		Instant createdAt, Instant updatedAt) {

	public static DocumentResponse from(Document document) {
		return new DocumentResponse(document.getId(), document.getOwnerId(), document.getTitle(),
				document.getContent(), document.getVisibility(), document.getCreatedAt(), document.getUpdatedAt());
	}

}
