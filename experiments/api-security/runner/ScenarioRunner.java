import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.InetAddress;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * SEC-01/SEC-02 시나리오 러너. 같은 공격을 여러 대상(보호 버전, 취약 대조군)에 보내 공격 성공률·차단률·오탐률·지연을 잰다.
 *
 * <p>
 * JDK만 쓰는 단일 파일 프로그램이다. 대상은 루프백 주소만 허용한다.
 *
 * <pre>
 * java experiments/api-security/runner/ScenarioRunner.java ^
 *   --target protected=http://127.0.0.1:8080 --target vulnerable=http://127.0.0.1:18080 ^
 *   --trials 30 --warmup 3 --out experiments/api-security/results
 * </pre>
 */
public class ScenarioRunner {

	static final String PASSWORD = "correct-horse-battery";

	static final Pattern FIRST_ID = Pattern.compile("\"id\"\\s*:\\s*(\\d+)");

	public static void main(String[] args) throws Exception {
		Map<String, URI> targets = new LinkedHashMap<>();
		int trials = 30;
		int warmup = 3;
		Path out = Path.of("experiments/api-security/results");
		for (int i = 0; i < args.length; i++) {
			switch (args[i]) {
				case "--target" -> {
					String[] pair = args[++i].split("=", 2);
					targets.put(pair[0], requireLoopback(URI.create(pair[1])));
				}
				case "--trials" -> trials = Integer.parseInt(args[++i]);
				case "--warmup" -> warmup = Integer.parseInt(args[++i]);
				case "--out" -> out = Path.of(args[++i]);
				default -> throw new IllegalArgumentException("알 수 없는 인자: " + args[i]);
			}
		}
		if (targets.isEmpty()) {
			throw new IllegalArgumentException("--target name=url 을 하나 이상 지정하세요.");
		}

		List<Measurement> measurements = new ArrayList<>();
		for (Map.Entry<String, URI> target : targets.entrySet()) {
			System.out.printf("[%s] %s — 워밍업 %d회, 측정 %d회%n", target.getKey(), target.getValue(), warmup, trials);
			for (int trial = 1; trial <= warmup + trials; trial++) {
				List<Measurement> result = runTrial(target.getKey(), target.getValue(), trial - warmup);
				if (trial > warmup) {
					measurements.addAll(result);
				}
			}
		}

		String stamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss"));
		Files.createDirectories(out.resolve("raw"));
		Path raw = out.resolve("raw").resolve("sec-api-" + stamp + ".jsonl");
		Files.write(raw, measurements.stream().map(Measurement::toJson).toList(), StandardCharsets.UTF_8);
		Path summary = out.resolve("sec-api-" + stamp + ".md");
		Files.writeString(summary, summarize(targets, trials, warmup, measurements, stamp), StandardCharsets.UTF_8);
		System.out.println("요약: " + summary);
		System.out.println("원본: " + raw);
	}

	/**
	 * 한 번의 시도: 새 사용자 alice(피해자)·bob(공격자)을 만들고 공격 요청과 정상 요청을 보낸다.
	 */
	static List<Measurement> runTrial(String target, URI base, int trial) {
		Session alice = new Session(base);
		Session bob = new Session(base);
		alice.signupAndLogin("runner-alice-" + UUID.randomUUID() + "@securelab.test");
		bob.signupAndLogin("runner-bob-" + UUID.randomUUID() + "@securelab.test");

		String marker = "secret-" + UUID.randomUUID();
		long privateId = alice.createDocument("victim private", marker, "PRIVATE");
		long publicId = alice.createDocument("victim public", "notice", "PUBLIC");
		List<Measurement> results = new ArrayList<>();

		// 정상 요청 (대조): 막히면 오탐
		Timed ownRead = alice.timed("GET", "/api/v1/documents/" + privateId, null);
		results.add(Measurement.legit(target, trial, "LEGIT-OWNER-READ", ownRead, ownRead.status() == 200));
		Timed publicRead = bob.timed("GET", "/api/v1/documents/" + publicId, null);
		results.add(Measurement.legit(target, trial, "LEGIT-PUBLIC-READ", publicRead, publicRead.status() == 200));

		// SEC-01 읽기: 본문(marker)이 공격자에게 노출되면 침해
		Timed read = bob.timed("GET", "/api/v1/documents/" + privateId, null);
		results.add(Measurement.attack(target, trial, "SEC-01-READ", read,
				read.status() == 200 && read.body().contains(marker)));

		// SEC-01 목록: 목록에 피해자 비공개 문서 ID가 보이면 침해
		Timed list = bob.timed("GET", "/api/v1/documents?size=100", null);
		results.add(Measurement.attack(target, trial, "SEC-01-LIST", list,
				list.status() == 200 && Pattern.compile("\"id\"\\s*:\\s*" + privateId + "\\b").matcher(list.body()).find()));

		// SEC-01 수정: 피해자가 다시 읽었을 때 제목이 바뀌었으면 침해
		String tampered = "tampered-" + UUID.randomUUID();
		Timed update = bob.timed("PATCH", "/api/v1/documents/" + privateId, "{\"title\":\"" + tampered + "\"}");
		boolean updated = alice.timed("GET", "/api/v1/documents/" + privateId, null).body().contains(tampered);
		results.add(Measurement.attack(target, trial, "SEC-01-UPDATE", update, updated));

		// SEC-02: 일반 사용자가 관리자 API 응답을 받으면 침해
		Timed admin = bob.timed("GET", "/api/v1/security/events", null);
		results.add(Measurement.attack(target, trial, "SEC-02-ADMIN-API", admin, admin.status() == 200));

		// SEC-01 삭제 (마지막): 피해자 문서가 사라지면 침해
		Timed delete = bob.timed("DELETE", "/api/v1/documents/" + privateId, null);
		boolean deleted = alice.timed("GET", "/api/v1/documents/" + privateId, null).status() == 404;
		results.add(Measurement.attack(target, trial, "SEC-01-DELETE", delete, deleted));
		return results;
	}

