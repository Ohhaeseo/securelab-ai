package com.securelab.ai.dto;

import java.time.Instant;

import com.securelab.ai.entity.AiSession;
import com.securelab.ai.entity.AiSessionStatus;

public record AiSessionResponse(Long id, String modelName, AiSessionStatus status, Instant createdAt) {

	public static AiSessionResponse from(AiSession session) {
		return new AiSessionResponse(session.getId(), session.getModelName(), session.getStatus(),
				session.getCreatedAt());
	}

}
