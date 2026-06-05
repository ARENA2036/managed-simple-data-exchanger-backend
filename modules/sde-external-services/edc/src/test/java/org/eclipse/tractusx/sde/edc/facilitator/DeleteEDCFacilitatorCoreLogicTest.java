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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.tractusx.sde.common.exception.ServiceException;
import org.eclipse.tractusx.sde.edc.api.EDCFeignClientApi;
import org.eclipse.tractusx.sde.edc.entities.request.asset.AssetEntryRequest;
import org.eclipse.tractusx.sde.edc.entities.request.businesspartnergroup.BusinessPartnerGroupRequest;
import org.eclipse.tractusx.sde.edc.entities.request.contractdefinition.ContractDefinitionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

/**
 * Tests the EDC deletion facilitator used when provider data is removed.
 *
 * <p>The facilitator translates known EDC identifiers into delete calls and intentionally tolerates
 * 404-style not-found responses. These tests protect the deletion order, blank usage-policy handling,
 * and exception parsing behavior without calling a live EDC API.</p>
 */
class DeleteEDCFacilitatorCoreLogicTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	void findEDCOfferInformationDeletesContractPoliciesAndAssetFromLookupResult() {
		FakeEDCFeignClientApi api = new FakeEDCFeignClientApi();
		api.contractDefinition = MAPPER.createObjectNode()
				.put("@id", "contract-id")
				.put("edc:accessPolicyId", "access-policy-id")
				.put("edc:contractPolicyId", "usage-policy-id");

		new DeleteEDCFacilitator(api).findEDCOfferInformation("shell-1", "submodel-1");

		assertThat(api.calls).containsExactly(
				"getContract:shell-1-submodel-1",
				"deleteContract:contract-id",
				"deletePolicy:access-policy-id",
				"deletePolicy:usage-policy-id",
				"deleteAsset:shell-1-submodel-1");
	}

	@Test
	void deleteUsagePolicySkipsBlankIds() {
		FakeEDCFeignClientApi api = new FakeEDCFeignClientApi();

		new DeleteEDCFacilitator(api).deleteUsagePolicy(" ");

		assertThat(api.calls).isEmpty();
	}

	@Test
	void deleteMethodsIgnoreCurrentNotFoundStyleErrors() {
		FakeEDCFeignClientApi api = new FakeEDCFeignClientApi();
		api.throwNotFound = true;

		new DeleteEDCFacilitator(api).deleteAssets("missing-asset");

		assertThat(api.calls).containsExactly("deleteAsset:missing-asset");
	}

	@Test
	void deleteMethodsWrapNonNotFoundErrorsAsServiceException() {
		FakeEDCFeignClientApi api = new FakeEDCFeignClientApi();
		api.throwFailure = true;

		assertThatThrownBy(() -> new DeleteEDCFacilitator(api).deleteAssets("asset-1"))
				.isInstanceOf(ServiceException.class)
				.hasMessageContaining("Exception in EDC delete request process");
	}

	private static final class FakeEDCFeignClientApi implements EDCFeignClientApi {
		private final List<String> calls = new ArrayList<>();
		private JsonNode contractDefinition = MAPPER.createObjectNode();
		private boolean throwNotFound;
		private boolean throwFailure;

		@Override
		public JsonNode getContractDefination(String id) {
			calls.add("getContract:" + id);
			return contractDefinition;
		}

		@Override
		public ResponseEntity<Object> deleteContractDefinition(String contractDefinitionId) {
			calls.add("deleteContract:" + contractDefinitionId);
			throwIfConfigured();
			return ResponseEntity.noContent().build();
		}

		@Override
		public ResponseEntity<Object> deletePolicyDefinitions(String policydefinitionsId) {
			calls.add("deletePolicy:" + policydefinitionsId);
			throwIfConfigured();
			return ResponseEntity.noContent().build();
		}

		@Override
		public ResponseEntity<Object> deleteAssets(String assetsId) {
			calls.add("deleteAsset:" + assetsId);
			throwIfConfigured();
			return ResponseEntity.noContent().build();
		}

		private void throwIfConfigured() {
			if (throwNotFound) {
				throw new CurrentNotFoundStyleException();
			}
			if (throwFailure) {
				throw new IllegalStateException("boom");
			}
		}

		@Override public ResponseEntity<Object> getAsset(String assetId) { return null; }
		@Override public String createAsset(AssetEntryRequest requestBody) { return null; }
		@Override public String updateAsset(AssetEntryRequest requestBody) { return null; }
		@Override public JsonNode getAssetByFilterExpression(ObjectNode requestBody) { return null; }
		@Override public JsonNode getPolicy(String policyId) { return null; }
		@Override public JsonNode createPolicy(JsonNode requestBody) { return null; }
		@Override public JsonNode updatePolicy(String policyUUId, JsonNode requestBody) { return null; }
		@Override public String createContractDefination(ContractDefinitionRequest requestBody) { return null; }
		@Override public JsonNode updateContractDefination(ContractDefinitionRequest requestBody) { return null; }
		@Override public JsonNode getContractDefinitionsByFilterExpression(ObjectNode requestBody) { return null; }
		@Override public JsonNode getBusinessPartnerGroups(String bpn) { return null; }
		@Override public void updateBusinessPartnerGroups(BusinessPartnerGroupRequest requestBody) { }
		@Override public void createBusinessPartnerGroups(BusinessPartnerGroupRequest requestBody) { }
		@Override public void deleteBusinessPartnerGroups(String bpn) { }
	}

	private static final class CurrentNotFoundStyleException extends RuntimeException {
		@Override
		public String toString() {
			return "FeignException$NotFound: 404 Not Found";
		}
	}
}
