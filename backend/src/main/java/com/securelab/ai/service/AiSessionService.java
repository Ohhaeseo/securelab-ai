package com.securelab.ai.service;

import com.securelab.ai.dto.AiSessionResponse;
import com.securelab.ai.entity.AiSession;
import com.securelab.ai.model.AiClient;
import com.securelab.ai.repository.AiSessionRepository;
import com.securelab.auth.security.AuthenticatedUser;
import com.securelab.global.exception.BusinessException;
import com.securelab.global.exception.ErrorCode;
import com.securelab.securitylab.audit.SecurityEventRecorder;
import com.securelab.securitylab.audit.SecurityEventType;
import com.securelab.securitylab.audit.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AiSessionService {

	private static final Logger log = LoggerFactory.getLogger(AiSessionService.class);

	private final AiSessionRepository sessions;

	private final AiClient aiClient;

	private final SecurityEventRecorder securityEvents;

	public AiSessionService(AiSessionRepository sessions, AiClient aiClient, SecurityEventRecorder securityEvents) {
		this.sessions = sessions;
		this.aiClient = aiClient;
		this.securityEvents = securityEvents;
	}

	@Transactional
	public AiSessionResponse start(AuthenticatedUser user) {
		return AiSessionResponse.from(this.sessions.save(AiSession.start(user.id(), this.aiClient.modelName())));
	}

	/**
	 * 세션 소유자만 쓸 수 있다. 남의 세션이면 403.
	 */
	@Transactional(readOnly = true)
	public AiSession getOwned(AuthenticatedUser user, Long sessionId) {
		AiSession session = this.sessions.findById(sessionId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
		if (!session.isOwnedBy(user.id())) {
			log.warn("security.denied resource=ai_session id={} actor={}", sessionId, user.id());
			this.securityEvents.blocked(SecurityEventType.ACCESS_DENIED, Severity.HIGH, user.id(), "AI_SESSION_ACCESS",
					"AI_SESSION", String.valueOf(sessionId));
			throw new BusinessException(ErrorCode.ACCESS_DENIED);
		}
		return session;
	}

}
