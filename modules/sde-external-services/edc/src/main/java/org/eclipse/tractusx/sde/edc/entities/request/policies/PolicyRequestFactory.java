/********************************************************************************
 * Copyright (c) 2022 BMW GmbH
 * Copyright (c) 2022,2024 T-Systems International GmbH
 * Copyright (c) 2025 ARENA2036 e.V.
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

package org.eclipse.tractusx.sde.edc.entities.request.policies;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.sde.common.configuration.properties.EDCVersionConfigurationProperties;
import org.eclipse.tractusx.sde.common.mapper.JsonObjectMapper;
import org.eclipse.tractusx.sde.edc.constants.EDCAssetConfigurableConstant;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import static org.eclipse.tractusx.sde.edc.entities.request.policies.PolicyConstraintBuilderService.ACCESS_POLICY_TYPE;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyRequestFactory {

    private final EDCAssetConfigurableConstant edcAssetConfigurableConstant;
    private final EDCVersionConfigurationProperties edcVersion;
	private final JsonObjectMapper jsonObjectMapper;

	public PolicyDefinitionRequest getPolicy(String policyId, String assetId, List<ActionRequest> action, String type) {

        List<PermissionRequest> permissions = getPermissions(action, type);

		//saturn context
		Object contextList = List.of(
				"https://w3id.org/catenax/2025/9/policy/odrl.jsonld",
				"https://w3id.org/catenax/2025/9/policy/context.jsonld",
				Map.of("@vocab", "https://w3id.org/edc/v0.0.1/ns/")
		);
		//jupiter context
        Map<String, String> contextMap = Map.of(
                "edc", "https://w3id.org/edc/v0.0.1/ns/",
                "tx", "https://w3id.org/tractusx/v0.0.1/ns/",
                "odrl", "http://www.w3.org/ns/odrl/2/",
                "cx-policy", "https://w3id.org/catenax/policy/"
        );

		String policyType = edcVersion.getMinor() >= 11 ? "Set" : "odrl:Set";
        PolicyRequest policyRequest = PolicyRequest.builder()
				.useNameSpacePrefix(edcVersion.getMinor() < 11)
                .type(policyType)
                .permission(permissions)
                .obligation(new ArrayList<>())
                .prohibition(new ArrayList<>())
                .target(assetId)
                .assigner(edcAssetConfigurableConstant.getManufacturerId())
                .build();

        log.debug("Created PolicyRequest for manufacturerId {}:\n$$$\nPolicyRequest:\n{}",
                edcAssetConfigurableConstant.getManufacturerId(),
                policyRequest.toJsonString()
        );

        policyId = getGeneratedPolicyId(policyId, type);

        PolicyDefinitionRequest policyDefinitionRequest = PolicyDefinitionRequest.builder()
                .id(policyId)
                .context(edcVersion.getMinor() >= 11 ? contextList : contextMap)
                .policy(policyRequest)
                .build();

        log.debug("Created policyDefinitionRequest for manufacturerId{}:\n$$$\npolicyDefinitionRequest:\n{}",
                edcAssetConfigurableConstant.getManufacturerId(),
                policyDefinitionRequest.toJsonString()
        );

        return policyDefinitionRequest;
    }

	@SneakyThrows
	public PolicyDefinitionRequest setPolicyIdAndGetObject(String assetId, JsonNode jsonNode, String type) {
		
		JsonNode contentPolicy= ((ObjectNode) jsonNode).get("content");
		PolicyRequest policyRequest = jsonObjectMapper.jsonNodeToObject(contentPolicy, PolicyRequest.class);
		
		((ObjectNode) contentPolicy).remove("@id");
		
		//Use submodel id to generate unique policy id for asset use policy type as prefix asset/usage
		String policyId = getGeneratedPolicyId(assetId, type);

        List<Object> contextList = List.of(
                Map.of("@vocab", "https://w3id.org/edc/v0.0.1/ns/")
        );

		
		return PolicyDefinitionRequest.builder()
				.id(policyId)
				.context(contextList)
				.policy(policyRequest)
				.build();
	}
	
	private String getGeneratedPolicyId(String assetId, String type) {
		String submodelId = assetId;
		if (assetId.length() > 45) {
			submodelId = assetId.substring(46);
			submodelId = submodelId.replace("urn:uuid:", "");
		}
		return type + "-" + submodelId;
	}
	

	public List<PermissionRequest> getPermissions(List<ActionRequest> actions, String type) {
		if (actions == null) {
			return Collections.emptyList();
		}
		return actions.stream().map(ActionRequest::getAction)
				.map(logicalGroup -> PermissionRequest
						.builder()
						.action(createActionByType(type))
						.constraint(logicalGroup)
						.build())
				.toList();

	}

	private @NonNull Object createActionByType(String type) {
		if (ACCESS_POLICY_TYPE.equals(type)) {
			return edcVersion.getMinor() >= 11 ? "access" : Map.of("@id", "odrl:access");
		}
		return edcVersion.getMinor() >= 11 ? "use" : Map.of("@id", "odrl:use");
	}
}