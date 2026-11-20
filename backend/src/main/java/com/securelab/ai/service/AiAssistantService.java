package com.securelab.ai.service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import java.util.TreeMap;

import com.securelab.ai.dto.AskRequest;
import com.securelab.ai.dto.AskResponse;
import com.securelab.ai.entity.AiSession;
import com.securelab.ai.entity.AiToolCall;
import com.securelab.ai.model.AiClient;
import com.securelab.ai.model.AiPrompt;
import com.securelab.ai.model.AiProperties;
import com.securelab.ai.model.AiReply;
import com.securelab.ai.repository.AiToolCallRepository;
import com.securelab.auth.security.AuthenticatedUser;
import com.securelab.document.dto.DocumentResponse;
import com.securelab.document.service.DocumentService;
import com.securelab.global.web.RequestIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.stereotype.Service;

/**
 * 문서 기반 AI 질문 처리 (M5).
 * <ol>
 * <li>세션 소유자 확인</li>
 * <li><b>인가된 문서만</b> 가져온다 — 문서 서비스 경유, DB 단계에서 거름 (AI-03)</li>
 * <li>모델 호출</li>
 * <li>제안된 도구 호출은 기록만 하고 실행하지 않는다 (실행 정책은 7주차)</li>
 * </ol>
 * 질문·답변·문서 원문은 저장하지 않는다.
 */
@Service
public class AiAssistantService {

	private static final Logger log = LoggerFactory.getLogger(AiAssistantService.class);

	private static final int MAX_KEYWORDS = 5;

	private final AiSessionService sessions;

	private final DocumentService documents;

	private final AiClient aiClient;

	private final AiToolCallRepository toolCalls;

	private final AiProperties properties;

	public AiAssistantService(AiSessionService sessions, DocumentService documents, AiClient aiClient,
			AiToolCallRepository toolCalls, AiProperties properties) {
		this.sessions = sessions;
		this.documents = documents;
		this.aiClient = aiClient;
		this.toolCalls = toolCalls;
		this.properties = properties;
	}

	public AskResponse ask(AuthenticatedUser user, Long sessionId, AskRequest request) {
		AiSession session = this.sessions.getOwned(user, sessionId);

		List<DocumentResponse> authorized = request.documentIdsOrEmpty().isEmpty()
				? this.documents.searchReadableForAssistant(user, keywords(request.question()),
						this.properties.maxDocuments())
				: this.documents.getReadableForAssistant(user, request.documentIdsOrEmpty());

		AiPrompt prompt = PromptBuilder.build(authorized, request.question(), this.properties.maxDocumentChars());
		AiReply reply = this.aiClient.complete(prompt);

		List<AskResponse.ToolCallResult> toolResults = reply.toolCalls().stream().map((proposal) -> {
			this.toolCalls.save(new AiToolCall(session.getId(), proposal.name(), digest(proposal),
					AiToolCall.PROPOSED, "EXECUTION_POLICY_PENDING", MDC.get(RequestIdFilter.MDC_KEY)));
			log.info("ai.tool-proposed session={} tool={} executed=false", session.getId(), proposal.name());
			return new AskResponse.ToolCallResult(proposal.name(), "NOT_EXECUTED");
		}).toList();

		List<AskResponse.Citation> citations = authorized.stream()
			.map((document) -> new AskResponse.Citation(document.id(), document.title()))
			.toList();
		return new AskResponse(reply.answer(), citations, toolResults, this.aiClient.modelName());
	}

	/**
	 * 질문에서 검색어를 뽑는다: 글자·숫자 덩어리 중 2자 이상, 중복 제거, 최대 5개.
	 */
	static List<String> keywords(String question) {
		return Arrays.stream(question.toLowerCase(Locale.ROOT).split("[^\\p{L}\\p{N}]+"))
			.filter((token) -> token.length() >= 2)
			.distinct()
			.limit(MAX_KEYWORDS)
			.toList();
	}

	private static String digest(AiReply.ToolCallProposal proposal) {
		String canonical = proposal.name() + "|" + new TreeMap<>(proposal.arguments());
		try {
			byte[] hash = MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8));
			return HexFormat.of().formatHex(hash);
		}
		catch (NoSuchAlgorithmException ex) {
			throw new IllegalStateException(ex);
		}
	}

}
