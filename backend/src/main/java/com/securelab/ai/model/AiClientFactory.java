package com.securelab.ai.model;

import java.util.regex.Pattern;

import com.securelab.ai.tools.ToolRegistry;
import tools.jackson.databind.json.JsonMapper;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;

/**
 * 모델 이름으로 클라이언트를 만든다. {@code mock-1}은 MockAiClient, 나머지는 로컬 Ollama 모델. 벤치마크가 여러 모델을 같은 조건으로 비교할
 * 때 쓴다.
 */
@Component
public class AiClientFactory {

	public static final String MOCK_MODEL = "mock-1";

	private static final Pattern MODEL_NAME = Pattern.compile("[A-Za-z0-9._:-]{1,64}");

	private final AiProperties aiProperties;

	private final OllamaProperties ollamaProperties;

	private final OllamaChatGateway gateway;

	private final ToolRegistry tools;

	private final ModelOutputParser parser;

	public AiClientFactory(AiProperties aiProperties, OllamaProperties ollamaProperties, OllamaChatGateway gateway,
			ToolRegistry tools, JsonMapper jsonMapper) {
		this.aiProperties = aiProperties;
		this.ollamaProperties = ollamaProperties;
		this.gateway = gateway;
		this.tools = tools;
		this.parser = new ModelOutputParser(jsonMapper);
	}

	public AiClient forModel(String model, long seed) {
		if (model == null || !MODEL_NAME.matcher(model).matches()) {
			throw new IllegalArgumentException("모델 이름 형식이 올바르지 않습니다");
		}
		if (MOCK_MODEL.equals(model)) {
			return new MockAiClient(this.aiProperties);
		}
		return new OllamaAiClient(this.gateway, this.tools, this.parser, model, this.ollamaProperties.temperature(),
				this.ollamaProperties.maxTokens(), seed);
	}

	/**
	 * {@code securelab.ai.provider=ollama}이면 도우미가 Ollama 기본 모델을 쓴다.
	 */
	@Component
	static class OllamaProviderConfiguration {

		@Bean
		@ConditionalOnProperty(prefix = "securelab.ai", name = "provider", havingValue = "ollama")
		AiClient ollamaAiClient(AiClientFactory factory, OllamaProperties properties) {
			return factory.forModel(properties.model(), 42);
		}

	}

}
