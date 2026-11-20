package com.securelab.securitylab.dto;

import java.time.Instant;

import com.securelab.securitylab.audit.SecurityEvent;
import com.securelab.securitylab.audit.SecurityEventType;
import com.securelab.securitylab.audit.Severity;

public record SecurityEventResponse(Long id, SecurityEventType eventType, Severity severity, String action,
		String reasonCode, Long userId, String resourceType, String resourceId, String ipAddress, String requestId,
		Instant occurredAt) {

	public static SecurityEventResponse from(SecurityEvent event) {
		return new SecurityEventResponse(event.getId(), event.getEventType(), event.getSeverity(), event.getAction(),
				event.getReasonCode(), event.getUserId(), event.getResourceType(), event.getResourceId(),
				event.getIpAddress(), event.getRequestId(), event.getOccurredAt());
	}

}
