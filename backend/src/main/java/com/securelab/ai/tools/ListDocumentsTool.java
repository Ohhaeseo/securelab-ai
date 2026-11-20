package com.securelab.ai.tools;

import java.util.List;
import java.util.Map;

import com.securelab.auth.security.AuthenticatedUser;
import com.securelab.document.service.DocumentService;
import com.securelab.user.entity.Role;

import org.springframework.stereotype.Component;

/**
 * 현재 사용자가 읽을 수 있는 문서 목록 (본인 + 공개). 목록 API와 같은 인가 쿼리를 쓴다.
 */
@Component
public class ListDocumentsTool implements AiTool {

	private final DocumentService documents;

	public ListDocumentsTool(DocumentService documents) {
		this.documents = documents;
	}

	@Override
	public String name() {
		return "list_documents";
	}

	@Override
	public Role requiredRole() {
		return Role.USER;
	}

	@Override
	public List<ToolArgument> arguments() {
		return List.of(ToolArgument.optionalInteger("limit", 1, 20));
	}

	@Override
	public Object execute(AuthenticatedUser user, Map<String, Object> arguments) {
		int limit = (int) ToolArgument.longValue(arguments, "limit", 10);
		return this.documents.list(user, 0, limit)
			.content()
			.stream()
			.map((document) -> Map.of("documentId", document.id(), "title", document.title(), "visibility",
					document.visibility()))
			.toList();
	}

}
