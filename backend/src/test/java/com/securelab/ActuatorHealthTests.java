package com.securelab;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Actuator는 health만, 세부 정보 없이 노출한다.
 */
@Import(TestcontainersConfiguration.class)
@SpringBootTest
@AutoConfigureMockMvc
class ActuatorHealthTests {

	@Autowired
	MockMvc mvc;

	@Test
	void healthIsPublicAndUpWithoutDetails() throws Exception {
		mvc.perform(get("/actuator/health"))
			.andExpect(status().isOk())
			.andExpect(jsonPath("$.status").value("UP"))
			.andExpect(jsonPath("$.components").doesNotExist());
	}

	@Test
	@WithMockUser(roles = "ADMIN")
	void otherEndpointsAreNotExposedEvenToAdmin() throws Exception {
		mvc.perform(get("/actuator/env")).andExpect(status().isNotFound());
		mvc.perform(get("/actuator/beans")).andExpect(status().isNotFound());
	}

}
