package com.securelab.experiment.controller;

import jakarta.validation.Valid;

import com.securelab.experiment.dto.BenchmarkResult;
import com.securelab.experiment.dto.ExperimentRequest;
import com.securelab.experiment.service.AiInjectionBenchmark;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 허가된 실험 실행 (lab 프로필 + ADMIN). 실험도 도구 실행 정책을 건너뛰지 않는다 — 벤치마크는 일반 사용자 세션으로 정책을 그대로 거친다.
 */
@Profile("lab")
@RestController
@RequestMapping("/api/v1/experiments")
public class ExperimentController {

	private final AiInjectionBenchmark benchmark;

	public ExperimentController(AiInjectionBenchmark benchmark) {
		this.benchmark = benchmark;
	}

	@PostMapping
	public BenchmarkResult run(@Valid @RequestBody ExperimentRequest request) {
		return this.benchmark.run(request.models(), request.repetitions());
	}

}
