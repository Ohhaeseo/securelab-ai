package com.securelab.securitylab.detection;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

	/** Redis 장애 시 대체 집계: (계정 + IP)의 기간 내 실패 수. 차단으로 거부된 시도는 세지 않는다. */
	@Query("""
			select count(a) from LoginAttempt a
			where a.emailDigest = :digest and a.ipAddress = :ip
			  and a.success = false and a.blocked = false and a.createdAt > :since
			""")
	long countFailures(@Param("digest") String digest, @Param("ip") String ip, @Param("since") Instant since);

	/** Redis 장애 시 대체 집계: IP의 기간 내 실패 수. */
	@Query("""
			select count(a) from LoginAttempt a
			where a.ipAddress = :ip and a.success = false and a.blocked = false and a.createdAt > :since
			""")
	long countFailuresByIp(@Param("ip") String ip, @Param("since") Instant since);

	/** (계정 + IP)의 마지막 성공 시각. 성공하면 계정 카운터가 초기화되는 Redis 동작을 DB 집계에서도 맞춘다. */
	@Query("""
			select max(a.createdAt) from LoginAttempt a
			where a.emailDigest = :digest and a.ipAddress = :ip and a.success = true
			""")
	Instant lastSuccessAt(@Param("digest") String digest, @Param("ip") String ip);

	@Modifying
	@Query("delete from LoginAttempt a where a.createdAt < :cutoff")
	int deleteOlderThan(@Param("cutoff") Instant cutoff);

}
