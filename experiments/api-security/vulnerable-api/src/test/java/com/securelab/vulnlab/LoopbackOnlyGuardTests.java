package com.securelab.vulnlab;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class LoopbackOnlyGuardTests {

	@ParameterizedTest
	@ValueSource(strings = { "127.0.0.1", "::1", "127.0.0.2" })
	void loopbackAddressesAreAllowed(String address) {
		assertThatCode(() -> LoopbackOnlyGuard.verify(address)).doesNotThrowAnyException();
	}

	@ParameterizedTest
	@ValueSource(strings = { "0.0.0.0", "::", "192.168.0.10", "10.0.0.1" })
	void nonLoopbackAddressesAreRefused(String address) {
		assertThatThrownBy(() -> LoopbackOnlyGuard.verify(address)).isInstanceOf(IllegalStateException.class)
			.hasMessageContaining("루프백");
	}

	@ParameterizedTest
	@NullAndEmptySource
	void missingAddressIsRefused(String address) {
		assertThatThrownBy(() -> LoopbackOnlyGuard.verify(address)).isInstanceOf(IllegalStateException.class);
	}

}
