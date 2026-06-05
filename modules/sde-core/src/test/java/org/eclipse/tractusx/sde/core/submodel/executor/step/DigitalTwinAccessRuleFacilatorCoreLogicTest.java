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

package org.eclipse.tractusx.sde.core.submodel.executor.step;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.tractusx.sde.common.configuration.properties.SDEConfigurationProperties;
import org.eclipse.tractusx.sde.common.constants.SubmoduleCommonColumnsConstant;
import org.eclipse.tractusx.sde.common.entities.Policies;
import org.eclipse.tractusx.sde.common.entities.PolicyModel;
import org.eclipse.tractusx.sde.common.submodel.executor.DatabaseUsecaseStep;
import org.eclipse.tractusx.sde.digitaltwins.facilitator.DigitalTwinsFacilitator;
import org.eclipse.tractusx.sde.digitaltwins.facilitator.DigitalTwinsUtility;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Verifies digital-twin access-rule creation and cleanup behavior.
 *
 * <p>Access rules decide who may discover and read digital twin descriptors. These tests verify the
 * public-readable fallback, BPN-specific rule creation, generated rule-id persistence on the working
 * row, and the current old-rule deletion behavior. External DTR calls are replaced by local fakes.</p>
 */
class DigitalTwinAccessRuleFacilatorCoreLogicTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	void createAccessRuleCreatesPublicReadableRuleWhenPolicyHasNoBpnConstraints() {
		FakeDigitalTwinsFacilitator facilitator = new FakeDigitalTwinsFacilitator();
		DigitalTwinAccessRuleFacilator accessRules = accessRules(facilitator, new FakeDatabaseUsecaseStep(null));
		ObjectNode row = row();

		accessRules.createAccessRule(1, row, Map.of("manufacturerPartId", "part-1"), policy(List.of()),
				"urn:samm:test");

		assertThat(facilitator.createdRuleBpns).containsExactly("PUBLIC_READABLE");
		assertThat(row.get(SubmoduleCommonColumnsConstant.SHELL_ACCESS_RULE_IDS).asText()).isEqualTo("rule-1");
	}

	@Test
	void createAccessRuleCreatesOneRuleForEachAccessBpn() {
		FakeDigitalTwinsFacilitator facilitator = new FakeDigitalTwinsFacilitator();
		DigitalTwinAccessRuleFacilator accessRules = accessRules(facilitator, new FakeDatabaseUsecaseStep(null));
		ObjectNode row = row();

		accessRules.createAccessRule(1, row, Map.of("manufacturerPartId", "part-1"),
				policy(List.of("BPNL00000003AAA1", "BPNL00000003BBB2")), "urn:samm:test");

		assertThat(facilitator.createdRuleBpns).containsExactly("BPNL00000003AAA1", "BPNL00000003BBB2");
		assertThat(row.get(SubmoduleCommonColumnsConstant.SHELL_ACCESS_RULE_IDS).asText()).isEqualTo("rule-1,rule-2");
	}

	@Test
	void createAccessRuleDeletesExistingRuleIdsBeforeCreatingNewOnes() {
		FakeDigitalTwinsFacilitator facilitator = new FakeDigitalTwinsFacilitator();
		JsonObject existing = new JsonObject();
		existing.addProperty(SubmoduleCommonColumnsConstant.SHELL_ACCESS_RULE_IDS, "old-1, old-2");
		DigitalTwinAccessRuleFacilator accessRules = accessRules(facilitator, new FakeDatabaseUsecaseStep(existing));

		accessRules.createAccessRule(1, row(), Map.of("manufacturerPartId", "part-1"), policy(List.of()),
				"urn:samm:test");

		assertThat(facilitator.deletedRules).containsExactly("old-1, old-2", "old-1, old-2");
	}

	private static DigitalTwinAccessRuleFacilator accessRules(FakeDigitalTwinsFacilitator facilitator,
			DatabaseUsecaseStep database) {
		SDEConfigurationProperties properties = new SDEConfigurationProperties();
		properties.setManufacturerId("BPNL00000003CML1");
		DigitalTwinAccessRuleFacilator accessRules = new DigitalTwinAccessRuleFacilator(facilitator,
				new DigitalTwinsUtility(), properties, database);
		accessRules.init(schema());
		return accessRules;
	}

	private static ObjectNode row() {
		return MAPPER.createObjectNode().put("partInstanceId", "part-1");
	}

	private static PolicyModel policy(List<String> bpns) {
		List<Policies> accessPolicies = bpns.isEmpty()
				? List.of()
				: List.of(Policies.builder().technicalKey("BusinessPartnerNumber").value(bpns).build());
		return PolicyModel.builder().accessPolicies(accessPolicies).usagePolicies(List.of()).build();
	}

	private static JsonObject schema() {
		return JsonParser.parseString("""
				{
				  "id": "serial-part",
				  "items": {
				    "properties": {},
				    "required": [],
				    "dependentRequired": {}
				  },
				  "addOn": {
				    "identifier": "${partInstanceId}",
				    "lookupShellSpecificAssetIdsSpecs": {},
				    "shortIdSpecs": [],
				    "responseTemplate": {}
				  }
				}
				""").getAsJsonObject();
	}

	private static final class FakeDigitalTwinsFacilitator extends DigitalTwinsFacilitator {
		private final List<String> createdRuleBpns = new ArrayList<>();
		private final List<String> deletedRules = new ArrayList<>();

		private FakeDigitalTwinsFacilitator() {
			super(null, null, null);
		}

		@Override
		public JsonNode createAccessControlsRule(String edcBpn, JsonNode request) {
			createdRuleBpns.add(request.at("/policy/accessRules/0/value").asText());
			return MAPPER.createObjectNode().put("id", "rule-" + createdRuleBpns.size());
		}

		@Override
		public void deleteAccessControlsRule(String ruleId, String edcBpn) {
			deletedRules.add(ruleId);
		}
	}

	private static final class FakeDatabaseUsecaseStep implements DatabaseUsecaseStep {
		private final JsonObject existing;

		private FakeDatabaseUsecaseStep(JsonObject existing) {
			this.existing = existing;
		}

		@Override public void init(JsonObject submodelSchema) { }
		@Override public JsonNode run(Integer rowIndex, ObjectNode jsonObject, String processId, PolicyModel policy) { return jsonObject; }
		@Override public void saveSubmoduleWithDeleted(Integer rowIndex, JsonObject jsonObject, String delProcessId, String refProcessId) { }
		@Override public List<JsonObject> readCreatedTwins(String processId, String isDeleted) { return List.of(); }
		@Override public JsonObject readCreatedTwinsBySpecifyColomn(String sematicId, String value) { return null; }
		@Override public JsonObject readCreatedTwinsDetails(String uuid) {
			if (existing == null) {
				throw new org.eclipse.tractusx.sde.common.exception.NoDataFoundException("none");
			}
			return existing;
		}
		@Override public int getUpdatedData(String processId) { return 0; }
	}
}
