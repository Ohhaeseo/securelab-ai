package com.securelab;

import org.springframework.boot.SpringApplication;

public class TestSecureLabApplication {

	public static void main(String[] args) {
		SpringApplication.from(SecureLabApplication::main).with(TestcontainersConfiguration.class).run(args);
	}

}
