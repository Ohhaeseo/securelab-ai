package com.securelab.support;

import java.net.http.HttpHeaders;
import java.util.List;

import com.jayway.jsonpath.JsonPath;

public record ApiResponse(int status, HttpHeaders headers, String body) {

	public <T> T json(String path) {
		return JsonPath.read(this.body, path);
	}

	public String header(String name) {
		return this.headers.firstValue(name).orElse(null);
	}

	public List<String> setCookies() {
		return this.headers.allValues("Set-Cookie");
	}

	@Override
	public String toString() {
		return "HTTP " + this.status + " " + this.body;
	}

}
