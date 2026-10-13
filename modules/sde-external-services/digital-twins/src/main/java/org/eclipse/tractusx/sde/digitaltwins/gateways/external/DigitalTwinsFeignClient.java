/********************************************************************************
 * Copyright (c) 2022,2024 T-Systems International GmbH
 * Copyright (c) 2026 ARENA2036 e.V.
 * Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.sde.digitaltwins.gateways.external;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.eclipse.tractusx.sde.common.model.KeycloakJWTTokenResponse;
import org.eclipse.tractusx.sde.digitaltwins.entities.request.CreateSubModelRequest;
import org.eclipse.tractusx.sde.digitaltwins.entities.request.ShellDescriptorRequest;
import org.eclipse.tractusx.sde.digitaltwins.entities.response.ShellDescriptorResponse;
import org.eclipse.tractusx.sde.digitaltwins.entities.response.ShellLookupResponse;
import org.eclipse.tractusx.sde.digitaltwins.entities.response.SubModelListResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "DigitalTwinsFeignClient", url = "placeholder")
public interface DigitalTwinsFeignClient {

	@PostMapping(consumes = MediaType.APPLICATION_FORM_URLENCODED_VALUE)
	KeycloakJWTTokenResponse readAuthToken(URI url,
										   @RequestParam("grant_type") String grantType,
										   @RequestParam("client_id") String clientId,
										   @RequestParam("client_secret") String clientSecret);



	@PostMapping(path = "${digital-twins.registry.uri:/api/v3}/shell-descriptors")
	ResponseEntity<ShellDescriptorResponse> createShellDescriptor(URI url, @RequestBody ShellDescriptorRequest request,
			@RequestHeader Map<String, String> requestHeader);

	@GetMapping(path = "${digital-twins.registry.uri:/api/v3}/shell-descriptors/{aasIdentifier}")
	ResponseEntity<ShellDescriptorResponse> getShellDescriptorByShellId(URI url, @PathVariable("aasIdentifier") String shellId,
			@RequestHeader("Edc-Bpn") String edcBpn,
			@RequestHeader Map<String, String> requestHeader);

	@PutMapping(path = "${digital-twins.registry.uri:/api/v3}/shell-descriptors/{aasIdentifier}")
	ResponseEntity<Void> updateShellDescriptorByShellId(URI url, @PathVariable("aasIdentifier") String shellId,
			@RequestHeader("Edc-Bpn") String edcBpn, @RequestBody ShellDescriptorRequest request,
			@RequestHeader Map<String, String> requestHeader);

	@DeleteMapping(path = "${digital-twins.registry.uri:/api/v3}/shell-descriptors/{aasIdentifier}")
	ResponseEntity<Void> deleteShell(URI url, @PathVariable("assetIds") String shellId,
			@RequestHeader Map<String, String> requestHeader);

	@PostMapping(path = "${digital-twins.registry.uri:/api/v3}/shell-descriptors/{aasIdentifier}/submodel-descriptors")
	ResponseEntity<String> createSubModel(URI url, @PathVariable("aasIdentifier") String shellId,
			@RequestBody CreateSubModelRequest request, @RequestHeader("Edc-Bpn") String edcBpn,
			@RequestHeader Map<String, String> requestHeader);
	
	@PutMapping(path = "${digital-twins.registry.uri:/api/v3}/shell-descriptors/{aasIdentifier}/submodel-descriptors/{submodelIdentifier}")
	ResponseEntity<String> updateSubModel(URI url, @PathVariable("aasIdentifier") String shellId,
			@PathVariable("submodelIdentifier") String submodelIdentifier, @RequestBody CreateSubModelRequest request,
			@RequestHeader("Edc-Bpn") String edcBpn,
			@RequestHeader Map<String, String> requestHeader);

	@GetMapping(path = "${digital-twins.registry.uri:/api/v3}/shell-descriptors/{aasIdentifier}/submodel-descriptors")
	ResponseEntity<SubModelListResponse> getSubModels(URI url, @PathVariable("aasIdentifier") String shellId,
			@RequestHeader("Edc-Bpn") String edcBpn,
			@RequestHeader Map<String, String> requestHeader);

	@DeleteMapping(path = "${digital-twins.registry.uri:/api/v3}/shell-descriptors/{aasIdentifier}/submodel-descriptors/{submodelIdentifier}")
	ResponseEntity<Object> deleteSubmodelfromShellById(URI url, @PathVariable("aasIdentifier") String shellId,
			@PathVariable("submodelIdentifier") String submodelIdentifier,
			@RequestHeader Map<String, String> requestHeader);

	@GetMapping(path = "${digital-twins.registry.lookup.uri:/api/v3}/lookup/shells")
	ResponseEntity<ShellLookupResponse> shellLookup(URI url, @RequestParam("assetIds") List<String> assetIds,
			@RequestHeader("Edc-Bpn") String edcBpn,
			@RequestHeader Map<String, String> requestHeader);
	
	@PostMapping(path = "${digital-twins.registry.lookup.uri:/api/v3}/lookup/shells/{shellId}")
	ResponseEntity<List<Object>> createShellSpecificAttributes(URI url, @PathVariable("shellId") String shellId,
			@RequestHeader("Edc-Bpn") String edcBpn, @RequestBody List<Object> specificAssetIds,
			@RequestHeader Map<String, String> requestHeader);

	@DeleteMapping(path = "${digital-twins.registry.lookup.uri:/api/v3}/lookup/shells/{shellId}")
	ResponseEntity<Object> deleteShellSpecificAttributes(URI url, @PathVariable("assetIds") String shellId,
			@RequestHeader("Edc-Bpn") String edcBpn,
			@RequestHeader Map<String, String> requestHeader);

}