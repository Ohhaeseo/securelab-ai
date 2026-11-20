package com.securelab.ai.service;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import com.securelab.ai.dto.AskRequest;
import com.securelab.ai.dto.AskResponse;
import com.securelab.ai.entity.AiSession;
import com.securelab.ai.model.AiClient;
import com.securelab.ai.model.AiPrompt;
import com.securelab.ai.model.AiProperties;
import com.securelab.ai.model.AiReply;
import com.securelab.ai.policy.ToolExecutionPolicyService;
import com.securelab.auth.security.AuthenticatedUser;
import com.securelab.document.dto.DocumentResponse;
import com.securelab.document.service.DocumentService;

import org.springframework.stereotype.Service;

/**
 * 문서 기반 AI 질문 처리 (M5).
 * <ol>
 * <li>세션 소유자 확인</li>
 * <li><b>인가된 문서만</b> 가져온다 — 문서 서비스 경유, DB 단계에서 거름 (AI-03)</li>
 * <li>모델 호출</li>
 * <li>제안된 도구 호출은 {@link ToolExecutionPolicyService}가 실행 여부를 정한다 (AI-01/02/04)</li>
 * </ol>
 * 질문·답변·문서 원문은 저장하지 않는다.
 */
@Service
public class AiAssistantService {

	private static final int MAX_KEYWORDS = 5;

	private final AiSessionService sessions;

	private final DocumentService documents;

	private final AiClient aiClient;

	private final ToolExecutionPolicyService toolPolicy;

	private final AiProperties properties;

	public AiAssistantService(AiSessionService sessions, DocumentService documents, AiClient aiClient,
			ToolExecutionPolicyService toolPolicy, AiProperties properties) {
		this.sessions = sessions;
		this.documents = documents;
		this.aiClient = aiClient;
		this.toolPolicy = toolPolicy;
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

		// 모델이 제안한 도구는 서버 정책만이 실행 여부를 정한다 (AI-01/02/04).
		List<AskResponse.ToolCallResult> toolResults = new ArrayList<>();
		for (int i = 0; i < reply.toolCalls().size(); i++) {
			toolResults.add(this.toolPolicy.evaluate(user, session.getId(), reply.toolCalls().get(i), i));
		}

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

}
