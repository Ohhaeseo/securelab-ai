package com.securelab.vulnlab;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.stereotype.Component;

/**
 * 메모리 저장소. 재시작하면 비워지고, 본 서비스 DB와 섞이지 않는다.
 */
@Component
public class LabStore {

	private static final int MAX_EVENTS = 500;

	private final AtomicLong userIds = new AtomicLong();

	private final AtomicLong documentIds = new AtomicLong();

	private final AtomicLong eventIds = new AtomicLong();

	private final Map<String, UserRecord> usersByEmail = new ConcurrentHashMap<>();

	private final Map<Long, DocumentRecord> documents = new ConcurrentHashMap<>();

	private final List<EventRecord> events = new CopyOnWriteArrayList<>();

	public static String normalizeEmail(String email) {
		return (email != null) ? email.strip().toLowerCase(Locale.ROOT) : null;
	}

	/**
	 * @return 새 사용자, 이미 있으면 빈 값
	 */
	public Optional<UserRecord> createUser(String email, String passwordHash) {
		String normalized = normalizeEmail(email);
		UserRecord user = new UserRecord(this.userIds.incrementAndGet(), normalized, passwordHash, "USER",
				Instant.now());
		return (this.usersByEmail.putIfAbsent(normalized, user) == null) ? Optional.of(user) : Optional.empty();
	}

	public Optional<UserRecord> findUserByEmail(String email) {
		return Optional.ofNullable(this.usersByEmail.get(normalizeEmail(email)));
	}

	public DocumentRecord createDocument(long ownerId, String title, String content, String visibility) {
		Instant now = Instant.now();
		DocumentRecord document = new DocumentRecord(this.documentIds.incrementAndGet(), ownerId, title, content,
				(visibility != null) ? visibility : "PRIVATE", now, now);
		this.documents.put(document.id(), document);
		return document;
	}

	public Optional<DocumentRecord> findDocument(long id) {
		return Optional.ofNullable(this.documents.get(id));
	}

	public List<DocumentRecord> allDocumentsNewestFirst() {
		List<DocumentRecord> all = new ArrayList<>(this.documents.values());
		all.sort(Comparator.comparingLong(DocumentRecord::id).reversed());
		return all;
	}

	public void saveDocument(DocumentRecord document) {
		this.documents.put(document.id(), document);
	}

	public boolean deleteDocument(long id) {
		return this.documents.remove(id) != null;
	}

	public void recordLogin(String email, String ipAddress) {
		this.events.add(new EventRecord(this.eventIds.incrementAndGet(), "LOGIN_SUCCESS", email, ipAddress,
				Instant.now()));
		while (this.events.size() > MAX_EVENTS) {
			this.events.remove(0);
		}
	}

	public List<EventRecord> eventsNewestFirst() {
		List<EventRecord> copy = new ArrayList<>(this.events);
		copy.sort(Comparator.comparingLong(EventRecord::id).reversed());
		return copy;
	}

	public record UserRecord(long id, String email, String passwordHash, String role, Instant createdAt) {

		@Override
		public String toString() {
			return "UserRecord{id=" + this.id + ", role=" + this.role + "}";
		}

	}

	public record DocumentRecord(long id, long ownerId, String title, String content, String visibility,
			Instant createdAt, Instant updatedAt) {

		DocumentRecord update(String newTitle, String newContent, String newVisibility) {
			return new DocumentRecord(this.id, this.ownerId, (newTitle != null) ? newTitle : this.title,
					(newContent != null) ? newContent : this.content,
					(newVisibility != null) ? newVisibility : this.visibility, this.createdAt, Instant.now());
		}

	}

	public record EventRecord(long id, String eventType, String email, String ipAddress, Instant occurredAt) {
	}

}
