package com.securelab.vulnlab;

import java.net.InetAddress;
import java.net.UnknownHostException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;

/**
 * {@code server.address}가 루프백 주소가 아니면 기동을 막는다. 빈 생성 단계에서 실패하므로 Tomcat이 포트를 열기 전에 멈춘다.
 */
@Component
public class LoopbackOnlyGuard {

	private static final Logger log = LoggerFactory.getLogger(LoopbackOnlyGuard.class);

	public LoopbackOnlyGuard(Environment environment) {
		verify(environment.getProperty("server.address"));
		log.warn("⚠️ INTENTIONALLY VULNERABLE LAB TARGET — loopback only ({}). 배포 금지.",
				environment.getProperty("server.address"));
	}

	static void verify(String address) {
		if (address == null || address.isBlank()) {
			throw new IllegalStateException(
					"vulnerable-api는 server.address를 루프백 주소(127.0.0.1)로 지정해야 실행됩니다. 지정하지 않으면 모든 인터페이스에 열립니다.");
		}
		try {
			if (!InetAddress.getByName(address).isLoopbackAddress()) {
				throw new IllegalStateException(
						"vulnerable-api는 루프백 주소에서만 실행할 수 있습니다. server.address=" + address);
			}
		}
		catch (UnknownHostException ex) {
			throw new IllegalStateException("server.address를 해석할 수 없습니다: " + address, ex);
		}
	}

}
