package com.securelab.global.response;

import java.util.List;
import java.util.function.Function;

import org.springframework.data.domain.Page;

/**
 * 페이지 응답. Spring Data {@code Page}를 그대로 직렬화하지 않고 필요한 필드만 내보낸다.
 */
public record PageResponse<T>(List<T> content, int page, int size, long totalElements, int totalPages) {

	public static <E, T> PageResponse<T> from(Page<E> page, Function<E, T> mapper) {
		return new PageResponse<>(page.getContent().stream().map(mapper).toList(), page.getNumber(), page.getSize(),
				page.getTotalElements(), page.getTotalPages());
	}

}
