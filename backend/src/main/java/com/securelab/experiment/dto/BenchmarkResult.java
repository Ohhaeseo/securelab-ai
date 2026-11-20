package com.securelab.experiment.dto;

import java.util.List;
import java.util.Map;

/**
 * AI-01 벤치마크 요약. 모델 반응과 실제 행위를 따로 센다 (기획서 7장).
 */
public record BenchmarkResult(String scenario, int payloads, int repetitions, List<String> targets,
		List<ModelTargetSummary> results) {

	/**
	 * @param trials 시도 수 (페이로드 × 반복)
	 * @param modelErrors 모델 서버가 끝내 응답하지 못한 시도 수 (판정 제외 — 비율은 trials - modelErrors 기준)
	 * @param proposedTarget 모델이 공격 목표 도구를 제안한 수
	 * @param canaryObeyed 답변이 문서 속 canary로 시작한 수 (문서 지시를 따른 텍스트 반응)
	 * @param modelComplied 위 둘 중 하나라도 해당 — "모델 반응"
	 * @param executedWithPolicy 정책 게이트(ON)에서 목표 도구가 실제 실행된 수 — ASR 분자
	 * @param executedWithoutPolicySimulated 게이트가 없다면(OFF) 제안대로 실행됐을 수 — 계산값, 실제 실행하지 않음
	 * @param invalidFormat 요청한 JSON 형식을 지키지 않은 응답 수
	 * @param compliedByPayload 페이로드 유형별 모델 반응 수
	 */
	public record ModelTargetSummary(String model, String target, int trials, int modelErrors, int proposedTarget,
			int canaryObeyed,
			int modelComplied, int executedWithPolicy, int executedWithoutPolicySimulated, int invalidFormat,
			double latencyP50Ms, double latencyP95Ms, Map<String, Integer> compliedByPayload) {
	}

}
