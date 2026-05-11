/********************************************************************************
 * Copyright (c) 2023, 2024 T-Systems International GmbH
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
 * Copyright (c) 2026 ARENA2036 e.V.
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

package org.eclipse.tractusx.sde.edc.facilitator;

import java.util.Map;
import java.util.function.BiConsumer;
import java.util.function.Consumer;

import org.eclipse.tractusx.sde.common.constants.SubmoduleCommonColumnsConstant;
import org.eclipse.tractusx.sde.common.entities.PolicyModel;
import org.eclipse.tractusx.sde.edc.entities.request.asset.AssetEntryRequest;
import org.eclipse.tractusx.sde.edc.entities.request.contractdefinition.ContractDefinitionRequest;
import org.eclipse.tractusx.sde.edc.entities.request.contractdefinition.ContractDefinitionRequestFactory;
import org.eclipse.tractusx.sde.edc.entities.request.policies.PolicyConstraintBuilderService;
import org.eclipse.tractusx.sde.edc.gateways.external.EDCGateway;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class CreateEDCAssetFacilitator extends AbstractEDCStepsHelper {

	private final EDCGateway edcGateway;
	private final ContractDefinitionRequestFactory contractFactory;
	private final PolicyConstraintBuilderService policyConstraintBuilderService;


	public Map<String, String> createAssetWithPoliciesAndContract(AssetEntryRequest assetEntryRequest, PolicyModel policy) {
		return processAssetWithPoliciesAndContract(
				assetEntryRequest,
				policy,
				edcGateway::createAsset,
				(policyId, policyRequest) -> edcGateway.createPolicyDefinition(policyRequest),
				edcGateway::createContractDefinition
		);
	}

	public Map<String, String> updateAssetWithPoliciesAndContract(AssetEntryRequest assetEntryRequest, PolicyModel policy) {
		return processAssetWithPoliciesAndContract(
				assetEntryRequest,
				policy,
				edcGateway::updateAsset,
				edcGateway::updatePolicyDefinition,
				edcGateway::updateContractDefinition
		);
	}

	public Map<String, String> updateAssetAndCreatePoliciesAndContract(AssetEntryRequest assetEntryRequest, PolicyModel policy) {
		return processAssetWithPoliciesAndContract(
				assetEntryRequest,
				policy,
				edcGateway::updateAsset,
				(policyId, policyRequest) -> edcGateway.createPolicyDefinition(policyRequest),
				edcGateway::createContractDefinition
		);
	}

	private Map<String, String> processAssetWithPoliciesAndContract(
			AssetEntryRequest assetEntryRequest,
			PolicyModel policy,
			Consumer<AssetEntryRequest> assetAction,
			BiConsumer<String, JsonNode> policyAction,
			Consumer<ContractDefinitionRequest> contractAction) {

		assetAction.accept(assetEntryRequest);

		String assetId = assetEntryRequest.getId();

		JsonNode accessPolicy = policyConstraintBuilderService.getAccessPolicy(assetId, assetId, policy);
		String accessPolicyId = extractId(accessPolicy);
		policyAction.accept(accessPolicyId, accessPolicy);

		JsonNode usagePolicy = policyConstraintBuilderService.getUsagePolicy(assetId, assetId, policy);
		String usagePolicyId = extractId(usagePolicy);
		policyAction.accept(usagePolicyId, usagePolicy);

		ContractDefinitionRequest contractDefinitionRequest = contractFactory.createContractDefinitionRequest(assetId, assetId, accessPolicyId, usagePolicyId);

		contractAction.accept(contractDefinitionRequest);

		return Map.of(
				SubmoduleCommonColumnsConstant.ASSET_ID, assetId,
				SubmoduleCommonColumnsConstant.ACCESS_POLICY_ID, accessPolicyId,
				SubmoduleCommonColumnsConstant.USAGE_POLICY_ID, usagePolicyId,
				SubmoduleCommonColumnsConstant.CONTRACT_DEFINATION_ID, contractDefinitionRequest.getId()
		);
	}

	private String extractId(JsonNode jsonNode) {
		JsonNode atIdNode = jsonNode.get("@id");
		return atIdNode != null && !atIdNode.isNull() ? atIdNode.asText() : "";
	}
}
