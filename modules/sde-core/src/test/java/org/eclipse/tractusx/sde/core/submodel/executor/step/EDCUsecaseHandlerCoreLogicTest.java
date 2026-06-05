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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.tractusx.sde.common.constants.SubmoduleCommonColumnsConstant;
import org.eclipse.tractusx.sde.common.entities.PolicyModel;
import org.eclipse.tractusx.sde.common.exception.CsvHandlerUseCaseException;
import org.eclipse.tractusx.sde.edc.entities.request.asset.AssetEntryRequest;
import org.eclipse.tractusx.sde.edc.entities.request.asset.AssetEntryRequestFactory;
import org.eclipse.tractusx.sde.edc.facilitator.CreateEDCAssetFacilitator;
import org.eclipse.tractusx.sde.edc.facilitator.DeleteEDCFacilitator;
import org.eclipse.tractusx.sde.edc.gateways.external.EDCGateway;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Verifies the EDC executor step that bridges submodel processing to EDC asset, policy, and contract
 * management.
 *
 * <p>The tests use local fakes for the asset factory, gateway, create facilitator, and delete
 * facilitator. They protect the decision between create and update flows, the propagation of returned
 * EDC identifiers into the working JSON object, error wrapping with row context, and the current
 * delete-field mapping. This keeps core provider-pipeline behavior testable without any live EDC
 * control plane.</p>
 */
class EDCUsecaseHandlerCoreLogicTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	void runCreatesAssetWithPoliciesAndContractWhenAssetDoesNotExist() {
		FakeEDCGateway gateway = new FakeEDCGateway(false);
		FakeCreateEDCAssetFacilitator facilitator = new FakeCreateEDCAssetFacilitator();
		EDCUsecaseHandler handler = handler(gateway, facilitator, new FakeDeleteEDCFacilitator());
		handler.init(schema());

		ObjectNode result = handler.run(4, assetInfo(), "process-1", new PolicyModel());

