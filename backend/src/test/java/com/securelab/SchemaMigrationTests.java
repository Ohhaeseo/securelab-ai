package com.securelab;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

/**
 * Flyway V1 스키마가 적용되고, 기획서의 제약조건이 DB 수준에서 지켜지는지 확인한다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@Transactional
class SchemaMigrationTests {

	@Autowired
	JdbcTemplate jdbc;

	@Test
	void flywayAppliesInitialSchema() {
		Integer applied = jdbc.queryForObject(
				"select count(*) from flyway_schema_history where version = '1' and success", Integer.class);

		assertThat(applied).isEqualTo(1);
	}

	@Test
	void emailMustBeUnique() {
		insertUser("dup@securelab.test", "USER");

		assertThatThrownBy(() -> insertUser("dup@securelab.test", "USER"))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void emailMustBeStoredInLowercase() {
		assertThatThrownBy(() -> insertUser("Upper@securelab.test", "USER"))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void roleIsLimitedToUserAndAdmin() {
		assertThatThrownBy(() -> insertUser("role@securelab.test", "ROOT"))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void documentDefaultsToPrivate() {
		long ownerId = insertUser("owner@securelab.test", "USER");
		long documentId = jdbc.queryForObject(
				"insert into documents (owner_id, title, content) values (?, ?, ?) returning id", Long.class,
				ownerId, "title", "content");

		String visibility = jdbc.queryForObject("select visibility from documents where id = ?", String.class,
				documentId);

		assertThat(visibility).isEqualTo("PRIVATE");
	}

	@Test
	void documentVisibilityIsLimitedToPrivateAndPublic() {
		long ownerId = insertUser("visibility@securelab.test", "USER");

		assertThatThrownBy(() -> jdbc.update(
				"insert into documents (owner_id, title, content, visibility) values (?, ?, ?, ?)", ownerId, "title",
				"content", "SECRET"))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	@Test
	void documentRequiresExistingOwner() {
		assertThatThrownBy(() -> jdbc.update("insert into documents (owner_id, title, content) values (?, ?, ?)",
				999_999L, "title", "content"))
			.isInstanceOf(DataIntegrityViolationException.class);
	}

	private long insertUser(String email, String role) {
		return jdbc.queryForObject(
				"insert into users (email, password_hash, role) values (?, ?, ?) returning id", Long.class, email,
				"not-a-real-hash", role);
	}

}
