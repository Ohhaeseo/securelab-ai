package com.securelab.user.entity;

public enum Role {

	USER, ADMIN;

	/**
	 * Spring Security 권한 이름 (예: {@code ROLE_ADMIN}).
	 */
	public String authority() {
		return "ROLE_" + name();
	}

}
