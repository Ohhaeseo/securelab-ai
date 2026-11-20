package com.securelab.securitylab.audit;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/**
 * IP·계정 식별자가 든 기록의 보존 기간 (기획서 5장: 로컬 실험 7~30일, I-5).
 *
 * @param loginAttempts {@code login_attempts} 보존 기간
 * @param securityEvents {@code security_events} 보존 기간
 */
@ConfigurationProperties("securelab.retention")
public record RetentionProperties(@DefaultValue("30d") Duration loginAttempts,
		@DefaultValue("30d") Duration securityEvents) {
}
