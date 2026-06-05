/********************************************************************************
 * Copyright (c) 2026 ARENA2036 e.V.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.sde.configuration;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.web.ServerProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Integration-style regression tests for the configured Spring Security filter chain.
 *
 * <p>The tests start a minimal Spring context with the real security configuration and synthetic
 * endpoints. They verify that public routes remain reachable, protected routes reject missing or
 * insufficient authentication, and authorized JWT users can access guarded resources. This protects
 * endpoint security behavior that pure controller tests cannot exercise.</p>
 */
@SpringBootTest(classes = {
		SecurityConfig.class,
		ServerProperties.class,
		SecurityFilterChainRegressionTest.TestEndpoints.class,
		SecurityFilterChainRegressionTest.JwtDecoderConfiguration.class
}, properties = "keycloak.clientid=sde-client")
@AutoConfigureMockMvc
class SecurityFilterChainRegressionTest {

	@Autowired
	private MockMvc mockMvc;

	@Test
	void publicPingEndpointDoesNotRequireAuthentication() throws Exception {
		mockMvc.perform(get("/ping"))
				.andExpect(status().isOk())
				.andExpect(header().string("X-XSS-Protection", "1; mode=block"));
	}

	@Test
	void protectedEndpointRejectsMissingAuthentication() throws Exception {
		mockMvc.perform(get("/test-security/protected"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	void protectedEndpointRejectsAuthenticatedUserWithoutRequiredAuthority() throws Exception {
		mockMvc.perform(get("/test-security/protected")
				.with(jwt().authorities(new SimpleGrantedAuthority("User"))))
				.andExpect(status().isForbidden());
	}

	@Test
	void protectedEndpointAllowsAuthenticatedUserWithRequiredAuthority() throws Exception {
		mockMvc.perform(get("/test-security/protected")
				.with(jwt().authorities(new SimpleGrantedAuthority("Allowed"))))
				.andExpect(status().isOk());
	}

	@RestController
	static class TestEndpoints {

		@GetMapping("/ping")
		String ping() {
			return "ok";
		}

		@GetMapping("/test-security/protected")
		@PreAuthorize("hasAuthority('Allowed')")
		String protectedEndpoint() {
			return "ok";
		}
	}

	@TestConfiguration
	static class JwtDecoderConfiguration {

		@Bean
		JwtDecoder jwtDecoder() {
			return token -> Jwt.withTokenValue(token)
					.header("alg", "none")
					.claim("sub", "test-user")
					.build();
		}
	}
}
