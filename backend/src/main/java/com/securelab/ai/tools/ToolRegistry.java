package com.securelab.ai.tools;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

/**
 * 허용 목록. 스프링 빈으로 등록된 {@link AiTool}만 들어간다. 이름은 정확히 일치해야 한다 (대소문자 구분, 별칭 없음).
 */
@Component
public class ToolRegistry {

	private final Map<String, AiTool> tools;

	public ToolRegistry(List<AiTool> tools) {
		Set<String> names = new HashSet<>();
		for (AiTool tool : tools) {
			if (!names.add(tool.name())) {
				throw new IllegalStateException("도구 이름 중복: " + tool.name());
			}
		}
		this.tools = tools.stream().collect(Collectors.toUnmodifiableMap(AiTool::name, Function.identity()));
	}

	public Optional<AiTool> find(String name) {
		return Optional.ofNullable(this.tools.get(name));
	}

	public Set<String> names() {
		return this.tools.keySet();
	}

	public Collection<AiTool> all() {
		return this.tools.values();
	}

	/**
	 * 인자 검증: 선언되지 않은 인자 거부, 필수 인자 확인, 타입·범위 확인.
	 * @return 오류 사유 (없으면 빈 값)
	 */
	public static Optional<String> validate(AiTool tool, Map<String, Object> arguments) {
		Map<String, ToolArgument> declared = tool.arguments()
			.stream()
			.collect(Collectors.toMap(ToolArgument::name, Function.identity()));
		for (String name : arguments.keySet()) {
			if (!declared.containsKey(name)) {
				return Optional.of("선언되지 않은 인자: " + name);
			}
		}
		for (ToolArgument argument : tool.arguments()) {
			Object value = arguments.get(argument.name());
			if (value == null) {
				if (argument.required()) {
					return Optional.of(argument.name() + ": 필수");
				}
				continue;
			}
			Optional<String> error = argument.check(value);
			if (error.isPresent()) {
				return error;
			}
		}
		return Optional.empty();
	}

}
