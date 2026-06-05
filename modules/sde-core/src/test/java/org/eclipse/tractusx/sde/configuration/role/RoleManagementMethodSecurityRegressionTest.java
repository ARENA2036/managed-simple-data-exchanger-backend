/********************************************************************************
 * Copyright (c) 2026 ARENA2036 e.V.
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations
 * under the License.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.sde.configuration.role;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.tractusx.sde.core.controller.RoleManagementController;
import org.eclipse.tractusx.sde.core.role.entity.RolePermissionEntity;
import org.eclipse.tractusx.sde.core.role.entity.RolePojo;
import org.eclipse.tractusx.sde.core.service.RoleManagementService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.PermissionEvaluator;
import org.springframework.security.access.expression.method.DefaultMethodSecurityExpressionHandler;
import org.springframework.security.access.expression.method.MethodSecurityExpressionHandler;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(RoleManagementMethodSecurityRegressionTest.MethodSecurityTestConfiguration.class)
/**
 * Method-security regression tests for role and permission management services.
 *
 * <p>The tests execute selected service methods through Spring method security to verify that
 * configured permissions actually allow or deny access as intended. This complements the lower-level
 * permission evaluator tests by exercising {@code @PreAuthorize} behavior on real service entry
 * points, while using local test beans to keep the suite deterministic and independent from Keycloak.</p>
 */
class RoleManagementMethodSecurityRegressionTest {

	@Autowired
	private RoleManagementController controller;

	@Autowired
	private RoleManagementServiceStub roleManagementService;

	@BeforeEach
	void resetStub() {
		roleManagementService.reset();
	}

	@AfterEach
	void clearSecurityContext() {
		SecurityContextHolder.clearContext();
	}

	@Test
	void saveRoleAllowsUserWhenCreateRolePermissionIsSeededForAuthority() {
		roleManagementService.allow("User", "create_role");
		SecurityContextHolder.getContext()
				.setAuthentication(new TestingAuthenticationToken("user", "n/a", "User"));
		RolePojo request = role("RefactoringReviewer");

		RolePojo response = controller.saveRole(request);

		assertThat(response).isSameAs(request);
		assertThat(roleManagementService.savedRoles()).containsExactly("RefactoringReviewer");
		assertThat(roleManagementService.lastRoles()).containsExactly("User");
		assertThat(roleManagementService.lastPermissions()).containsExactly("create_role");
	}

	@Test
	void saveRoleDeniesCreatorWhenCreateRolePermissionIsMissing() {
		SecurityContextHolder.getContext()
				.setAuthentication(new TestingAuthenticationToken("creator", "n/a", "Creator"));

		assertThatThrownBy(() -> controller.saveRole(role("ForbiddenRole")))
				.isInstanceOf(AccessDeniedException.class);
		assertThat(roleManagementService.savedRoles()).isEmpty();
		assertThat(roleManagementService.lastRoles()).containsExactly("Creator");
		assertThat(roleManagementService.lastPermissions()).containsExactly("create_role");
	}

	@Test
	void getRolePermissionAllowsAnyAuthorityWithReadRolePermissionMapping() {
		roleManagementService.allow("Admin", "read_role_permission");
		roleManagementService.permissionsForRole("Creator", List.of("provider_create_contract_offer"));
		SecurityContextHolder.getContext()
				.setAuthentication(new TestingAuthenticationToken("admin", "n/a", "Admin"));

		assertThat(controller.getRolePermission("Creator")).containsExactly("provider_create_contract_offer");
		assertThat(roleManagementService.lastRoles()).containsExactly("Admin");
		assertThat(roleManagementService.lastPermissions()).containsExactly("read_role_permission");
	}

	private RolePojo role(String roleName) {
		RolePojo role = new RolePojo();
		role.setRole(roleName);
		role.setDescription("Regression test role");
		return role;
	}

	@Configuration
	@EnableMethodSecurity(prePostEnabled = true)
	static class MethodSecurityTestConfiguration {

		@Bean
		RoleManagementServiceStub roleManagementService() {
			return new RoleManagementServiceStub();
		}

		@Bean
		RoleManagementController roleManagementController(RoleManagementService roleManagementService) {
			return new RoleManagementController(roleManagementService);
		}

		@Bean
		CustomPermissionEvaluator customPermissionEvaluator(RoleManagementService roleManagementService) {
			return new CustomPermissionEvaluator(roleManagementService);
		}

		@Bean
		static MethodSecurityExpressionHandler methodSecurityExpressionHandler(
				PermissionEvaluator permissionEvaluator) {
			DefaultMethodSecurityExpressionHandler expressionHandler = new DefaultMethodSecurityExpressionHandler();
			expressionHandler.setPermissionEvaluator(permissionEvaluator);
			return expressionHandler;
		}
	}

	static final class RoleManagementServiceStub extends RoleManagementService {

		private final List<RolePermissionEntity> allowedMappings = new ArrayList<>();
		private final List<String> savedRoles = new ArrayList<>();
		private List<String> lastRoles = List.of();
		private List<String> lastPermissions = List.of();
		private String permissionLookupRole;
		private List<String> permissionLookupResult = List.of();

		RoleManagementServiceStub() {
			super(null, null, null);
		}

		void reset() {
			allowedMappings.clear();
			savedRoles.clear();
			lastRoles = List.of();
			lastPermissions = List.of();
			permissionLookupRole = null;
			permissionLookupResult = List.of();
		}

		void allow(String role, String permission) {
			allowedMappings.add(RolePermissionEntity.builder()
					.sdeRole(role)
					.sdePermission(permission)
					.build());
		}

		void permissionsForRole(String role, List<String> permissions) {
			permissionLookupRole = role;
			permissionLookupResult = permissions;
		}

		@Override
		public List<RolePermissionEntity> findAll(List<String> role, List<String> permission) {
			lastRoles = role;
			lastPermissions = permission;
			return allowedMappings.stream()
					.filter(mapping -> role.contains(mapping.getSdeRole()))
					.filter(mapping -> permission.contains(mapping.getSdePermission()))
					.toList();
		}

		@Override
		public RolePojo saveRole(RolePojo role) {
			savedRoles.add(role.getRole());
			return role;
		}

		@Override
		public List<String> getRolePermission(List<String> role) {
			if (role.contains(permissionLookupRole)) {
				return permissionLookupResult;
			}
			return List.of();
		}

		List<String> savedRoles() {
			return savedRoles;
		}

		List<String> lastRoles() {
			return lastRoles;
		}

		List<String> lastPermissions() {
			return lastPermissions;
		}
	}
}
