package com.securelab.document.dto;

import java.time.Instant;

import com.securelab.document.entity.Document;
import com.securelab.document.entity.Visibility;

/**
 * 목록용. 본문은 넣지 않는다.
 */
public record DocumentSummaryResponse(Long id, Long ownerId, String title, Visibility visibility, Instant updatedAt) {

	public static DocumentSummaryResponse from(Document document) {
		return new DocumentSummaryResponse(document.getId(), document.getOwnerId(), document.getTitle(),
				document.getVisibility(), document.getUpdatedAt());
	}

}
