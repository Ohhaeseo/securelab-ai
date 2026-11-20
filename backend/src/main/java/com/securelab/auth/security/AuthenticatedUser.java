package com.securelab.auth.security;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

import com.securelab.user.entity.Role;
import com.securelab.user.entity.User;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 세션에 저장되는 인증 주체. 객체 단위 인가(소유권 검사)에 쓰는 사용자 {@code id}를 들고 있다.
 */
public final class AuthenticatedUser implements UserDetails, CredentialsContainer {

	@Serial
	private static final long serialVersionUID = 1L;

	private final Long id;

	private final String email;

	private final Role role;

	private final boolean enabled;

	/** 인증 직후 {@link #eraseCredentials()}로 지워진다. 세션에는 남지 않는다. */
	private String passwordHash;

	private AuthenticatedUser(Long id, String email, Role role, boolean enabled, String passwordHash) {
		this.id = id;
		this.email = email;
		this.role = role;
		this.enabled = enabled;
		this.passwordHash = passwordHash;
	}

	public static AuthenticatedUser from(User user) {
		return new AuthenticatedUser(user.getId(), user.getEmail(), user.getRole(), user.isEnabled(),
				user.getPasswordHash());
	}

	public Long id() {
		return this.id;
	}

	public String email() {
		return this.email;
	}

	public Role role() {
		return this.role;
	}

	public boolean isAdmin() {
		return this.role == Role.ADMIN;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority(this.role.authority()));
	}

	@Override
	public String getPassword() {
		return this.passwordHash;
	}

	@Override
	public String getUsername() {
		return this.email;
	}

	@Override
	public boolean isEnabled() {
		return this.enabled;
	}

	@Override
	public void eraseCredentials() {
		this.passwordHash = null;
	}

	@Override
	public String toString() {
		return "AuthenticatedUser{id=" + this.id + ", role=" + this.role + "}";
	}

}
