package com.securelab.ai.policy;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Map;
import java.util.Optional;
import java.util.TreeMap;

import com.securelab.ai.dto.AskResponse;
import com.securelab.ai.entity.AiToolCall;
import com.securelab.ai.model.AiReply;
import com.securelab.ai.repository.AiToolCallRepository;
import com.securelab.ai.tools.AiTool;
import com.securelab.ai.tools.ToolRegistry;
import com.securelab.auth.security.AuthenticatedUser;
import com.securelab.global.exception.BusinessException;
import com.securelab.global.web.RequestIdFilter;
import com.securelab.securitylab.audit.SecurityEventRecorder;
import com.securelab.securitylab.audit.SecurityEventType;
import com.securelab.securitylab.audit.Severity;
import com.securelab.user.entity.Role;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.stereotype.Service;

/**
 * 모델이 제안한 도구 호출의 실행 여부를 서버가 정한다 (M6, AI-01/02/04). 모델 출력은 신뢰하지 않는다.
 *
 * <ol>
 * <li>호출 수 상한 — 한 답변에 3개까지</li>
 * <li>허용 목록 — 등록된 도구 이름만 (AI-04)</li>
 * <li>인자 스키마 — 선언된 인자·타입·범위만 (LLM05)</li>
 * <li>역할 — 관리자 도구는 관리자만 (AI-02)</li>
 * <li>실행 — <b>현재 사용자 권한으로</b> 도메인 서비스를 불러 객체 단위 인가가 그대로 적용된다</li>
 * </ol>
 * 모든 판정은 {@code ai_tool_calls}에, 거부는 보안 이벤트에도 남긴다. 이 서비스를 거치지 않고 도구를 실행하는 경로는 없다 (공개 실행 API 없음).
 */
@Service
public class ToolExecutionPolicyService {

	public static final int MAX_TOOL_CALLS_PER_REPLY = 3;

	public static final String EXECUTED = "EXECUTED";

	public static final String DENIED = "DENIED";

	private static final Logger log = LoggerFactory.getLogger(ToolExecutionPolicyService.class);

	private final ToolRegistry registry;

	private final AiToolCallRepository toolCalls;

	private final SecurityEventRecorder securityEvents;

	public ToolExecutionPolicyService(ToolRegistry registry, AiToolCallRepository toolCalls,
			SecurityEventRecorder securityEvents) {
		this.registry = registry;
		this.toolCalls = toolCalls;
		this.securityEvents = securityEvents;
	}

	/**
	 * @param index 이 답변에서 몇 번째 제안인지 (0부터)
	 */
	public AskResponse.ToolCallResult evaluate(AuthenticatedUser user, Long sessionId,
			AiReply.ToolCallProposal proposal, int index) {
		String name = (proposal.name() != null) ? proposal.name() : "";
		Map<String, Object> arguments = (proposal.arguments() != null) ? proposal.arguments() : Map.of();

		if (index >= MAX_TOOL_CALLS_PER_REPLY) {
			return deny(user, sessionId, name, arguments, ToolDenialReason.TOO_MANY_TOOL_CALLS);
		}
		Optional<AiTool> tool = this.registry.find(name);
		if (tool.isEmpty()) {
			return deny(user, sessionId, name, arguments, ToolDenialReason.NOT_IN_ALLOWLIST);
		}
		Optional<String> schemaError = ToolRegistry.validate(tool.get(), arguments);
		if (schemaError.isPresent()) {
			log.info("ai.tool-schema-invalid tool={} error={}", name, schemaError.get());
			return deny(user, sessionId, name, arguments, ToolDenialReason.SCHEMA_INVALID);
		}
		if (tool.get().requiredRole() == Role.ADMIN && !user.isAdmin()) {
			return deny(user, sessionId, name, arguments, ToolDenialReason.ROLE_REQUIRED);
		}
		try {
			Object output = tool.get().execute(user, arguments);
			record(sessionId, name, arguments, AiToolCall.ALLOWED, null);
			log.info("ai.tool-executed session={} tool={} actor={}", sessionId, name, user.id());
			return new AskResponse.ToolCallResult(name, EXECUTED, null, output);
		}
		catch (BusinessException ex) {
			ToolDenialReason reason = switch (ex.errorCode()) {
				case ACCESS_DENIED -> ToolDenialReason.NOT_PERMITTED;
				case RESOURCE_NOT_FOUND -> ToolDenialReason.NOT_FOUND;
				default -> ToolDenialReason.TOOL_ERROR;
			};
			return deny(user, sessionId, name, arguments, reason);
		}
	}

	private AskResponse.ToolCallResult deny(AuthenticatedUser user, Long sessionId, String name,
			Map<String, Object> arguments, ToolDenialReason reason) {
		record(sessionId, name, arguments, AiToolCall.DENIED, reason.name());
		log.warn("security.ai-tool-denied session={} tool={} reason={} actor={}", sessionId, abbreviate(name),
				reason, user.id());
		this.securityEvents.blocked(SecurityEventType.AI_TOOL_DENIED, reason.severity(), user.id(), reason.name(),
				"AI_TOOL", abbreviate(name));
		return new AskResponse.ToolCallResult(name, DENIED, reason.name(), null);
	}

	private void record(Long sessionId, String name, Map<String, Object> arguments, String decision,
			String reasonCode) {
		this.toolCalls.save(new AiToolCall(sessionId, name.isEmpty() ? "(empty)" : name, digest(name, arguments),
				decision, reasonCode, MDC.get(RequestIdFilter.MDC_KEY)));
	}

	private static String abbreviate(String name) {
		return (name.length() > 100) ? name.substring(0, 100) : name;
	}

	private static String digest(String name, Map<String, Object> arguments) {
		String canonical = name + "|" + new TreeMap<>(arguments);
		try {
			return HexFormat.of()
				.formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
