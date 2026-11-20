package com.securelab.ai.controller;

import java.net.URI;

import jakarta.validation.Valid;

import com.securelab.ai.dto.AiSessionResponse;
import com.securelab.ai.dto.AskRequest;
import com.securelab.ai.dto.AskResponse;
import com.securelab.ai.service.AiAssistantService;
import com.securelab.ai.service.AiSessionService;
import com.securelab.auth.security.AuthenticatedUser;

import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 문서 기반 AI 도우미. 도구 실행용 공개 API는 두지 않는다 (기획서 6장).
 */
@RestController
@RequestMapping("/api/v1/ai/sessions")
public class AiController {

	private final AiSessionService sessions;

	private final AiAssistantService assistant;

	public AiController(AiSessionService sessions, AiAssistantService assistant) {
		this.sessions = sessions;
		this.assistant = assistant;
	}

	@PostMapping
	public ResponseEntity<AiSessionResponse> start(@AuthenticationPrincipal AuthenticatedUser user) {
		AiSessionResponse session = this.sessions.start(user);
		return ResponseEntity.created(URI.create("/api/v1/ai/sessions/" + session.id())).body(session);
	}

	@PostMapping("/{id}/messages")
	public AskResponse ask(@AuthenticationPrincipal AuthenticatedUser user, @PathVariable Long id,
			@Valid @RequestBody AskRequest request) {
		return this.assistant.ask(user, id, request);
	}

}
