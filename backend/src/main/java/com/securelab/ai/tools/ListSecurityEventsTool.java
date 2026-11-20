package com.securelab.ai.tools;

import java.util.List;
import java.util.Map;

import com.securelab.auth.security.AuthenticatedUser;
import com.securelab.securitylab.audit.SecurityEventQueryService;
import com.securelab.user.entity.Role;

import org.springframework.stereotype.Component;

/**
 * 최근 보안 이벤트 요약. 관리자 전용 — 일반 사용자의 AI가 제안하면 역할 검사에서 거부된다 (AI-02).
 */
@Component
public class ListSecurityEventsTool implements AiTool {

	private final SecurityEventQueryService events;

	public ListSecurityEventsTool(SecurityEventQueryService events) {
		this.events = events;
	}

	@Override
	public String name() {
		return "list_security_events";
	}

	@Override
	public Role requiredRole() {
		return Role.ADMIN;
	}

	@Override
	public List<ToolArgument> arguments() {
		return List.of(ToolArgument.optionalInteger("limit", 1, 20));
	}

	@Override
	public Object execute(AuthenticatedUser user, Map<String, Object> arguments) {
		int limit = (int) ToolArgument.longValue(arguments, "limit", 10);
		return this.events.events(null, 0, limit)
			.content()
			.stream()
			.map((event) -> Map.of("eventType", event.eventType(), "reasonCode", String.valueOf(event.reasonCode()),
					"occurredAt", event.occurredAt()))
			.toList();
	}

}
