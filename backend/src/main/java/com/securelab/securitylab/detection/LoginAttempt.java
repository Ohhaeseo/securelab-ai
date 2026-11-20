package com.securelab.securitylab.detection;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "login_attempts")
public class LoginAttempt {

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "user_id")
	private Long userId;

	@Column(name = "email_digest", nullable = false, length = 64)
	private String emailDigest;

	@Column(name = "ip_address", length = 45)
	private String ipAddress;

	@Column(nullable = false)
	private boolean success;

	@Column(nullable = false)
	private boolean blocked;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected LoginAttempt() {
	}

	static LoginAttempt success(Long userId, String emailDigest, String ipAddress) {
		return new LoginAttempt(userId, emailDigest, ipAddress, true, false);
	}

	static LoginAttempt failure(Long userId, String emailDigest, String ipAddress) {
		return new LoginAttempt(userId, emailDigest, ipAddress, false, false);
	}

	static LoginAttempt blocked(Long userId, String emailDigest, String ipAddress) {
		return new LoginAttempt(userId, emailDigest, ipAddress, false, true);
	}

	private LoginAttempt(Long userId, String emailDigest, String ipAddress, boolean success, boolean blocked) {
		this.userId = userId;
		this.emailDigest = emailDigest;
		this.ipAddress = ipAddress;
		this.success = success;
		this.blocked = blocked;
		this.createdAt = Instant.now();
	}

	public Long getUserId() {
		return this.userId;
	}

	public String getEmailDigest() {
		return this.emailDigest;
	}

	public boolean isSuccess() {
		return this.success;
	}

	public boolean isBlocked() {
		return this.blocked;
	}

	public Instant getCreatedAt() {
		return this.createdAt;
	}

}