	static URI requireLoopback(URI uri) {
		try {
			if (uri.getHost() == null || !InetAddress.getByName(uri.getHost()).isLoopbackAddress()) {
				throw new IllegalArgumentException("러너는 루프백 대상만 허용합니다: " + uri);
			}
		}
		catch (IOException ex) {
			throw new IllegalArgumentException("대상 주소를 해석할 수 없습니다: " + uri, ex);
		}
		return uri;
	}

	static String summarize(Map<String, URI> targets, int trials, int warmup, List<Measurement> all, String stamp) {
		StringBuilder md = new StringBuilder();
		md.append("# SEC-01/02 실측 결과 (").append(stamp).append(")\n\n");
		md.append("- 시도: 대상별 ").append(trials).append("회 (워밍업 ").append(warmup).append("회 제외), 시도마다 새 사용자 2명\n");
		md.append("- 대상: ");
		targets.forEach((name, uri) -> md.append('`').append(name).append("` ").append(uri).append("  "));
		md.append("\n- 런타임: Java ").append(System.getProperty("java.version")).append(", ")
			.append(System.getProperty("os.name")).append(", CPU 코어 ")
			.append(Runtime.getRuntime().availableProcessors()).append("\n");
		md.append("- 판정: 공격은 **실제로 보안 목표가 침해됐는지**(본문 노출, 목록 노출, 변경 반영, 삭제 반영, 관리자 응답 수신)로 센다. 응답 코드만 보지 않는다.\n\n");

		md.append("## 공격 (ASR = 침해 수 ÷ 시도 수)\n\n");
		md.append("| 대상 | 시나리오 | 시도 | 침해 | ASR | 차단률 | 응답 코드 | p50 ms | p95 ms |\n");
		md.append("| --- | --- | ---: | ---: | ---: | ---: | --- | ---: | ---: |\n");
		appendRows(md, all, true);

		md.append("\n## 정상 요청 (오탐률 = 막힌 정상 요청 ÷ 정상 요청)\n\n");
		md.append("| 대상 | 요청 | 시도 | 막힘(오탐) | 오탐률 | 응답 코드 | p50 ms | p95 ms |\n");
		md.append("| --- | --- | ---: | ---: | ---: | --- | ---: | ---: |\n");
		appendRows(md, all, false);
		md.append("\n> 지연은 같은 PC에서 잰 참고 값이다. 두 대상은 저장소가 다르다(PostgreSQL vs 메모리) — ADR-0002.\n");
		return md.toString();
	}

	static void appendRows(StringBuilder md, List<Measurement> all, boolean attacks) {
		Map<String, List<Measurement>> groups = new LinkedHashMap<>();
		for (Measurement m : all) {
			if (m.attack() == attacks) {
				groups.computeIfAbsent(m.target() + "|" + m.scenario(), (k) -> new ArrayList<>()).add(m);
			}
		}
		groups.forEach((key, rows) -> {
			String[] parts = key.split("\\|");
			int n = rows.size();
			long hits = rows.stream().filter(attacks ? Measurement::breached : (m) -> !m.allowed()).count();
			Map<Integer, Integer> statuses = new java.util.TreeMap<>();
			rows.forEach((m) -> statuses.merge(m.status(), 1, Integer::sum));
			List<Double> latencies = rows.stream().map(Measurement::latencyMs).sorted().toList();
			String rate = String.format(Locale.ROOT, "%.1f%%", 100.0 * hits / n);
			if (attacks) {
				md.append(String.format(Locale.ROOT, "| %s | %s | %d | %d | %s | %.1f%% | %s | %.1f | %.1f |%n", parts[0],
						parts[1], n, hits, rate, 100.0 * (n - hits) / n, statuses, percentile(latencies, 50),
						percentile(latencies, 95)));
			}
			else {
				md.append(String.format(Locale.ROOT, "| %s | %s | %d | %d | %s | %s | %.1f | %.1f |%n", parts[0], parts[1],
						n, hits, rate, statuses, percentile(latencies, 50), percentile(latencies, 95)));
			}
		});
	}

