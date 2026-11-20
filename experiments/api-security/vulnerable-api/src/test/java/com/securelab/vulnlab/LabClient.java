package com.securelab.vulnlab;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

import com.jayway.jsonpath.JsonPath;

/**
 * 실제 HTTP + 쿠키 저장소 테스트 클라이언트 (사용자 1명 = 클라이언트 1개).
 */
final class LabClient {

	private final URI baseUri;

	private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);

	private final HttpClient http;

	LabClient(int port) {
		this.baseUri = URI.create("http://127.0.0.1:" + port);
		this.http = HttpClient.newBuilder().cookieHandler(this.cookies).build();
	}

	Response send(String method, String path, String json, boolean withCsrf) {
		if (withCsrf && csrfToken() == null) {
			send("GET", "/api/v1/auth/csrf", null, false);
		}
		HttpRequest.Builder request = HttpRequest.newBuilder(this.baseUri.resolve(path))
			.method(method,
					(json != null) ? HttpRequest.BodyPublishers.ofString(json) : HttpRequest.BodyPublishers.noBody());
		if (json != null) {
			request.header("Content-Type", "application/json");
		}
		if (withCsrf) {
			request.header("X-XSRF-TOKEN", csrfToken());
		}
		try {
			HttpResponse<String> response = this.http.send(request.build(), HttpResponse.BodyHandlers.ofString());
			return new Response(response.statusCode(), response.body());
		}
		catch (IOException ex) {
			throw new IllegalStateException(ex);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(ex);
		}
	}

	Response get(String path) {
		return send("GET", path, null, false);
	}

	Response post(String path, String json) {
		return send("POST", path, json, true);
	}

	void signupAndLogin(String email) {
		String credentials = "{\"email\":\"" + email + "\",\"password\":\"correct-horse-battery\"}";
		if (post("/api/v1/auth/signup", credentials).status() != 201
				|| post("/api/v1/auth/login", credentials).status() != 200) {
			throw new IllegalStateException("가입·로그인 실패: " + email);
		}
	}

	private String csrfToken() {
		return this.cookies.getCookieStore()
			.get(this.baseUri)
			.stream()
			.filter((cookie) -> cookie.getName().equals(LabSecurityConfig.CSRF_COOKIE) && !cookie.getValue().isEmpty())
			.map(HttpCookie::getValue)
			.findFirst()
			.orElse(null);
	}

	record Response(int status, String body) {

		<T> T json(String path) {
			return JsonPath.read(this.body, path);
		}

	}

}
