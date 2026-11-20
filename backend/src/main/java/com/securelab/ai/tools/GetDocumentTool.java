package com.securelab.ai.tools;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.securelab.auth.security.AuthenticatedUser;
import com.securelab.document.dto.DocumentResponse;
import com.securelab.document.service.DocumentService;
import com.securelab.user.entity.Role;

import org.springframework.stereotype.Component;

/**
 * 문서 1건 조회. {@link DocumentService#get}을 현재 사용자로 부르므로 남의 비공개 문서는 거부된다.
 */
@Component
public class GetDocumentTool implements AiTool {

	private static final int MAX_CONTENT_CHARS = 2000;

	private final DocumentService documents;

	public GetDocumentTool(DocumentService documents) {
		this.documents = documents;
	}

	@Override
	public String name() {
		return "get_document";
	}

	@Override
	public Role requiredRole() {
		return Role.USER;
	}

	@Override
	public List<ToolArgument> arguments() {
		return List.of(ToolArgument.requiredInteger("documentId", 1, Long.MAX_VALUE));
	}

	@Override
	public Object execute(AuthenticatedUser user, Map<String, Object> arguments) {
		DocumentResponse document = this.documents.get(user, ToolArgument.longValue(arguments, "documentId", 0));
		Map<String, Object> output = new LinkedHashMap<>();
		output.put("documentId", document.id());
		output.put("title", document.title());
		output.put("content", (document.content().length() <= MAX_CONTENT_CHARS) ? document.content()
				: document.content().substring(0, MAX_CONTENT_CHARS));
		return output;
	}

}
