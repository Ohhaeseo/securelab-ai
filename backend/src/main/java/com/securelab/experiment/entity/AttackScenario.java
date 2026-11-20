package com.securelab.experiment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 공격 시나리오 카탈로그 (읽기 전용). 시드는 Flyway V3에 있으며 합성 페이로드만 담는다.
 */
@Entity
@Table(name = "attack_scenarios")
public class AttackScenario {

	@Id
	private Long id;

	@Column(nullable = false, length = 20)
	private String code;

	@Enumerated(EnumType.STRING)
	@Column(nullable = false, length = 10)
	private ScenarioCategory category;

	@Column(nullable = false, length = 200)
	private String title;

	@Column(nullable = false, columnDefinition = "text")
	private String payload;

	@Column(name = "expected_outcome", nullable = false, length = 200)
	private String expectedOutcome;

	protected AttackScenario() {
	}

	public Long getId() {
		return this.id;
	}

	public String getCode() {
		return this.code;
	}

	public ScenarioCategory getCategory() {
		return this.category;
	}

	public String getTitle() {
		return this.title;
	}

	public String getPayload() {
		return this.payload;
	}

	public String getExpectedOutcome() {
		return this.expectedOutcome;
	}

}
