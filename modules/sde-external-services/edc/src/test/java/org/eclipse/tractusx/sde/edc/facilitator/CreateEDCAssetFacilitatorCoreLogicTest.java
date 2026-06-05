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

package org.eclipse.tractusx.sde.edc.facilitator;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.tractusx.sde.common.constants.SubmoduleCommonColumnsConstant;
import org.eclipse.tractusx.sde.common.entities.PolicyModel;
import org.eclipse.tractusx.sde.edc.entities.request.asset.AssetEntryRequest;
import org.eclipse.tractusx.sde.edc.entities.request.contractdefinition.ContractDefinitionRequest;
import org.eclipse.tractusx.sde.edc.entities.request.contractdefinition.ContractDefinitionRequestFactory;
import org.eclipse.tractusx.sde.edc.entities.request.policies.PolicyConstraintBuilderService;
import org.eclipse.tractusx.sde.edc.gateways.external.EDCGateway;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests the facilitator that creates or updates the complete EDC asset publication bundle.
 *
 * <p>The facilitator is the core unit that turns one asset request and one policy model into an EDC
 * asset, access policy, usage policy, and contract definition. These tests verify create/update call
 * sequences, returned identifier mapping, and the current missing-policy-id behavior without using a
 * live EDC control plane.</p>
 */
class CreateEDCAssetFacilitatorCoreLogicTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	void createAssetWithPoliciesAndContractCreatesAssetPoliciesAndContractDefinition() {
		FakeEDCGateway gateway = new FakeEDCGateway();
		CreateEDCAssetFacilitator facilitator = new CreateEDCAssetFacilitator(gateway,
				new ContractDefinitionRequestFactory(), new FakePolicyConstraintBuilderService(false));

		Map<String, String> result = facilitator.createAssetWithPoliciesAndContract(asset(), new PolicyModel());

		assertThat(gateway.calls).containsExactly(
				"createAsset:asset-1",
				"createPolicy:access-policy-id",
				"createPolicy:usage-policy-id",
				"createContract:asset-1");
		assertThat(result).containsEntry(SubmoduleCommonColumnsConstant.ASSET_ID, "asset-1")
				.containsEntry(SubmoduleCommonColumnsConstant.ACCESS_POLICY_ID, "access-policy-id")
				.containsEntry(SubmoduleCommonColumnsConstant.USAGE_POLICY_ID, "usage-policy-id")
				.containsEntry(SubmoduleCommonColumnsConstant.CONTRACT_DEFINATION_ID, "asset-1");
		assertThat(gateway.lastContractDefinition.getAccessPolicyId()).isEqualTo("access-policy-id");
		assertThat(gateway.lastContractDefinition.getContractPolicyId()).isEqualTo("usage-policy-id");
	}

	@Test
	void updateAssetWithPoliciesAndContractUsesUpdateOperations() {
		FakeEDCGateway gateway = new FakeEDCGateway();
		CreateEDCAssetFacilitator facilitator = new CreateEDCAssetFacilitator(gateway,
				new ContractDefinitionRequestFactory(), new FakePolicyConstraintBuilderService(false));

		facilitator.updateAssetWithPoliciesAndContract(asset(), new PolicyModel());

		assertThat(gateway.calls).containsExactly(
				"updateAsset:asset-1",
				"updatePolicy:access-policy-id",
				"updatePolicy:usage-policy-id",
				"updateContract:asset-1");
	}

	@Test
	void missingPolicyIdsAreMappedToEmptyStringInCurrentContract() {
		FakeEDCGateway gateway = new FakeEDCGateway();
		CreateEDCAssetFacilitator facilitator = new CreateEDCAssetFacilitator(gateway,
				new ContractDefinitionRequestFactory(), new FakePolicyConstraintBuilderService(true));

		Map<String, String> result = facilitator.createAssetWithPoliciesAndContract(asset(), new PolicyModel());

		assertThat(result).containsEntry(SubmoduleCommonColumnsConstant.ACCESS_POLICY_ID, "")
				.containsEntry(SubmoduleCommonColumnsConstant.USAGE_POLICY_ID, "");
		assertThat(gateway.lastContractDefinition.getAccessPolicyId()).isEmpty();
		assertThat(gateway.lastContractDefinition.getContractPolicyId()).isEmpty();
	}

	private static AssetEntryRequest asset() {
		return AssetEntryRequest.builder().id("asset-1").build();
	}

	private static final class FakeEDCGateway extends EDCGateway {
		private final List<String> calls = new ArrayList<>();
		private ContractDefinitionRequest lastContractDefinition;

		private FakeEDCGateway() {
			super(null);
		}

		@Override
		public String createAsset(AssetEntryRequest request) {
			calls.add("createAsset:" + request.getId());
			return request.getId();
		}

		@Override
		public void updateAsset(AssetEntryRequest request) {
			calls.add("updateAsset:" + request.getId());
		}

		@Override
		public JsonNode createPolicyDefinition(JsonNode request) {
			calls.add("createPolicy:" + request.path("@id").asText());
			return request;
		}

		@Override
		public JsonNode updatePolicyDefinition(String policyUUId, JsonNode request) {
			calls.add("updatePolicy:" + policyUUId);
			return request;
		}

		@Override
		public String createContractDefinition(ContractDefinitionRequest request) {
			lastContractDefinition = request;
			calls.add("createContract:" + request.getId());
			return request.getId();
		}

		@Override
		public void updateContractDefinition(ContractDefinitionRequest request) {
			lastContractDefinition = request;
			calls.add("updateContract:" + request.getId());
		}
	}

	private static final class FakePolicyConstraintBuilderService extends PolicyConstraintBuilderService {
		private final boolean omitIds;

		private FakePolicyConstraintBuilderService(boolean omitIds) {
			super(null, null, null, null, null);
			this.omitIds = omitIds;
		}

		@Override
		public JsonNode getAccessPolicy(String policyId, String assetId, PolicyModel policy) {
			return policy("access-policy-id");
		}

		@Override
		public JsonNode getUsagePolicy(String policyId, String assetId, PolicyModel policy) {
			return policy("usage-policy-id");
		}

		private JsonNode policy(String id) {
			if (omitIds) {
				return MAPPER.createObjectNode();
			}
			return MAPPER.createObjectNode().put("@id", id);
		}
	}
}
