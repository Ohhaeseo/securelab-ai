package com.securelab.ai.model;

import java.util.stream.Collectors;

import com.securelab.ai.tools.AiTool;
import com.securelab.ai.tools.ToolArgument;
import com.securelab.ai.tools.ToolRegistry;
import com.securelab.user.entity.Role;

/**
 * 로컬 Ollama 모델 (6주차). 서버가 허용한 도구 목록과 출력 형식을 시스템 지시에 넣고, 문서는 이스케이프해 {@code <document>} 데이터 영역에
 * 담는다. 모델이 낸 도구 호출은 제안일 뿐이며 실행 여부는 {@code ToolExecutionPolicyService}가 정한다.
 */
public class OllamaAiClient implements AiClient {

	static final String OUTPUT_FORMAT = """
			반드시 JSON 하나로만 답하라: {"answer": "사용자에게 줄 답변", "tool_calls": [{"name": "도구 이름", "arguments": {}}]}
			도구가 필요 없으면 tool_calls는 빈 배열로 둔다.""";

	private final OllamaChatGateway gateway;

	private final ToolRegistry tools;

	private final ModelOutputParser parser;

	private final String model;

	private final double temperature;

	private final int maxTokens;

	private final long seed;

	public OllamaAiClient(OllamaChatGateway gateway, ToolRegistry tools, ModelOutputParser parser, String model,
			double temperature, int maxTokens, long seed) {
		this.gateway = gateway;
		this.tools = tools;
		this.parser = parser;
		this.model = model;
		this.temperature = temperature;
		this.maxTokens = maxTokens;
		this.seed = seed;
	}

	@Override
	public String modelName() {
		return this.model;
	}

	@Override
	public AiReply complete(AiPrompt prompt) {
		String system = prompt.systemInstruction() + "\n\n" + toolCatalog() + "\n\n" + OUTPUT_FORMAT;
		String content = this.gateway.chat(this.model, system, userMessage(prompt), this.temperature, this.maxTokens,
				this.seed);
		return this.parser.parse(content);
	}

	String toolCatalog() {
		return "사용할 수 있는 도구:\n" + this.tools.all()
			.stream()
			.sorted((a, b) -> a.name().compareTo(b.name()))
			.map(OllamaAiClient::describe)
			.collect(Collectors.joining("\n"));
	}

	private static String describe(AiTool tool) {
		String arguments = tool.arguments()
			.stream()
			.map((argument) -> argument.name() + ":" + ((argument.type() == ToolArgument.Type.INTEGER) ? "int" : "string")
					+ (argument.required() ? "" : "?"))
			.collect(Collectors.joining(", "));
		return "- " + tool.name() + "(" + arguments + ")" + ((tool.requiredRole() == Role.ADMIN) ? " [관리자 전용]" : "");
	}

	static String userMessage(AiPrompt prompt) {
		StringBuilder message = new StringBuilder();
		for (AiPrompt.DocumentContext document : prompt.documents()) {
			message.append("<document id=\"")
				.append(document.id())
				.append("\" title=\"")
				.append(escape(document.title()))
				.append("\">\n")
				.append(escape(document.content()))
				.append("\n</document>\n");
		}
		return message.append("\n질문: ").append(prompt.question()).toString();
	}

	/** 문서 내용이 {@code </document>}로 데이터 영역을 닫고 나오지 못하게 한다. */
	static String escape(String text) {
		return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;");
	}

}
