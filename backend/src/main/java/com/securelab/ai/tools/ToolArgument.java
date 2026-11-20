package com.securelab.ai.tools;

import java.util.Map;
import java.util.Optional;

/**
 * 도구 인자 스키마. 모델이 만든 인자는 이 스키마로 검증한 뒤에만 쓴다 (LLM05 부적절한 출력 처리 방어).
 */
public record ToolArgument(String name, Type type, boolean required, long min, long max) {

	public enum Type {

		/** 정수. 모델이 문자열 "12"로 보내도 받지 않는다. */
		INTEGER,

		/** 문자열. {@code min}~{@code max}는 길이. */
		STRING

	}

	public static ToolArgument requiredInteger(String name, long min, long max) {
		return new ToolArgument(name, Type.INTEGER, true, min, max);
	}

	public static ToolArgument optionalInteger(String name, long min, long max) {
		return new ToolArgument(name, Type.INTEGER, false, min, max);
	}

	/**
	 * @return 오류 사유 (없으면 빈 값)
	 */
	Optional<String> check(Object value) {
		return switch (this.type) {
			case INTEGER -> {
				if (!(value instanceof Long || value instanceof Integer)) {
					yield Optional.of(this.name + ": 정수가 아님");
				}
				long number = ((Number) value).longValue();
				yield (number < this.min || number > this.max) ? Optional.of(this.name + ": 범위 밖") : Optional.empty();
			}
			case STRING -> {
				if (!(value instanceof String text)) {
					yield Optional.of(this.name + ": 문자열이 아님");
				}
				yield (text.length() < this.min || text.length() > this.max) ? Optional.of(this.name + ": 길이 범위 밖")
						: Optional.empty();
			}
		};
	}

	static long longValue(Map<String, Object> arguments, String name, long defaultValue) {
		Object value = arguments.get(name);
		return (value instanceof Number number) ? number.longValue() : defaultValue;
	}

}
