package com.securelab.document.service;

import com.securelab.auth.security.AuthenticatedUser;
import com.securelab.document.dto.DocumentCreateRequest;
import com.securelab.document.dto.DocumentResponse;
import com.securelab.document.entity.Document;
import com.securelab.document.repository.DocumentRepository;
import com.securelab.global.exception.BusinessException;
import com.securelab.global.exception.ErrorCode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DocumentService {

	private static final Logger log = LoggerFactory.getLogger(DocumentService.class);

	private final DocumentRepository documentRepository;

	private final DocumentAccessPolicy accessPolicy;

	public DocumentService(DocumentRepository documentRepository, DocumentAccessPolicy accessPolicy) {
		this.documentRepository = documentRepository;
		this.accessPolicy = accessPolicy;
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
			throw denied("read", user, documentId);
		}
		return DocumentResponse.from(document);
	}

	private Document findOrThrow(Long documentId) {
		return this.documentRepository.findById(documentId)
			.orElseThrow(() -> new BusinessException(ErrorCode.RESOURCE_NOT_FOUND));
	}

	private static BusinessException denied(String action, AuthenticatedUser user, Long documentId) {
		log.warn("security.denied resource=document action={} id={} actor={}", action, documentId, user.id());
		return new BusinessException(ErrorCode.ACCESS_DENIED);
	}

}
