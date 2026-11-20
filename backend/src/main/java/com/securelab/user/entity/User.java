package com.securelab.user.entity;

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
@Table(name = "users")
public class User {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	/** 소문자로 정규화된 이메일 (DB CHECK 제약). */
	@Column(nullable = false, unique = true, length = 255)
	private String email;

	@Column(name = "password_hash", nullable = false, length = 255)
	private String passwordHash;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 20)
	private Role role;

	@Column(nullable = false)
	private boolean enabled;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected User() {
	}

	private User(String email, String passwordHash, Role role) {
		this.email = Objects.requireNonNull(email);
		this.passwordHash = Objects.requireNonNull(passwordHash);
		this.role = Objects.requireNonNull(role);
		this.enabled = true;
		this.createdAt = Instant.now();
	}

	/**
	 * @param normalizedEmail {@link com.securelab.user.service.EmailNormalizer}로 정규화한 이메일
	 * @param passwordHash 단방향 해시 (평문 비밀번호를 넘기지 않는다)
	 */
	public static User create(String normalizedEmail, String passwordHash, Role role) {
		return new User(normalizedEmail, passwordHash, role);
	}

	public Long getId() {
		return this.id;
	}

	public String getEmail() {
		return this.email;
	}

	public String getPasswordHash() {
		return this.passwordHash;
	}

	public Role getRole() {
		return this.role;
	}

	public boolean isEnabled() {
		return this.enabled;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

	@Override
	public String toString() {
		// 비밀번호 해시는 로그에 남기지 않는다.
		return "User{id=" + this.id + ", role=" + this.role + ", enabled=" + this.enabled + "}";
	}

}
