package com.securelab.securitylab;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.UUID;

import com.securelab.TestcontainersConfiguration;
import com.securelab.securitylab.audit.DataRetentionJob;
import org.junit.jupiter.api.Test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * 보존 기간(30일)이 지난 로그인 기록·보안 이벤트만 지운다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
class DataRetentionJobTests {

	@Autowired
	DataRetentionJob job;

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void purgesOnlyRecordsOlderThanRetention() {
		String marker = UUID.randomUUID().toString().substring(0, 8);
		String digest = "a".repeat(56) + marker;
		insertAttempt(digest, "40 days");
		insertAttempt(digest, "1 day");
		insertEvent("old-" + marker, "40 days");
		insertEvent("new-" + marker, "1 day");

		DataRetentionJob.PurgeResult result = this.job.purge();

		assertThat(result.loginAttempts()).isGreaterThanOrEqualTo(1);
		assertThat(result.securityEvents()).isGreaterThanOrEqualTo(1);
		assertThat(this.jdbc.queryForObject("select count(*) from login_attempts where email_digest = ?",
				Integer.class, digest))
			.isEqualTo(1);
		assertThat(this.jdbc.queryForList("select request_id from security_events where request_id like ?",
				String.class, "%-" + marker))
			.containsExactly("new-" + marker);
	}

	private void insertAttempt(String digest, String age) {
		this.jdbc.update("insert into login_attempts (email_digest, ip_address, success, created_at) "
				+ "values (?, '127.0.0.1', false, now() - cast(? as interval))", digest, age);
	}

	private void insertEvent(String requestId, String age) {
		this.jdbc.update("insert into security_events (event_type, severity, action, request_id, occurred_at) "
				+ "values ('ACCESS_DENIED', 'LOW', 'BLOCKED', ?, now() - cast(? as interval))", requestId, age);
	}

}
