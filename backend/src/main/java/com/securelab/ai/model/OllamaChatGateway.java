package com.securelab.ai.model;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Ollama {@code /api/chat} 호출. 스트리밍 없이 JSON 형식 출력을 요청한다. 도구 실행은 하지 않는다 — 출력은 텍스트로만 돌려준다
 * (ADR-0005, ADR-0006).
 */
@Component
public class OllamaChatGateway {

	private final RestClient http;

	private final JsonMapper jsonMapper;

	public OllamaChatGateway(OllamaProperties properties, JsonMapper jsonMapper) {
		requireLoopback(properties);
		SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
		requestFactory.setConnectTimeout(java.time.Duration.ofSeconds(3));
		requestFactory.setReadTimeout(properties.timeout());
		this.http = RestClient.builder().baseUrl(properties.baseUrl().toString()).requestFactory(requestFactory).build();
		this.jsonMapper = jsonMapper;
	}

	/**
	 * @return 모델이 낸 메시지 본문 (JSON 형식을 요청했지만 지켜졌는지는 호출 쪽이 검사한다)
	 */
	public String chat(String model, String system, String user, double temperature, int maxTokens, long seed) {
		Map<String, Object> request = Map.of("model", model, "stream", false, "format", "json", "options",
				Map.of("temperature", temperature, "num_predict", maxTokens, "seed", seed), "messages",
				List.of(Map.of("role", "system", "content", system), Map.of("role", "user", "content", user)));
		String response = this.http.post()
			.uri("/api/chat")
			.contentType(MediaType.APPLICATION_JSON)
			.body(this.jsonMapper.writeValueAsString(request))
			.retrieve()
			.body(String.class);
		JsonNode content = this.jsonMapper.readTree(response).path("message").path("content");
		return content.isString() ? content.stringValue() : "";
	}

	static void requireLoopback(OllamaProperties properties) {
		if (properties.allowRemote()) {
			return;
		}
		String host = properties.baseUrl().getHost();
		try {
			if (host == null || !InetAddress.getByName(host).isLoopbackAddress()) {
				throw new IllegalStateException("Ollama 주소는 루프백만 허용합니다 (문서가 외부로 나가지 않도록): "
						+ properties.baseUrl() + " — 원격을 쓰려면 securelab.ai.ollama.allow-remote=true");
			}
		}
		catch (UnknownHostException ex) {
			throw new IllegalStateException("Ollama 주소를 해석할 수 없습니다: " + properties.baseUrl(), ex);
		}
	}

}