	/** nearest-rank 백분위수. */
	static double percentile(List<Double> sorted, int p) {
		if (sorted.isEmpty()) {
			return Double.NaN;
		}
		int rank = (int) Math.ceil(p / 100.0 * sorted.size());
		return sorted.get(Math.max(0, rank - 1));
	}

	record Timed(int status, String body, double latencyMs) {
	}

	record Measurement(String target, int trial, String scenario, boolean attack, boolean breached, boolean allowed,
			int status, double latencyMs) {

		static Measurement attack(String target, int trial, String scenario, Timed t, boolean breached) {
			return new Measurement(target, trial, scenario, true, breached, false, t.status(), t.latencyMs());
		}

		static Measurement legit(String target, int trial, String scenario, Timed t, boolean allowed) {
			return new Measurement(target, trial, scenario, false, false, allowed, t.status(), t.latencyMs());
		}

		String toJson() {
			return String.format(Locale.ROOT,
					"{\"target\":\"%s\",\"trial\":%d,\"scenario\":\"%s\",\"attack\":%b,\"breached\":%b,\"allowed\":%b,\"status\":%d,\"latencyMs\":%.3f}",
					this.target, this.trial, this.scenario, this.attack, this.breached, this.allowed, this.status,
					this.latencyMs);
		}

	}

	/**
	 * 사용자 한 명의 브라우저: 쿠키(세션, *XSRF-TOKEN)를 따로 들고, 상태 변경 요청에 CSRF 헤더를 붙인다.
	 */
	static final class Session {

		private final URI base;

		private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);

		private final HttpClient http;

		Session(URI base) {
			this.base = base;
			this.http = HttpClient.newBuilder()
				.cookieHandler(this.cookies)
				.connectTimeout(Duration.ofSeconds(5))
				.build();
		}

		void signupAndLogin(String email) {
			String body = "{\"email\":\"" + email + "\",\"password\":\"" + PASSWORD + "\"}";
			expect(timed("POST", "/api/v1/auth/signup", body), 201, "가입");
			expect(timed("POST", "/api/v1/auth/login", body), 200, "로그인");
		}

		long createDocument(String title, String content, String visibility) {
			Timed created = timed("POST", "/api/v1/documents",
					"{\"title\":\"" + title + "\",\"content\":\"" + content + "\",\"visibility\":\"" + visibility + "\"}");
			expect(created, 201, "문서 작성");
			Matcher id = FIRST_ID.matcher(created.body());
			if (!id.find()) {
				throw new IllegalStateException("문서 ID를 찾을 수 없습니다: " + created.body());
			}
			return Long.parseLong(id.group(1));
		}

		Timed timed(String method, String path, String json) {
			boolean stateChanging = !method.equals("GET");
			if (stateChanging && csrfToken() == null) {
				send("GET", "/api/v1/auth/csrf", null, false);
			}
			return send(method, path, json, stateChanging);
		}

		private Timed send(String method, String path, String json, boolean withCsrf) {
			HttpRequest.Builder request = HttpRequest.newBuilder(this.base.resolve(path))
				.timeout(Duration.ofSeconds(10))
				.method(method, (json != null) ? HttpRequest.BodyPublishers.ofString(json)
						: HttpRequest.BodyPublishers.noBody());
			if (json != null) {
				request.header("Content-Type", "application/json");
			}
			if (withCsrf) {
				request.header("X-XSRF-TOKEN", csrfToken());
			}
			try {
				long start = System.nanoTime();
				HttpResponse<String> response = this.http.send(request.build(), HttpResponse.BodyHandlers.ofString());
				double ms = (System.nanoTime() - start) / 1_000_000.0;
				return new Timed(response.statusCode(), response.body(), ms);
			}
			catch (IOException ex) {
				throw new IllegalStateException(method + " " + this.base.resolve(path) + " 실패", ex);
			}
			catch (InterruptedException ex) {
				Thread.currentThread().interrupt();
				throw new IllegalStateException(ex);
			}
		}

		private String csrfToken() {
			return this.cookies.getCookieStore()
				.get(this.base)
				.stream()
				.filter((cookie) -> cookie.getName().endsWith("XSRF-TOKEN") && !cookie.getValue().isEmpty())
				.map(HttpCookie::getValue)
				.findFirst()
				.orElse(null);
		}

		private static void expect(Timed response, int status, String step) {
			if (response.status() != status) {
				throw new IllegalStateException(step + " 실패: HTTP " + response.status() + " " + response.body());
			}
		}

	}

}
