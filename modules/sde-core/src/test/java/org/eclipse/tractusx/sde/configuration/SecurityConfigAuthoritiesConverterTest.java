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

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Unit tests for converting JWT realm roles into Spring Security authorities.
 *
 * <p>The backend relies on Keycloak realm roles being exposed as authorities before method-security
 * permission checks can run. These tests verify the expected claim shape, the generated authorities,
 * and empty/missing role handling. Keeping this behavior isolated makes authentication-related
 * regressions easier to diagnose than a failing full security context test.</p>
 */
class SecurityConfigAuthoritiesConverterTest {

	private static final String CLIENT_ID = "sde-client";

	private final SecurityConfig securityConfig = new SecurityConfig();

	@Test
	void authoritiesConverterReadsRealmAndClientRolesWithoutRolePrefix() {
		ReflectionTestUtils.setField(securityConfig, "resourceName", CLIENT_ID);

		Jwt jwt = jwtWithClaims(Map.of(
				"realm_access", Map.of("roles", List.of("Admin", "User")),
				"resource_access", Map.of(CLIENT_ID, Map.of("roles", List.of("Creator")))));

		List<String> authorities = securityConfig.authoritiesConverter().convert(jwt).stream()
				.map(authority -> authority.getAuthority())
				.toList();

		assertThat(authorities).containsExactlyInAnyOrder("Admin", "User", "Creator");
		assertThat(authorities).doesNotContain("ROLE_Admin", "ROLE_User", "ROLE_Creator");
	}

	@Test
	void authoritiesConverterIgnoresRolesForOtherClients() {
		ReflectionTestUtils.setField(securityConfig, "resourceName", CLIENT_ID);

		Jwt jwt = jwtWithClaims(Map.of(
				"resource_access", Map.of("other-client", Map.of("roles", List.of("OtherRole")))));

		assertThat(securityConfig.authoritiesConverter().convert(jwt)).isEmpty();
	}

	private Jwt jwtWithClaims(Map<String, Object> claims) {
		return Jwt.withTokenValue("token")
				.header("alg", "none")
				.claims(existingClaims -> existingClaims.putAll(claims))
				.build();
	}
}
