package com.securelab.user.controller;

import com.securelab.auth.security.AuthenticatedUser;
import com.securelab.user.dto.UserResponse;
import com.securelab.user.service.UserService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
public class UserController {

	private final UserService userService;

	public UserController(UserService userService) {
		this.userService = userService;
	}

	@GetMapping("/me")
	public UserResponse me(@AuthenticationPrincipal AuthenticatedUser principal) {
		return this.userService.getMe(principal);
	}

	@GetMapping("/{id}")
	public UserResponse user(@AuthenticationPrincipal AuthenticatedUser principal, @PathVariable Long id) {
		return this.userService.getUser(principal, id);
	}

}
