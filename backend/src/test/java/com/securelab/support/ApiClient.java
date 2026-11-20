package com.securelab.support;

import java.io.IOException;
import java.net.CookieManager;
import java.net.CookiePolicy;
import java.net.HttpCookie;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * 실제 HTTP로 API를 호출하는 테스트 클라이언트. 브라우저처럼 쿠키(세션, XSRF-TOKEN)를 저장하고, 상태 변경 요청에는 XSRF-TOKEN 쿠키 값을
 * {@code X-XSRF-TOKEN} 헤더로 붙인다.
 */
public final class ApiClient {

	private static final Set<String> STATE_CHANGING = Set.of("POST", "PUT", "PATCH", "DELETE");

	private final URI baseUri;

	private final CookieManager cookies = new CookieManager(null, CookiePolicy.ACCEPT_ALL);

	private final HttpClient http;

	public ApiClient(int port) {
		this.baseUri = URI.create("http://127.0.0.1:" + port);
		this.http = HttpClient.newBuilder()
			.cookieHandler(this.cookies)
			.followRedirects(HttpClient.Redirect.NEVER)
			.build();
	}

	public ApiResponse get(String path) {
		return send("GET", path, null, true);
	}

	public ApiResponse post(String path, String json) {
		return send("POST", path, json, true);
	}

	public ApiResponse patch(String path, String json) {
		return send("PATCH", path, json, true);
	}

	public ApiResponse delete(String path) {
		return send("DELETE", path, null, true);
	}

	/**
	 * CSRF 헤더 없이 보낸다 (CSRF 방어 확인용).
	 */
	public ApiResponse postWithoutCsrf(String path, String json) {
		return send("POST", path, json, false);
	}

	public ApiResponse getWithHeader(String path, String headerName, String headerValue) {
		return send("GET", path, null, false, Map.of(headerName, headerValue));
	}

	/**
	 * 지정한 CSRF 토큰으로 보낸다 (재발급 전 토큰이 거부되는지 확인용).
	 */
	public ApiResponse postWithCsrfToken(String path, String json, String csrfToken) {
		return send("POST", path, json, false, Map.of("X-XSRF-TOKEN", csrfToken));
	}

	/**
	 * 쿠키를 직접 심는다 (탈취·고정된 세션 ID 재사용 시나리오용).
	 */
	public void setCookie(String name, String value) {
		HttpCookie cookie = new HttpCookie(name, value);
		cookie.setPath("/");
		cookie.setVersion(0);
		this.cookies.getCookieStore().add(this.baseUri, cookie);
	}

	public ApiResponse login(String email, String password) {
		return post("/api/v1/auth/login", "{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
	}

	public ApiResponse signupAndLogin(String email, String password) {
		ApiResponse signup = post("/api/v1/auth/signup",
				"{\"email\":\"" + email + "\",\"password\":\"" + password + "\"}");
		if (signup.status() != 201) {
			throw new IllegalStateException("가입 실패: " + signup);
		}
		ApiResponse login = login(email, password);
		if (login.status() != 200) {
			throw new IllegalStateException("로그인 실패: " + login);
		}
		return login;
	}

	public ApiResponse send(String method, String path, String json, boolean withCsrf) {
		return send(method, path, json, withCsrf, Map.of());
	}

	private ApiResponse send(String method, String path, String json, boolean withCsrf, Map<String, String> headers) {
		HttpRequest.Builder request = HttpRequest.newBuilder(this.baseUri.resolve(path))
			.method(method, (json != null) ? HttpRequest.BodyPublishers.ofString(json)
					: HttpRequest.BodyPublishers.noBody());
		headers.forEach(request::header);
		if (json != null) {
			request.header("Content-Type", "application/json");
		}
		if (withCsrf && STATE_CHANGING.contains(method)) {
			request.header("X-XSRF-TOKEN", csrfToken());
		}
		try {
			HttpResponse<String> response = this.http.send(request.build(), HttpResponse.BodyHandlers.ofString());
			return new ApiResponse(response.statusCode(), response.headers(), response.body());
		}
		catch (IOException ex) {
			throw new IllegalStateException("HTTP 요청 실패: " + method + " " + path, ex);
		}
		catch (InterruptedException ex) {
			Thread.currentThread().interrupt();
			throw new IllegalStateException(ex);
		}
	}

	public Optional<String> cookie(String name) {
		return this.cookies.getCookieStore()
			.get(this.baseUri)
			.stream()
			.filter((cookie) -> cookie.getName().equals(name))
			.map(HttpCookie::getValue)
			.filter((value) -> !value.isEmpty())
			.findFirst();
	}

	private String csrfToken() {
		return cookie("XSRF-TOKEN").orElseGet(() -> {
			get("/api/v1/auth/csrf");
			return cookie("XSRF-TOKEN").orElseThrow(() -> new IllegalStateException("XSRF-TOKEN 쿠키를 받지 못했습니다."));
		});
	}

}
