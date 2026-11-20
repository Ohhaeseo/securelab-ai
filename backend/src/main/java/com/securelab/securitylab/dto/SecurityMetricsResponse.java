package com.securelab.securitylab.dto;

import java.time.Instant;
import java.util.Map;

import com.securelab.securitylab.audit.SecurityEventType;

/**
 * 기간 내 차단 이벤트 집계. 오탐률·지연 같은 실험 지표는 7장 지표 정의에 따라 실험 리포트(M7)에서 계산한다.
 */
public record SecurityMetricsResponse(Instant since, long totalBlocked, Map<SecurityEventType, Long> blockedByType) {
}
