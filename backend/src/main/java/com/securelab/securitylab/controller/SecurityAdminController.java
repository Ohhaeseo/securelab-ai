package com.securelab.securitylab.controller;

import java.time.Duration;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;

import com.securelab.global.response.PageResponse;
import com.securelab.securitylab.audit.SecurityEventQueryService;
import com.securelab.securitylab.audit.SecurityEventType;
import com.securelab.securitylab.dto.SecurityEventResponse;
import com.securelab.securitylab.dto.SecurityMetricsResponse;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 관리자 전용 보안 조회 API. 접근 제어는 SecurityConfig의 {@code /api/v1/security/** → ROLE_ADMIN} 규칙이 한다.
 */
@RestController
@RequestMapping("/api/v1/security")
public class SecurityAdminController {

	private final SecurityEventQueryService queryService;

	public SecurityAdminController(SecurityEventQueryService queryService) {
		this.queryService = queryService;
	}

	@GetMapping("/events")
	public PageResponse<SecurityEventResponse> events(@RequestParam(required = false) SecurityEventType type,
			@RequestParam(defaultValue = "0") @Min(0) int page,
			@RequestParam(defaultValue = "50") @Min(1) @Max(200) int size) {
		return this.queryService.events(type, page, size);
	}

	@GetMapping("/metrics")
	public SecurityMetricsResponse metrics(@RequestParam(defaultValue = "24") @Min(1) @Max(720) int hours) {
		return this.queryService.metrics(Duration.ofHours(hours));
	}

}