		assertThat(facilitator.createCalls).isEqualTo(1);
		assertThat(facilitator.updateCalls).isZero();
		assertThat(facilitator.lastAssetEntryRequest.getId()).isEqualTo("shell-1-submodel-1");
		assertThat(result.get(SubmoduleCommonColumnsConstant.ASSET_ID).asText()).isEqualTo("asset-id");
		assertThat(result.get(SubmoduleCommonColumnsConstant.ACCESS_POLICY_ID).asText()).isEqualTo("access-policy-id");
		assertThat(result.get(SubmoduleCommonColumnsConstant.USAGE_POLICY_ID).asText()).isEqualTo("usage-policy-id");
		assertThat(result.get(SubmoduleCommonColumnsConstant.CONTRACT_DEFINATION_ID).asText()).isEqualTo("contract-id");
		assertThat(gateway.lastAssetLookupId).isEqualTo("shell-1-submodel-1");
	}

	@Test
	void runUpdatesAssetWithPoliciesAndContractWhenAssetAlreadyExists() {
		FakeEDCGateway gateway = new FakeEDCGateway(true);
		FakeCreateEDCAssetFacilitator facilitator = new FakeCreateEDCAssetFacilitator();
		EDCUsecaseHandler handler = handler(gateway, facilitator, new FakeDeleteEDCFacilitator());
		handler.init(schema());

		handler.run(4, assetInfo(), "process-1", new PolicyModel());

		assertThat(facilitator.createCalls).isZero();
		assertThat(facilitator.updateCalls).isEqualTo(1);
	}

	@Test
	void runWrapsGatewayFailuresWithCsvRowContext() {
		FakeEDCGateway gateway = new FakeEDCGateway(false);
		gateway.failLookup = true;
		EDCUsecaseHandler handler = handler(gateway, new FakeCreateEDCAssetFacilitator(), new FakeDeleteEDCFacilitator());
		handler.init(schema());

		assertThatThrownBy(() -> handler.run(9, assetInfo(), "process-1", new PolicyModel()))
				.isInstanceOf(CsvHandlerUseCaseException.class)
				.hasMessageContaining("RowPosition")
				.hasMessageContaining("9")
				.hasMessageContaining("EDC:");
	}

	@Test
	void deleteDelegatesCurrentContractPolicyAndAssetFieldMapping() {
		FakeDeleteEDCFacilitator deleteFacilitator = new FakeDeleteEDCFacilitator();
		EDCUsecaseHandler handler = handler(new FakeEDCGateway(false), new FakeCreateEDCAssetFacilitator(),
				deleteFacilitator);

		JsonObject row = new JsonObject();
		row.addProperty(SubmoduleCommonColumnsConstant.CONTRACT_DEFINATION_ID, "contract-id");
		row.addProperty(SubmoduleCommonColumnsConstant.ACCESS_POLICY_ID, "access-policy-id");
		row.addProperty(SubmoduleCommonColumnsConstant.USAGE_POLICY_ID, "usage-policy-id");
		row.addProperty(SubmoduleCommonColumnsConstant.ASSET_ID, "asset-id");

		handler.delete(1, row, "delete-process", "reference-process");

		assertThat(deleteFacilitator.calls).containsExactly(
				"contract:contract-id",
				"access:usage-policy-id",
				"usage:access-policy-id",
				"asset:asset-id");
	}

	private static EDCUsecaseHandler handler(FakeEDCGateway gateway, FakeCreateEDCAssetFacilitator facilitator,
			FakeDeleteEDCFacilitator deleteFacilitator) {
		return new EDCUsecaseHandler(new FakeAssetEntryRequestFactory(), gateway, facilitator, deleteFacilitator);
	}

	private static ObjectNode assetInfo() {
		ObjectNode objectNode = MAPPER.createObjectNode();
		objectNode.put(SubmoduleCommonColumnsConstant.SHELL_ID, "shell-1");
		objectNode.put(SubmoduleCommonColumnsConstant.SUBMODULE_ID, "submodel-1");
		objectNode.put("partInstanceId", "part-1");
		return objectNode;
	}

	private static JsonObject schema() {
		return JsonParser.parseString("""
				{
				  "id": "serial-part",
				  "shortDescription": "Serial Part",
				  "semantic_id": "urn:samm:serial-part",
				  "submodelUriPath": "public",
				  "items": {
				    "properties": {},
				    "required": [],
				    "dependentRequired": {}
				  },
				  "addOn": {
				    "identifier": "${partInstanceId}",
				    "databaseIdentifierSpecs": ["${partInstanceId}"],
				    "lookupShellSpecificAssetIdsSpecs": {},
				    "shortIdSpecs": [],
				    "responseTemplate": {}
				  }
				}
				""").getAsJsonObject();
	}

	private static final class FakeAssetEntryRequestFactory extends AssetEntryRequestFactory {
		private FakeAssetEntryRequestFactory() {
			super(null, null, null);
		}

		@Override
		public AssetEntryRequest createAssetRequest(String submodel, String assetName, String shellId, String subModelId,
				String submoduleUriPath, String uuid, String sematicId, String dctType) {
			return AssetEntryRequest.builder().id(shellId + "-" + subModelId).build();
		}
	}

	private static final class FakeEDCGateway extends EDCGateway {
		private final boolean assetExists;
		private boolean failLookup;
		private String lastAssetLookupId;

		private FakeEDCGateway(boolean assetExists) {
			super(null);
			this.assetExists = assetExists;
		}

		@Override
		public boolean assetExistsLookup(String id) {
			lastAssetLookupId = id;
			if (failLookup) {
				throw new IllegalStateException("lookup failed");
			}
			return assetExists;
		}
	}

	private static final class FakeCreateEDCAssetFacilitator extends CreateEDCAssetFacilitator {
		private int createCalls;
		private int updateCalls;
		private AssetEntryRequest lastAssetEntryRequest;

		private FakeCreateEDCAssetFacilitator() {
			super(null, null, null);
		}

		@Override
		public Map<String, String> createAssetWithPoliciesAndContract(AssetEntryRequest assetEntryRequest,
				PolicyModel policy) {
			createCalls++;
			lastAssetEntryRequest = assetEntryRequest;
			return edcIds();
		}

		@Override
		public Map<String, String> updateAssetWithPoliciesAndContract(AssetEntryRequest assetEntryRequest,
				PolicyModel policy) {
			updateCalls++;
			lastAssetEntryRequest = assetEntryRequest;
			return edcIds();
		}

		private Map<String, String> edcIds() {
			Map<String, String> ids = new LinkedHashMap<>();
			ids.put(SubmoduleCommonColumnsConstant.ASSET_ID, "asset-id");
			ids.put(SubmoduleCommonColumnsConstant.ACCESS_POLICY_ID, "access-policy-id");
			ids.put(SubmoduleCommonColumnsConstant.USAGE_POLICY_ID, "usage-policy-id");
			ids.put(SubmoduleCommonColumnsConstant.CONTRACT_DEFINATION_ID, "contract-id");
			return ids;
		}
	}

	private static final class FakeDeleteEDCFacilitator extends DeleteEDCFacilitator {
		private final List<String> calls = new java.util.ArrayList<>();

		private FakeDeleteEDCFacilitator() {
			super(null);
		}

		@Override
		public void deleteContractDefination(String contractDefinationId) {
			calls.add("contract:" + contractDefinationId);
		}

		@Override
		public void deleteAccessPolicy(String accessPolicyId) {
			calls.add("access:" + accessPolicyId);
		}

		@Override
		public void deleteUsagePolicy(String usagePolicyId) {
			calls.add("usage:" + usagePolicyId);
		}

		@Override
		public void deleteAssets(String assetId) {
			calls.add("asset:" + assetId);
		}
	}
}
