package com.securelab.experiment.dto;

import java.util.List;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/**
 * @param scenario 지금은 {@code AI-01}(간접 Prompt Injection)만 지원
 * @param models {@code mock-1} 또는 로컬 Ollama 모델 이름 (최대 4개)
 * @param repetitions 페이로드·목표 조합마다 반복 횟수 (시드를 바꿔 반복)
 */
public record ExperimentRequest(@Pattern(regexp = "AI-01", message = "지원하는 시나리오는 AI-01입니다.") String scenario,
		@NotEmpty @Size(max = 4) List<@Pattern(regexp = "[A-Za-z0-9._:-]{1,64}") String> models,
		@Min(1) @Max(5) int repetitions) {
}
