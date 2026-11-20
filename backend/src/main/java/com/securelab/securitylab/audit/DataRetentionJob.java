package com.securelab.securitylab.audit;

import java.time.Instant;

import com.securelab.securitylab.detection.LoginAttemptRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * 보존 기간이 지난 로그인 기록·보안 이벤트를 지운다. 매일 03:30(서버 시간)에 실행한다.
 */
@Component
public class DataRetentionJob {

	private static final Logger log = LoggerFactory.getLogger(DataRetentionJob.class);

	private final LoginAttemptRepository loginAttempts;

	private final SecurityEventRepository securityEvents;

	private final RetentionProperties properties;

	public DataRetentionJob(LoginAttemptRepository loginAttempts, SecurityEventRepository securityEvents,
			RetentionProperties properties) {
		this.loginAttempts = loginAttempts;
		this.securityEvents = securityEvents;
		this.properties = properties;
	}

	@Scheduled(cron = "${securelab.retention.cron:0 30 3 * * *}")
	@Transactional
	public PurgeResult purge() {
		Instant now = Instant.now();
		int attempts = this.loginAttempts.deleteOlderThan(now.minus(this.properties.loginAttempts()));
		int events = this.securityEvents.deleteOlderThan(now.minus(this.properties.securityEvents()));
		log.info("retention.purged loginAttempts={} securityEvents={}", attempts, events);
		return new PurgeResult(attempts, events);
	}

	public record PurgeResult(int loginAttempts, int securityEvents) {
	}

}
