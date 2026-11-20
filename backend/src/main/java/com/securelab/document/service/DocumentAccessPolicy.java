package com.securelab.document.service;

import com.securelab.auth.security.AuthenticatedUser;
import com.securelab.document.entity.Document;

import org.springframework.stereotype.Component;

/**
 * 문서 객체 단위 인가 규칙을 한 곳에 모은다 (BOLA 방어, SEC-01). 컨트롤러·AI 검색 등 모든 경로가 이 규칙을 거친다.
 *
 * <ul>
 * <li>읽기: 소유자 또는 공개 문서</li>
 * <li>수정·삭제: 소유자만 (관리자도 예외 없음 — 최소 권한)</li>
 * </ul>
 */
@Component
public class DocumentAccessPolicy {

	public boolean canRead(AuthenticatedUser user, Document document) {
		return document.isOwnedBy(user.id()) || document.isPublic();
	}

	public boolean canModify(AuthenticatedUser user, Document document) {
		return document.isOwnedBy(user.id());
	}

}
