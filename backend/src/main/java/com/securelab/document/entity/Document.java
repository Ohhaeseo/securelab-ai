package com.securelab.document.entity;

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

@Entity
@Table(name = "documents")
public class Document {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 소유자. 생성 후 바꿀 수 없다. 인가 판단의 기준이다. */
	@Column(name = "owner_id", nullable = false, updatable = false)
	private Long ownerId;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, columnDefinition = "text")
	private String content;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private Visibility visibility;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	@Column(name = "updated_at", nullable = false)
	private Instant updatedAt;

	protected Document() {
	}

	private Document(Long ownerId, String title, String content, Visibility visibility) {
		this.ownerId = Objects.requireNonNull(ownerId);
		this.title = Objects.requireNonNull(title);
		this.content = Objects.requireNonNull(content);
		this.visibility = (visibility != null) ? visibility : Visibility.PRIVATE;
		this.createdAt = Instant.now();
		this.updatedAt = this.createdAt;
	}

	public static Document create(Long ownerId, String title, String content, Visibility visibility) {
		return new Document(ownerId, title, content, visibility);
	}

	public boolean isOwnedBy(Long userId) {
		return this.ownerId.equals(userId);
	}

	public boolean isPublic() {
		return this.visibility == Visibility.PUBLIC;
	}

	/**
	 * null인 항목은 바꾸지 않는다 (부분 수정).
	 */
	public void update(String title, String content, Visibility visibility) {
		if (title != null) {
			this.title = title;
		}
		if (content != null) {
			this.content = content;
		}
		if (visibility != null) {
			this.visibility = visibility;
		}
		this.updatedAt = Instant.now();
	}

	public Long getId() {
		return this.id;
	}

	public Long getOwnerId() {
		return this.ownerId;
	}

	public String getTitle() {
		return this.title;
	}

	public String getContent() {
		return this.content;
	}

	public Visibility getVisibility() {
		return this.visibility;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	public Instant getUpdatedAt() {
		return this.updatedAt;
	}

	@Override
	public String toString() {
		// 문서 본문은 로그에 남기지 않는다.
		return "Document{id=" + this.id + ", ownerId=" + this.ownerId + ", visibility=" + this.visibility + "}";
	}

}
