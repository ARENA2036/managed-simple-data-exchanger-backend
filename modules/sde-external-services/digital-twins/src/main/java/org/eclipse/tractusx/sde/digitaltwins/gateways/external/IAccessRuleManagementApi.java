/********************************************************************************
 * Copyright (c) 2024 T-Systems International GmbH
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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
import java.util.Map;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;

import com.fasterxml.jackson.databind.JsonNode;

@FeignClient(value = "IAccessRuleManagementApi", url = "placeholder")
public interface IAccessRuleManagementApi {

	@GetMapping(path = "${digital-twins.registry.uri:/api/v3}/access-controls/rules")
	public JsonNode getAccessControlsRules(URI url, @RequestHeader("Edc-Bpn") String edcBpn,
			@RequestHeader Map<String, String> requestHeader);

	@PostMapping(path = "${digital-twins.registry.uri:/api/v3}/access-controls/rules")
	public JsonNode createAccessControlsRule(URI url, @RequestHeader("Edc-Bpn") String edcBpn, @RequestBody JsonNode request,
			@RequestHeader Map<String, String> requestHeader);

	@GetMapping(path = "${digital-twins.registry.uri:/api/v3}/access-controls/rules/{ruleId}")
	public JsonNode getAccessControlsRuleById(URI url, @PathVariable("ruleId") String ruleId,
			@RequestHeader("Edc-Bpn") String edcBpn,
			@RequestHeader Map<String, String> requestHeader);

	@PutMapping(path = "${digital-twins.registry.uri:/api/v3}/access-controls/rules/{ruleId}")
	public JsonNode updateAccessControlsRule(URI url, @PathVariable("ruleId") String ruleId,
			@RequestHeader("Edc-Bpn") String edcBpn, @RequestBody JsonNode request,
			@RequestHeader Map<String, String> requestHeader);

	@DeleteMapping(path = "${digital-twins.registry.uri:/api/v3}/access-controls/rules/{ruleId}")
	public void deleteAccessControlsRule(URI url, @PathVariable("ruleId") String ruleId,
			@RequestHeader("Edc-Bpn") String edcBpn,
			@RequestHeader Map<String, String> requestHeader);

}