package com.securelab.ai.model;

import java.net.URI;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * 로컬 Ollama 설정 (6주차). 문서가 외부로 나가지 않도록 기본은 루프백 주소만 허용한다.
 *
 * @param baseUrl Ollama 주소
 * @param model 기본 모델 ({@code securelab.ai.provider=ollama}일 때 도우미가 쓰는 모델)
 * @param timeout 응답 대기 시간
 * @param temperature 샘플링 온도
 * @param maxTokens 응답 최대 토큰
 * @param allowRemote 루프백이 아닌 주소 허용 여부 (기본 false)
 */
@ConfigurationProperties("securelab.ai.ollama")
public record OllamaProperties(@DefaultValue("http://127.0.0.1:11434") URI baseUrl,
		@DefaultValue("qwen2.5:1.5b") String model, @DefaultValue("120s") Duration timeout,
		@DefaultValue("0.7") double temperature, @DefaultValue("256") int maxTokens,
		@DefaultValue("false") boolean allowRemote) {
}
