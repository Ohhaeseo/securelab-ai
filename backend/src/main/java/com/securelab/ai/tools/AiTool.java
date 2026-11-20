package com.securelab.ai.tools;

import java.util.List;
import java.util.Map;

import com.securelab.auth.security.AuthenticatedUser;
import com.securelab.user.entity.Role;

/**
 * AI가 제안할 수 있는 서버 도구. 등록된 도구만 허용 목록이 된다 (AI-04).
 *
 * <p>
 * 규칙 (ADR-0005):
 * <ul>
 * <li><b>읽기 전용만</b> 둔다. 상태를 바꾸는 도구는 문서 속 지시가 피해자 권한으로 공개 범위를 바꾸는 식으로 악용될 수 있다.</li>
 * <li>{@link #execute}는 <b>현재 사용자 권한으로</b> 도메인 서비스를 부른다 — 객체 단위 인가가 그대로 적용된다.</li>
 * </ul>
 */
public interface AiTool {

	String name();

	/** {@link Role#ADMIN}이면 관리자만 실행할 수 있다 (AI-02). */
	Role requiredRole();

	List<ToolArgument> arguments();

	/**
	 * @param arguments 스키마 검증을 통과한 인자
	 * @return 사용자에게 돌려줄 결과 (JSON 직렬화 가능)
	 */
	Object execute(AuthenticatedUser user, Map<String, Object> arguments);

}
