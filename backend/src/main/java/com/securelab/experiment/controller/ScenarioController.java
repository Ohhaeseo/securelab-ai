package com.securelab.experiment.controller;

import java.util.List;

import com.securelab.experiment.dto.ScenarioResponse;
import com.securelab.experiment.repository.AttackScenarioRepository;

import org.springframework.context.annotation.Profile;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 실험 관리 API (I-6). {@code lab} 프로필에서만 빈이 등록되므로 기본 프로필에서는 엔드포인트 자체가 없다(404). 관리자 역할은
 * SecurityConfig의 URL 규칙이 요구한다.
 */
@Profile("lab")
@RestController
@RequestMapping("/api/v1/scenarios")
public class ScenarioController {

	private final AttackScenarioRepository scenarios;

	public ScenarioController(AttackScenarioRepository scenarios) {
		this.scenarios = scenarios;
	}

	@GetMapping
	@Transactional(readOnly = true)
	public List<ScenarioResponse> list() {
		return this.scenarios.findAllByOrderByIdAsc().stream().map(ScenarioResponse::from).toList();
	}

}
