package com.securelab.securitylab.audit;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "security_events")
public class SecurityEvent {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id")
	private Long userId;

	@Enumerated(EnumType.STRING)
	@Column(name = "event_type", nullable = false, length = 40)
	private SecurityEventType eventType;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Severity severity;

	/** 서버가 한 조치 (예: BLOCKED). */
	@Column(nullable = false, length = 20)
	private String action;

	@Column(name = "reason_code", length = 40)
	private String reasonCode;

	@Column(name = "resource_type", length = 40)
	private String resourceType;

	@Column(name = "resource_id", length = 255)
	private String resourceId;

	@Column(name = "ip_address", length = 45)
	private String ipAddress;

	@Column(name = "request_id", length = 64)
	private String requestId;

	@Column(name = "occurred_at", nullable = false, updatable = false)
	private Instant occurredAt;

	protected SecurityEvent() {
	}

	SecurityEvent(Long userId, SecurityEventType eventType, Severity severity, String action, String reasonCode,
			String resourceType, String resourceId, String ipAddress, String requestId) {
		this.userId = userId;
		this.eventType = eventType;
		this.severity = severity;
		this.action = action;
		this.reasonCode = reasonCode;
		this.resourceType = resourceType;
		this.resourceId = truncate(resourceId, 255);
		this.ipAddress = truncate(ipAddress, 45);
		this.requestId = truncate(requestId, 64);
		this.occurredAt = Instant.now();
	}

	private static String truncate(String value, int max) {
		return (value != null && value.length() > max) ? value.substring(0, max) : value;
	}

	public Long getId() {
		return this.id;
	}

	public Long getUserId() {
		return this.userId;
	}

	public SecurityEventType getEventType() {
		return this.eventType;
	}

	public Severity getSeverity() {
		return this.severity;
	}

	public String getAction() {
		return this.action;
	}

	public String getReasonCode() {
		return this.reasonCode;
	}

	public String getResourceType() {
		return this.resourceType;
	}

	public String getResourceId() {
		return this.resourceId;
	}

	public String getIpAddress() {
		return this.ipAddress;
	}

	public String getRequestId() {
		return this.requestId;
	}

	public Instant getOccurredAt() {
		return this.occurredAt;
	}

}
