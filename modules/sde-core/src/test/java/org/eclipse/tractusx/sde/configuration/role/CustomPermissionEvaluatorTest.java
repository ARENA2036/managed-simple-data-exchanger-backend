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

package org.eclipse.tractusx.sde.configuration.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.eclipse.tractusx.sde.core.role.entity.RolePermissionEntity;
import org.eclipse.tractusx.sde.core.service.RoleManagementService;
import org.junit.jupiter.api.Test;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;

/**
 * Unit tests for the custom permission evaluator used by {@code hasPermission(...)} expressions.
 *
 * <p>The evaluator bridges authenticated roles to persisted application permissions through
 * {@code RoleManagementService}. These tests cover positive permission checks, denied access,
 * anonymous or unsupported authentication, and empty permission sets. The goal is to keep the central
 * authorization decision point stable without requiring a web request or Keycloak token.</p>
 */
class CustomPermissionEvaluatorTest {

	@Test
	void hasPermissionAllowsWhenAnyRequestedPermissionMatchesCurrentRole() {
		RoleManagementServiceStub roleManagementService = new RoleManagementServiceStub(
				List.of(RolePermissionEntity.builder()
						.sdeRole("Creator")
						.sdePermission("provider_create_contract_offer")
						.build()));
		CustomPermissionEvaluator evaluator = new CustomPermissionEvaluator(roleManagementService);
		TestingAuthenticationToken authentication = new TestingAuthenticationToken("user", "n/a", "Creator");

		boolean allowed = evaluator.hasPermission(authentication, "serialpart",
				"provider_create_contract_offer@provider_update_contract_offer");

		assertThat(allowed).isTrue();
		assertThat(roleManagementService.roles()).containsExactly("Creator");
		assertThat(roleManagementService.permissions())
				.containsExactly("provider_create_contract_offer", "provider_update_contract_offer");
	}

	@Test
	void hasPermissionDeniesWhenRoleManagementReturnsNoPermissionMappings() {
		RoleManagementServiceStub roleManagementService = new RoleManagementServiceStub(List.of());
		CustomPermissionEvaluator evaluator = new CustomPermissionEvaluator(roleManagementService);
		TestingAuthenticationToken authentication = new TestingAuthenticationToken("user", "n/a", "User");

		assertThat(evaluator.hasPermission(authentication, "", "provider_delete_contract_offer")).isFalse();
		assertThat(roleManagementService.roles()).containsExactly("User");
		assertThat(roleManagementService.permissions()).containsExactly("provider_delete_contract_offer");
	}

	@Test
	void hasPermissionRejectsAuthenticationWithoutAuthorities() {
		RoleManagementServiceStub roleManagementService = new RoleManagementServiceStub(List.of());
		CustomPermissionEvaluator evaluator = new CustomPermissionEvaluator(roleManagementService);
		TestingAuthenticationToken authentication = new TestingAuthenticationToken("user", "n/a");

		assertThatThrownBy(() -> evaluator.hasPermission(authentication, "", "provider_view_history"))
				.isInstanceOf(AccessDeniedException.class)
				.hasMessageContaining("No access for configured resources");
	}

	@Test
	void hasPermissionReturnsFalseForInvalidArguments() {
		RoleManagementServiceStub roleManagementService = new RoleManagementServiceStub(List.of());
		CustomPermissionEvaluator evaluator = new CustomPermissionEvaluator(roleManagementService);

		assertThat(evaluator.hasPermission(null, "", "provider_view_history")).isFalse();
		assertThat(evaluator.hasPermission(new TestingAuthenticationToken("user", "n/a", "User"), null,
				"provider_view_history")).isFalse();
		assertThat(evaluator.hasPermission(new TestingAuthenticationToken("user", "n/a", "User"), "", 42)).isFalse();
	}

	private static final class RoleManagementServiceStub extends RoleManagementService {

		private final List<RolePermissionEntity> mappings;
		private List<String> roles = List.of();
		private List<String> permissions = List.of();

		private RoleManagementServiceStub(List<RolePermissionEntity> mappings) {
			super(null, null, null);
			this.mappings = mappings;
		}

		@Override
		public List<RolePermissionEntity> findAll(List<String> role, List<String> permission) {
			this.roles = role;
			this.permissions = permission;
			return mappings;
		}

		private List<String> roles() {
			return roles;
		}

		private List<String> permissions() {
			return permissions;
		}
	}
}
