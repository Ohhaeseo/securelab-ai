package com.securelab.securitylab.audit;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SecurityEventRepository extends JpaRepository<SecurityEvent, Long> {

	Page<SecurityEvent> findByEventType(SecurityEventType eventType, Pageable pageable);

	@Modifying
	@Query("delete from SecurityEvent e where e.occurredAt < :cutoff")
	int deleteOlderThan(@Param("cutoff") Instant cutoff);

	@Query("""
			select e.eventType as eventType, count(e) as total
			from SecurityEvent e
			where e.occurredAt >= :since
			group by e.eventType
			""")
	List<EventTypeCount> countByTypeSince(@Param("since") Instant since);

	interface EventTypeCount {

		SecurityEventType getEventType();

		long getTotal();

	}

}
