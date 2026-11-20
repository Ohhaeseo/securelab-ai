package com.securelab.securitylab.audit;

import java.time.Duration;
import java.time.Instant;
import java.util.EnumMap;
import java.util.Map;

import com.securelab.global.response.PageResponse;
import com.securelab.securitylab.dto.SecurityEventResponse;
import com.securelab.securitylab.dto.SecurityMetricsResponse;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SecurityEventQueryService {

	private final SecurityEventRepository repository;

	public SecurityEventQueryService(SecurityEventRepository repository) {
		this.repository = repository;
	}

	@Transactional(readOnly = true)
	public PageResponse<SecurityEventResponse> events(SecurityEventType type, int page, int size) {
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
		Page<SecurityEvent> events = (type != null) ? this.repository.findByEventType(type, pageRequest)
				: this.repository.findAll(pageRequest);
		return PageResponse.from(events, SecurityEventResponse::from);
	}

	@Transactional(readOnly = true)
	public SecurityMetricsResponse metrics(Duration window) {
		Instant since = Instant.now().minus(window);
		Map<SecurityEventType, Long> byType = new EnumMap<>(SecurityEventType.class);
		for (SecurityEventType type : SecurityEventType.values()) {
			byType.put(type, 0L);
		}
		this.repository.countByTypeSince(since).forEach((row) -> byType.put(row.getEventType(), row.getTotal()));
		long total = byType.values().stream().mapToLong(Long::longValue).sum();
		return new SecurityMetricsResponse(since, total, byType);
	}

}
