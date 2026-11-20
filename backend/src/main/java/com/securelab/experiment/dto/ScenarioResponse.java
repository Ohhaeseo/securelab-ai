package com.securelab.experiment.dto;

import com.securelab.experiment.entity.AttackScenario;
import com.securelab.experiment.entity.ScenarioCategory;

public record ScenarioResponse(String code, ScenarioCategory category, String title, String payload,
		String expectedOutcome) {

	public static ScenarioResponse from(AttackScenario scenario) {
		return new ScenarioResponse(scenario.getCode(), scenario.getCategory(), scenario.getTitle(),
				scenario.getPayload(), scenario.getExpectedOutcome());
	}

}
