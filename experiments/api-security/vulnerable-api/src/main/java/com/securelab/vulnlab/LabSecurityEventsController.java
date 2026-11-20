package com.securelab.vulnlab;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * ⚠️ VULNERABLE (SEC-02 역할 우회): 관리자용 보안 이벤트 API인데, 역할 규칙이 없어 로그인한 누구나 호출할 수 있다. 응답에는 다른 사용자의
 * 이메일과 IP가 들어 있어 오남용 시 피해가 드러난다.
 */
@RestController
public class LabSecurityEventsController {

	private final LabStore store;

	public LabSecurityEventsController(LabStore store) {
		this.store = store;
	}

	@GetMapping("/api/v1/security/events")
	public Map<String, Object> events() {
		return Map.of("content", this.store.eventsNewestFirst());
	}

}
