package com.securelab.vulnlab;

import java.io.Serial;
import java.util.Collection;
import java.util.List;

import org.springframework.security.core.CredentialsContainer;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * 세션에 저장되는 인증 주체. 인증 자체는 보호 버전과 같은 수준으로 동작한다.
 */
public final class LabUser implements UserDetails, CredentialsContainer {

	@Serial
	private static final long serialVersionUID = 1L;

	private final long id;

	private final String email;

	private final String role;

	private String passwordHash;

	LabUser(LabStore.UserRecord record) {
		this.id = record.id();
		this.email = record.email();
		this.role = record.role();
		this.passwordHash = record.passwordHash();
	}

	public long id() {
		return this.id;
	}

	public String email() {
		return this.email;
	}

	public String role() {
		return this.role;
	}

	@Override
	public Collection<? extends GrantedAuthority> getAuthorities() {
		return List.of(new SimpleGrantedAuthority("ROLE_" + this.role));
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
	public void eraseCredentials() {
		this.passwordHash = null;
	}

	@Override
	public String toString() {
		return "LabUser{id=" + this.id + ", role=" + this.role + "}";
	}

}
