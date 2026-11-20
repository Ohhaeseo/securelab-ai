package com.securelab.document.repository;

import java.util.List;

import com.securelab.document.entity.Document;
import com.securelab.document.entity.Visibility;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DocumentRepository extends JpaRepository<Document, Long> {

	/**
	 * 사용자가 읽을 수 있는 문서 = 본인 문서 + 공개 문서. 인가 조건을 쿼리에 넣어 DB 단계에서 거른다.
	 */
	@Query("""
			select d from Document d
			where d.ownerId = :userId or d.visibility = :publicVisibility
			""")
	Page<Document> findReadableBy(@Param("userId") Long userId,
			@Param("publicVisibility") Visibility publicVisibility, Pageable pageable);

	/**
	 * AI 도우미용 키워드 검색. 인가 조건(본인 + 공개)과 검색 조건을 한 쿼리에 넣어, 읽을 수 없는 문서는 결과에 나올 수 없다 (AI-03).
	 * @param pattern 소문자, LIKE 특수문자를 {@code \}로 이스케이프한 {@code %키워드%}
	 */
	@Query("""
			select d from Document d
			where (d.ownerId = :userId or d.visibility = :publicVisibility)
			  and (lower(d.title) like :pattern escape '\\' or lower(d.content) like :pattern escape '\\')
			""")
	List<Document> searchReadableBy(@Param("userId") Long userId,
			@Param("publicVisibility") Visibility publicVisibility, @Param("pattern") String pattern,
			Pageable pageable);

}
