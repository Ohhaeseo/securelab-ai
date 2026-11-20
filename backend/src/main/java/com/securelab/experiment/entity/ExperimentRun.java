package com.securelab.experiment.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/**
 * 실험 시도 1건 (I-2). 모델 반응({@code modelComplied})과 실제 침해({@code actionExecuted})를 따로 기록한다.
 */
@Entity
@Table(name = "experiment_runs")
public class ExperimentRun {

	public static final String PROTECTED = "PROTECTED";

	public static final String VULNERABLE = "VULNERABLE";

	@Id
	@GeneratedValue(strategy = GenerationType.IDENTITY)
	private Long id;

	@Column(name = "scenario_id", nullable = false)
	private Long scenarioId;

	@Column(name = "session_id")
	private Long sessionId;

	@Column(nullable = false, length = 20)
	private String target;

	@Column(name = "defense_enabled", nullable = false)
	private boolean defenseEnabled;

	@Column(name = "model_complied")
	private Boolean modelComplied;

	@Column(name = "action_executed", nullable = false)
	private boolean actionExecuted;

	@Column(name = "policy_code", length = 40)
	private String policyCode;

	@Column(name = "http_status")
	private Short httpStatus;

	@Column(name = "latency_ms")
	private Integer latencyMs;

	@Column(name = "created_at", nullable = false, updatable = false)
	private Instant createdAt;

	protected ExperimentRun() {
	}

	public ExperimentRun(Long scenarioId, Long sessionId, String target, boolean defenseEnabled, Boolean modelComplied,
			boolean actionExecuted, String policyCode, Integer latencyMs) {
		this.scenarioId = scenarioId;
		this.sessionId = sessionId;
		this.target = target;
		this.defenseEnabled = defenseEnabled;
		this.modelComplied = modelComplied;
		this.actionExecuted = actionExecuted;
		this.policyCode = policyCode;
		this.latencyMs = latencyMs;
		this.createdAt = Instant.now();
	}

	public boolean isActionExecuted() {
		return this.actionExecuted;
	}

}
