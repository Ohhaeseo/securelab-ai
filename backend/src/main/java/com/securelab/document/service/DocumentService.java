package com.securelab.document.service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import com.securelab.auth.security.AuthenticatedUser;
import com.securelab.document.dto.DocumentCreateRequest;
import com.securelab.document.dto.DocumentResponse;
import com.securelab.document.dto.DocumentSummaryResponse;
import com.securelab.document.dto.DocumentUpdateRequest;
import com.securelab.document.entity.Document;
import com.securelab.document.entity.Visibility;
import com.securelab.document.repository.DocumentRepository;
import com.securelab.global.exception.BusinessException;
import com.securelab.global.exception.ErrorCode;
import com.securelab.global.response.PageResponse;
import com.securelab.securitylab.audit.SecurityEventRecorder;
import com.securelab.securitylab.audit.SecurityEventType;
import com.securelab.securitylab.audit.Severity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentService {

	private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

	private final DocumentRepository documentRepository;

	private final DocumentAccessPolicy accessPolicy;

	private final SecurityEventRecorder securityEvents;

	public DocumentService(DocumentRepository documentRepository, DocumentAccessPolicy accessPolicy,
			SecurityEventRecorder securityEvents) {
		this.documentRepository = documentRepository;
		this.accessPolicy = accessPolicy;
		this.securityEvents = securityEvents;
	}

	/**
	 * 소유자는 항상 인증된 사용자다. 요청 본문으로 소유자를 지정할 수 없다.
	 */
	@Transactional
	public DocumentResponse create(AuthenticatedUser user, DocumentCreateRequest request) {
		Document document = Document.create(user.id(), request.title(), request.content(), request.visibility());
		return DocumentResponse.from(this.documentRepository.save(document));
	}

	@Transactional(readOnly = true)
	public DocumentResponse get(AuthenticatedUser user, Long documentId) {
		Document document = findOrThrow(documentId);
		if (!this.accessPolicy.canRead(user, document)) {
			throw denied("DOCUMENT_READ", user, documentId);
		}
		return DocumentResponse.from(document);
	}

	/**
	 * 읽을 수 있는 문서(본인 + 공개)만 DB 쿼리 단계에서 거른다. 최신순 고정 정렬 (임의 속성 정렬을 받지 않는다).
	 */
	@Transactional(readOnly = true)
	public PageResponse<DocumentSummaryResponse> list(AuthenticatedUser user, int page, int size) {
		PageRequest pageRequest = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "id"));
		return PageResponse.from(this.documentRepository.findReadableBy(user.id(), Visibility.PUBLIC, pageRequest),
				DocumentSummaryResponse::from);
	}

	@Transactional
	public DocumentResponse update(AuthenticatedUser user, Long documentId, DocumentUpdateRequest request) {
		Document document = findOrThrow(documentId);
		if (!this.accessPolicy.canModify(user, document)) {
			throw denied("DOCUMENT_UPDATE", user, documentId);
		}
		document.update(request.title(), request.content(), request.visibility());
		return DocumentResponse.from(document);
	}

	@Transactional
	public void delete(AuthenticatedUser user, Long documentId) {
		Document document = findOrThrow(documentId);
		if (!this.accessPolicy.canModify(user, document)) {
			throw denied("DOCUMENT_DELETE", user, documentId);
		}
		this.documentRepository.delete(document);
	}

	/**
	 * AI 도우미가 참고할 문서를 ID로 지정한 경우. 하나라도 읽을 권한이 없으면 403 — 모델 호출 전에 차단한다 (AI-03).
	 */
	@Transactional(readOnly = true)
	public List<DocumentResponse> getReadableForAssistant(AuthenticatedUser user, List<Long> documentIds) {
		List<DocumentResponse> documents = new ArrayList<>();
		for (Long documentId : documentIds) {
			Document document = findOrThrow(documentId);
			if (!this.accessPolicy.canRead(user, document)) {
				throw denied("AI_DOCUMENT_ACCESS", user, documentId);
			}
			documents.add(DocumentResponse.from(document));
		}
		return documents;
	}

	/**
	 * AI 도우미용 키워드 검색. 읽을 수 있는 문서(본인 + 공개) 안에서만 DB 쿼리로 찾는다 (AI-03). 응답 후 텍스트 필터에 기대지 않는다.
	 */
	@Transactional(readOnly = true)
	public List<DocumentResponse> searchReadableForAssistant(AuthenticatedUser user, List<String> keywords,
			int limit) {
		Map<Long, DocumentResponse> found = new LinkedHashMap<>();
		PageRequest page = PageRequest.of(0, limit, Sort.by(Sort.Direction.DESC, "id"));
		for (String keyword : keywords) {
			String pattern = "%" + escapeLike(keyword.toLowerCase(Locale.ROOT)) + "%";
			for (Document document : this.documentRepository.searchReadableBy(user.id(), Visibility.PUBLIC, pattern,
					page)) {
				found.putIfAbsent(document.getId(), DocumentResponse.from(document));
			}
			if (found.size() >= limit) {
				break;
			}
		}
		return found.values().stream().limit(limit).toList();
	}

	private static String escapeLike(String value) {
		return value.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
	}

	private Document findOrThrow(Long documentId) {
		return this.documentRepository.findById(documentId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
	}

	private BusinessException denied(String reasonCode, AuthenticatedUser user, Long documentId) {
		log.warn("security.denied resource=document reason={} id={} actor={}", reasonCode, documentId, user.id());
		this.securityEvents.blocked(SecurityEventType.ACCESS_DENIED, Severity.HIGH, user.id(), reasonCode,
				"DOCUMENT", String.valueOf(documentId));
		return new BusinessException(ErrorCode.ACCESS_DENIED);
	}

}
