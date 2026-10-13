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

package org.eclipse.tractusx.sde.edc.entities.request.policies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.eclipse.tractusx.sde.common.configuration.properties.EDCVersionConfigurationProperties;
import org.eclipse.tractusx.sde.common.configuration.properties.SDEConfigurationProperties;
import org.eclipse.tractusx.sde.common.dataspace.DataspaceProfileProperties;
import org.eclipse.tractusx.sde.common.entities.Policies;
import org.eclipse.tractusx.sde.common.mapper.JsonObjectMapper;
import org.eclipse.tractusx.sde.edc.constants.EDCAssetConfigurableConstant;
import org.junit.jupiter.api.Test;

class PolicyBuilderDataspaceProfileTest {

	private static final List<Policies> CATENA_X_ACCESS = List.of(
			Policies.builder().technicalKey("BusinessPartnerNumber").value(List.of("BPNL000000000001")).build(),
			Policies.builder().technicalKey("cx-policy:Membership").value(List.of("active")).build());

	@Test
	void constructXUsesPlainOdrlContextAndUseAction() {
		PolicyConstraintBuilderService builder = builder("constructx");

		PolicyDefinitionRequest policy = policyRequest(builder, "constructx", PolicyConstraintBuilderService.ACCESS_POLICY_TYPE);

		assertThat(policy.getContext()).isEqualTo(List.of("http://www.w3.org/ns/odrl.jsonld",
				Map.of("@vocab", "https://w3id.org/edc/v0.0.1/ns/")));
		assertThat(policy.getPolicy().getPermission()).isEmpty();
	}

	@Test
	void constructXDropsCatenaXOperandsButKeepsInForceDate() {
		PolicyConstraintBuilderService builder = builder("constructx");

		assertThat(builder.getPoliciesConstraints(CATENA_X_ACCESS, PolicyConstraintBuilderService.ACCESS_POLICY_TYPE))
				.isEmpty();

		List<ActionRequest> withDate = builder.getPoliciesConstraints(
				List.of(Policies.builder().technicalKey("inForceDate").value(List.of("2027-01-01")).build()),
				PolicyConstraintBuilderService.USAGE_POLICY_TYPE);
		assertThat(withDate).hasSize(1);
		assertThat(withDate.get(0).toJsonString()).contains("inForceDate").doesNotContain("Membership");
	}

	@Test
	void tractusXKeepsCatenaXContextActionAndOperands() {
		PolicyConstraintBuilderService builder = builder("tractusx");

		PolicyDefinitionRequest policy = policyRequest(builder, "tractusx", PolicyConstraintBuilderService.ACCESS_POLICY_TYPE);

		assertThat(policy.getContext().toString()).contains("https://w3id.org/catenax/2025/9/policy/context.jsonld");
		assertThat(policy.getPolicy().getPermission()).hasSize(1);
		assertThat(policy.getPolicy().getPermission().get(0).getAction()).isEqualTo("access");
		assertThat(builder.getPoliciesConstraints(CATENA_X_ACCESS, PolicyConstraintBuilderService.ACCESS_POLICY_TYPE)
				.get(0).toJsonString()).contains("BusinessPartnerNumber", "Membership");
	}

	private static PolicyDefinitionRequest policyRequest(PolicyConstraintBuilderService builder, String profile, String type) {
		return factory(profile).getPolicy("asset-1", "asset-1", builder.getPoliciesConstraints(CATENA_X_ACCESS, type), type);
	}

	private static PolicyConstraintBuilderService builder(String profile) {
		return new PolicyConstraintBuilderService(factory(profile), mock(JsonObjectMapper.class), constant(), version(),
				mock(SDEConfigurationProperties.class), new DataspaceProfileProperties(profile));
	}

	private static PolicyRequestFactory factory(String profile) {
		return new PolicyRequestFactory(constant(), version(), mock(JsonObjectMapper.class),
				new DataspaceProfileProperties(profile));
	}

	private static EDCAssetConfigurableConstant constant() {
		EDCAssetConfigurableConstant constant = mock(EDCAssetConfigurableConstant.class);
		when(constant.getManufacturerId()).thenReturn("BPNL000000000065");
		return constant;
	}

	private static EDCVersionConfigurationProperties version() {
		EDCVersionConfigurationProperties version = mock(EDCVersionConfigurationProperties.class);
		when(version.getMinor()).thenReturn(13);
		return version;
	}
}
