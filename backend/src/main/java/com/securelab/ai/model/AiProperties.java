package com.securelab.ai.model;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * @param provider {@code mock}(기본) 또는 {@code ollama}(6주차)
 * @param mockFollowsInjections Mock 모델이 문서 속 지시를 순진하게 따르는지. 기본 true — 정책 게이트(7주차)가 막아야 할 "넘어가는 모델"을
 * 흉내 낸다.
 * @param maxDocuments 한 질문에 모델로 보내는 문서 수 상한
 * @param maxDocumentChars 문서 한 건당 모델로 보내는 최대 글자 수
 */
@ConfigurationProperties("securelab.ai")
public record AiProperties(@DefaultValue("mock") String provider, @DefaultValue("true") boolean mockFollowsInjections,
		@DefaultValue("5") int maxDocuments, @DefaultValue("2000") int maxDocumentChars) {
}
