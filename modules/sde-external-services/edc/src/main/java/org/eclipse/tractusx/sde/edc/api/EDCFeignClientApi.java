/********************************************************************************
 * Copyright (c) 2022, 2024 T-Systems International GmbH
 * Copyright (c) 2026 ARENA2036 e.V.
 * Copyright (c) 2022, 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.sde.edc.api;

import java.net.URI;
import java.util.Map;

import org.eclipse.tractusx.sde.edc.entities.request.asset.AssetEntryRequest;
import org.eclipse.tractusx.sde.edc.entities.request.businesspartnergroup.BusinessPartnerGroupRequest;
import org.eclipse.tractusx.sde.edc.entities.request.contractdefinition.ContractDefinitionRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

@FeignClient(value = "EDCFeignClientApi", url = "placeholder")
public interface EDCFeignClientApi {

	// Assets
	@GetMapping(path = "${edc.managementpath.apiversion.asset:/v3}/assets/{id}")
	ResponseEntity<Object> getAsset(URI url, @PathVariable("id") String assetId,
			@RequestHeader Map<String, String> requestHeader);

	@PostMapping("${edc.managementpath.apiversion.asset:/v3}/assets")
	String createAsset(URI url, @RequestBody AssetEntryRequest requestBody,
			@RequestHeader Map<String, String> requestHeader);

	@PutMapping("${edc.managementpath.apiversion.asset:/v3}/assets")
	String updateAsset(URI url, @RequestBody AssetEntryRequest requestBody,
			@RequestHeader Map<String, String> requestHeader);

	@PostMapping("${edc.managementpath.apiversion.asset:/v3}/assets/request")
	JsonNode getAssetByFilterExpression(URI url, @RequestBody ObjectNode requestBody,
			@RequestHeader Map<String, String> requestHeader);

	@DeleteMapping(path = "${edc.managementpath.apiversion.asset:/v3}/assets/{id}")
	ResponseEntity<Object> deleteAssets(URI url, @PathVariable("id") String assetsId,
			@RequestHeader Map<String, String> requestHeader);

	// Policy
	@GetMapping("${edc.managementpath.apiversion:/v3}/policydefinitions/{id}")
	JsonNode getPolicy(URI url, @PathVariable("id") String policyId,
			@RequestHeader Map<String, String> requestHeader);

	@PostMapping("${edc.managementpath.apiversion:/v3}/policydefinitions")
	JsonNode createPolicy(URI url, @RequestBody JsonNode requestBody,
			@RequestHeader Map<String, String> requestHeader);

	@PutMapping("${edc.managementpath.apiversion:/v3}/policydefinitions/{id}")
	JsonNode updatePolicy(URI url, @PathVariable("id") String policyUUId, @RequestBody JsonNode requestBody,
			@RequestHeader Map<String, String> requestHeader);

	@DeleteMapping(path = "${edc.managementpath.apiversion:/v3}/policydefinitions/{id}")
	ResponseEntity<Object> deletePolicyDefinitions(URI url, @PathVariable("id") String policydefinitionsId,
			@RequestHeader Map<String, String> requestHeader);

	// Contract Definition
	@PostMapping("${edc.managementpath.apiversion:/v3}/contractdefinitions")
	String createContractDefination(URI url, @RequestBody ContractDefinitionRequest requestBody,
			@RequestHeader Map<String, String> requestHeader);

	@PutMapping("${edc.managementpath.apiversion:/v3}/contractdefinitions")
	JsonNode updateContractDefination(URI url, @RequestBody ContractDefinitionRequest requestBody,
			@RequestHeader Map<String, String> requestHeader);

	@GetMapping("${edc.managementpath.apiversion:/v3}/contractdefinitions/{id}")
	JsonNode getContractDefination(URI url, @PathVariable("id") String id,
			@RequestHeader Map<String, String> requestHeader);

	@PostMapping("${edc.managementpath.apiversion:/v3}/contractdefinitions/request")
	JsonNode getContractDefinitionsByFilterExpression(URI url, @RequestBody ObjectNode requestBody,
			@RequestHeader Map<String, String> requestHeader);

	@DeleteMapping(path = "${edc.managementpath.apiversion:/v3}/contractdefinitions/{id}")
	ResponseEntity<Object> deleteContractDefinition(URI url, @PathVariable("id") String contractDefinitionId,
			@RequestHeader Map<String, String> requestHeader);

	// Business Partner Group
	@GetMapping("/business-partner-groups/{bpn}")
	JsonNode getBusinessPartnerGroups(URI url, @PathVariable("bpn") String bpn,
			@RequestHeader Map<String, String> requestHeader);

	@PutMapping("/business-partner-groups")
	void updateBusinessPartnerGroups(URI url, @RequestBody BusinessPartnerGroupRequest requestBody,
			@RequestHeader Map<String, String> requestHeader);

	@PostMapping("/business-partner-groups")
	void createBusinessPartnerGroups(URI url, @RequestBody BusinessPartnerGroupRequest requestBody,
			@RequestHeader Map<String, String> requestHeader);

	@DeleteMapping("/business-partner-groups/{bpn}")
	void deleteBusinessPartnerGroups(URI url, @PathVariable("bpn") String bpn,
			@RequestHeader Map<String, String> requestHeader);

}
