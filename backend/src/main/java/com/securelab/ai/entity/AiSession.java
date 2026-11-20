package com.securelab.ai.entity;

import java.time.Instant;
import java.util.Objects;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * AI 도우미 세션. 대화 내용은 저장하지 않는다.
 */
@Entity
@Table(name = "ai_sessions")
public class AiSession {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id", nullable = false, updatable = false)
	private Long userId;

	@Column(name = "model_name", nullable = false, length = 100)
	private String modelName;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private AiSessionStatus status;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected AiSession() {
	}

	private AiSession(Long userId, String modelName) {
		this.userId = Objects.requireNonNull(userId);
		this.modelName = Objects.requireNonNull(modelName);
		this.status = AiSessionStatus.ACTIVE;
		this.createdAt = Instant.now();
	}

	public static AiSession start(Long userId, String modelName) {
		return new AiSession(userId, modelName);
	}

	public boolean isOwnedBy(Long userId) {
		return this.userId.equals(userId);
	}

	public boolean isActive() {
		return this.status == AiSessionStatus.ACTIVE;
	}

	public Long getId() {
		return this.id;
	}

	public Long getUserId() {
		return this.userId;
	}

	public String getModelName() {
		return this.modelName;
	}

	public AiSessionStatus getStatus() {
		return this.status;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

}
