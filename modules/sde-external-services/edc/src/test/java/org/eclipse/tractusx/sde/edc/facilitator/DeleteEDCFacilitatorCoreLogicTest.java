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

package org.eclipse.tractusx.sde.edc.facilitator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.tractusx.sde.common.exception.ServiceException;
import org.eclipse.tractusx.sde.edc.api.EDCFeignClientApi;
import org.eclipse.tractusx.sde.edc.entities.request.asset.AssetEntryRequest;
import org.eclipse.tractusx.sde.edc.entities.request.businesspartnergroup.BusinessPartnerGroupRequest;
import org.eclipse.tractusx.sde.edc.entities.request.contractdefinition.ContractDefinitionRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

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

	private static DeleteEDCFacilitator facilitator(EDCFeignClientApi api) {
		DeleteEDCFacilitator facilitator = new DeleteEDCFacilitator(api);
		ReflectionTestUtils.setField(facilitator, "providerHostWithManagementPath", "http://localhost/management");
		return facilitator;
	}

	@Test
	void findEDCOfferInformationDeletesContractPoliciesAndAssetFromLookupResult() {
		FakeEDCFeignClientApi api = new FakeEDCFeignClientApi();
		api.contractDefinition = MAPPER.createObjectNode()
				.put("@id", "contract-id")
				.put("edc:accessPolicyId", "access-policy-id")
				.put("edc:contractPolicyId", "usage-policy-id");

		facilitator(api).findEDCOfferInformation("shell-1", "submodel-1");

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

		facilitator(api).deleteUsagePolicy(" ");

		assertThat(api.calls).isEmpty();
	}

	@Test
	void deleteMethodsIgnoreCurrentNotFoundStyleErrors() {
		FakeEDCFeignClientApi api = new FakeEDCFeignClientApi();
		api.throwNotFound = true;

		facilitator(api).deleteAssets("missing-asset");

		assertThat(api.calls).containsExactly("deleteAsset:missing-asset");
	}

	@Test
	void deleteMethodsWrapNonNotFoundErrorsAsServiceException() {
		FakeEDCFeignClientApi api = new FakeEDCFeignClientApi();
		api.throwFailure = true;

		assertThatThrownBy(() -> facilitator(api).deleteAssets("asset-1"))
				.isInstanceOf(ServiceException.class)
				.hasMessageContaining("Exception in EDC delete request process");
	}

	private static final class FakeEDCFeignClientApi implements EDCFeignClientApi {
		private final List<String> calls = new ArrayList<>();
		private JsonNode contractDefinition = MAPPER.createObjectNode();
		private boolean throwNotFound;
		private boolean throwFailure;

		@Override
		public JsonNode getContractDefination(URI url, String id, Map<String, String> requestHeader) {
			calls.add("getContract:" + id);
			return contractDefinition;
		}

		@Override
		public ResponseEntity<Object> deleteContractDefinition(URI url, String contractDefinitionId, Map<String, String> requestHeader) {
			calls.add("deleteContract:" + contractDefinitionId);
			throwIfConfigured();
			return ResponseEntity.noContent().build();
		}

		@Override
		public ResponseEntity<Object> deletePolicyDefinitions(URI url, String policydefinitionsId, Map<String, String> requestHeader) {
			calls.add("deletePolicy:" + policydefinitionsId);
			throwIfConfigured();
			return ResponseEntity.noContent().build();
		}

		@Override
		public ResponseEntity<Object> deleteAssets(URI url, String assetsId, Map<String, String> requestHeader) {
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

		@Override public ResponseEntity<Object> getAsset(URI url, String assetId, Map<String, String> h) { return null; }
		@Override public String createAsset(URI url, AssetEntryRequest requestBody, Map<String, String> h) { return null; }
		@Override public String updateAsset(URI url, AssetEntryRequest requestBody, Map<String, String> h) { return null; }
		@Override public JsonNode getAssetByFilterExpression(URI url, ObjectNode requestBody, Map<String, String> h) { return null; }
		@Override public JsonNode getPolicy(URI url, String policyId, Map<String, String> h) { return null; }
		@Override public JsonNode createPolicy(URI url, JsonNode requestBody, Map<String, String> h) { return null; }
		@Override public JsonNode updatePolicy(URI url, String policyUUId, JsonNode requestBody, Map<String, String> h) { return null; }
		@Override public String createContractDefination(URI url, ContractDefinitionRequest requestBody, Map<String, String> h) { return null; }
		@Override public JsonNode updateContractDefination(URI url, ContractDefinitionRequest requestBody, Map<String, String> h) { return null; }
		@Override public JsonNode getContractDefinitionsByFilterExpression(URI url, ObjectNode requestBody, Map<String, String> h) { return null; }
		@Override public JsonNode getBusinessPartnerGroups(URI url, String bpn, Map<String, String> h) { return null; }
		@Override public void updateBusinessPartnerGroups(URI url, BusinessPartnerGroupRequest requestBody, Map<String, String> h) { }
		@Override public void createBusinessPartnerGroups(URI url, BusinessPartnerGroupRequest requestBody, Map<String, String> h) { }
		@Override public void deleteBusinessPartnerGroups(URI url, String bpn, Map<String, String> h) { }
	}

	private static final class CurrentNotFoundStyleException extends RuntimeException {
		@Override
		public String toString() {
			return "FeignException$NotFound: 404 Not Found";
		}
	}
}
