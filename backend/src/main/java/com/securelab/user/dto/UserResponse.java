package com.securelab.user.dto;

import java.time.Instant;

import com.securelab.user.entity.Role;
import com.securelab.user.entity.User;

public record UserResponse(Long id, String email, Role role, Instant createdAt) {

	public static UserResponse from(User user) {
		return new UserResponse(user.getId(), user.getEmail(), user.getRole(), user.getCreatedAt());
	}

}
