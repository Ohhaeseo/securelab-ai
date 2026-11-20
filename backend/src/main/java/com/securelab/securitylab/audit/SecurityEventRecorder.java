package com.securelab.securitylab.audit;

import jakarta.servlet.http.HttpServletRequest;

import com.securelab.global.web.RequestIdFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 보안 판정을 {@code security_events}에 남긴다.
 * <ul>
 * <li>거부된 요청의 트랜잭션이 롤백돼도 기록이 남도록 별도 트랜잭션(REQUIRES_NEW)을 쓴다.</li>
 * <li>기록 실패가 원래 응답(403 등)을 바꾸지 않도록, 커밋 실패까지 포함해 예외를 삼키고 로그만 남긴다.</li>
 * </ul>
 */
@Component
public class SecurityEventRecorder {

	private static final Logger log = LoggerFactory.getLogger(SecurityEventRecorder.class);

	private final SecurityEventRepository repository;

	private final TransactionTemplate newTransaction;

	public SecurityEventRecorder(SecurityEventRepository repository, PlatformTransactionManager transactionManager) {
		this.repository = repository;
		this.newTransaction = new TransactionTemplate(transactionManager);
		this.newTransaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
	}

	public void blocked(SecurityEventType type, Severity severity, Long userId, String reasonCode,
			String resourceType, String resourceId) {
		SecurityEvent event = new SecurityEvent(userId, type, severity, "BLOCKED", reasonCode, resourceType,
				resourceId, currentClientIp(), MDC.get(RequestIdFilter.MDC_KEY));
		try {
			this.newTransaction.executeWithoutResult((status) -> this.repository.save(event));
		}
		catch (RuntimeException ex) {
			log.error("security event 기록 실패 type={} reason={}", type, reasonCode, ex);
		}
	}

	private static String currentClientIp() {
		if (RequestContextHolder.getRequestAttributes() instanceof ServletRequestAttributes attributes) {
			HttpServletRequest request = attributes.getRequest();
			return request.getRemoteAddr();
		}
		return null;
	}

}
