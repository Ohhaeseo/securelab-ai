package com.securelab.ai.model;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

/**
 * 모델 출력 {@code {"answer": "...", "tool_calls": [{"name": "...", "arguments": {...}}]}}을 해석한다. 모델 출력은 신뢰하지 않으므로
 * 형식이 틀리면 도구 호출 없이 원문을 답변으로 쓴다. 인자의 정수는 Long, 문자열은 String으로만 옮기고 나머지 타입은 문자열로 바꾼다 — 스키마
 * 검증은 정책 서비스가 한다.
 */
public final class ModelOutputParser {

	private static final int MAX_TOOL_CALLS = 10;

	private final JsonMapper jsonMapper;

	public ModelOutputParser(JsonMapper jsonMapper) {
		this.jsonMapper = jsonMapper;
	}

	public AiReply parse(String content) {
		String text = stripCodeFence(content == null ? "" : content.strip());
		JsonNode root;
		try {
			root = this.jsonMapper.readTree(text);
		}
		catch (JacksonException ex) {
			return new AiReply(text, List.of(), false);
		}
		if (root == null || !root.isObject()) {
			return new AiReply(text, List.of(), false);
		}
		String answer = root.path("answer").isString() ? root.path("answer").stringValue() : "";
		List<AiReply.ToolCallProposal> toolCalls = new ArrayList<>();
		JsonNode calls = root.path("tool_calls");
		if (calls.isArray()) {
			for (JsonNode call : calls) {
				if (toolCalls.size() >= MAX_TOOL_CALLS) {
					break;
				}
				JsonNode name = call.path("name");
				if (name.isString() && !name.stringValue().isBlank()) {
					toolCalls.add(new AiReply.ToolCallProposal(name.stringValue().strip(), arguments(call.path("arguments"))));
				}
			}
		}
		return new AiReply(answer, toolCalls, true);
	}

	private static Map<String, Object> arguments(JsonNode node) {
		Map<String, Object> arguments = new LinkedHashMap<>();
		if (!node.isObject()) {
			return arguments;
		}
		for (Map.Entry<String, JsonNode> entry : node.properties()) {
			JsonNode value = entry.getValue();
			if (value.isIntegralNumber() && value.canConvertToLong()) {
				arguments.put(entry.getKey(), value.asLong());
			}
			else if (value.isString()) {
				arguments.put(entry.getKey(), value.stringValue());
			}
			else {
				arguments.put(entry.getKey(), value.toString());
			}
		}
		return arguments;
	}

	private static String stripCodeFence(String text) {
		if (text.startsWith("```")) {
			int firstNewline = text.indexOf('\n');
			int lastFence = text.lastIndexOf("```");
			if (firstNewline > 0 && lastFence > firstNewline) {
				return text.substring(firstNewline + 1, lastFence).strip();
			}
		}
		return text;
	}

}
