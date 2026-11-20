package com.securelab.auth.dto;

import com.securelab.user.entity.Role;

public record LoginResponse(Long id, String email, Role role) {
}
