package com.securelab.ai.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

/**
 * 결정적으로 동작하는 가짜 LLM. GPU 없이 정책을 검증하기 위한 것이다 (기획서 2장).
 *
 * <ul>
 * <li>받은 문서의 제목과 앞부분으로 요약 답변을 만든다.</li>
 * <li>{@code mockFollowsInjections=true}이면 문서에 "이전 지시 무시"류 문구가 있을 때 <b>그 지시를 따르는 척</b> 도구 호출을 제안한다
 * (AI-01). 문서에 {@code tool:이름(키=값)}이 있으면 그 도구를, 없으면 {@code search_all_documents}를 제안한다.</li>
 * <li>질문에 {@code tool:이름(키=값)}이 있으면 그 도구 호출을 제안한다 (AI-02/AI-04 실험용).</li>
 * </ul>
 */
@Component
@ConditionalOnProperty(prefix = "securelab.ai", name = "provider", havingValue = "mock", matchIfMissing = true)
public class MockAiClient implements AiClient {

	private static final Pattern INJECTION = Pattern.compile(
			"(이전\\s*지시|앞의\\s*지시|모든\\s*지시).{0,10}(무시|잊)|ignore\\s+(all\\s+)?(previous|prior)\\s+instructions",
			Pattern.CASE_INSENSITIVE);

	/** {@code tool:이름} 또는 {@code tool:이름(키=값,키=값)}. 숫자 값은 Long, 나머지는 문자열. */
	private static final Pattern TOOL_DIRECTIVE = Pattern.compile("tool:([A-Za-z0-9_.-]{1,64})(?:\\(([^)]{0,200})\\))?");

	private static final int SUMMARY_CHARS = 80;

	private final AiProperties properties;

	public MockAiClient(AiProperties properties) {
		this.properties = properties;
	}

	@Override
	public String modelName() {
		return "mock-1";
	}

	@Override
	public AiReply complete(AiPrompt prompt) {
		List<AiReply.ToolCallProposal> toolCalls = new ArrayList<>();
		StringBuilder answer = new StringBuilder();
		if (prompt.documents().isEmpty()) {
			answer.append("참고할 수 있는 문서를 찾지 못했습니다.");
		}
		else {
			answer.append("문서 ").append(prompt.documents().size()).append("건을 참고했습니다.");
			for (AiPrompt.DocumentContext document : prompt.documents()) {
				answer.append("\n- ").append(document.title()).append(": ").append(summarize(document.content()));
				if (this.properties.mockFollowsInjections() && INJECTION.matcher(document.content()).find()) {
					Matcher injected = TOOL_DIRECTIVE.matcher(document.content());
					toolCalls.add(injected.find() ? proposal(injected)
							: new AiReply.ToolCallProposal("search_all_documents", Map.of()));
				}
			}
		}
		Matcher requested = TOOL_DIRECTIVE.matcher(prompt.question());
		if (requested.find()) {
			toolCalls.add(proposal(requested));
		}
		if (!toolCalls.isEmpty()) {
			answer.append("\n(도구 호출을 제안합니다: ")
				.append(String.join(", ", toolCalls.stream().map(AiReply.ToolCallProposal::name).toList()))
				.append(")");
		}
		return new AiReply(answer.toString(), toolCalls);
	}

	private static AiReply.ToolCallProposal proposal(Matcher directive) {
		Map<String, Object> arguments = new LinkedHashMap<>();
		String rawArguments = directive.group(2);
		if (rawArguments != null && !rawArguments.isBlank()) {
			for (String pair : rawArguments.split(",")) {
				String[] keyValue = pair.split("=", 2);
				String key = keyValue[0].strip();
				String value = (keyValue.length > 1) ? keyValue[1].strip() : "";
				arguments.put(key, value.matches("-?\\d{1,18}") ? (Object) Long.valueOf(value) : value);
			}
		}
		return new AiReply.ToolCallProposal(directive.group(1), arguments);
	}

	private static String summarize(String content) {
		String flat = content.replaceAll("\\s+", " ").strip();
		return (flat.length() <= SUMMARY_CHARS) ? flat : flat.substring(0, SUMMARY_CHARS) + "…";
	}

}
