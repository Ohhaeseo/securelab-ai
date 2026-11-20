package com.securelab.document.repository;

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

}
