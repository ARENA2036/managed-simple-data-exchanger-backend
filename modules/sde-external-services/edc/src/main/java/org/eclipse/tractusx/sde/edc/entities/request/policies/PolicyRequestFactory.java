/********************************************************************************
 * Copyright (c) 2022 BMW GmbH
 * Copyright (c) 2022,2024 T-Systems International GmbH
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
import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.eclipse.tractusx.sde.edc.constants.EDCAssetConfigurableConstant;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Service
@RequiredArgsConstructor
public class PolicyRequestFactory {

	private final EDCAssetConfigurableConstant edcAssetConfigurableConstant;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public PolicyDefinitionRequest getPolicy(String policyId, String assetId, List<ActionRequest> action, String type) {

        List<PermissionRequest> permissions = getPermissions(assetId, action);

        Map<String,String> contextMap = Map.of(
                // "@vocab", "https://w3id.org/edc/v0.0.1/ns/",
                "edc", "https://w3id.org/edc/v0.0.1/ns/",
                "tx", "https://w3id.org/tractusx/v0.0.1/ns/",
                "odrl", "http://www.w3.org/ns/odrl/2/",
                "cx-policy", "https://w3id.org/catenax/policy/"
        );
        PolicyRequest policyRequest = PolicyRequest.builder()
                .type("odrl:Set")
                .permission(permissions)
                .obligations(new ArrayList<>())
                .prohibitions(new ArrayList<>())
                .target(Map.of("@id", assetId))
                .assigner(Map.of("@id", edcAssetConfigurableConstant.getBpdmProviderBpnl()))
                .build();

        policyId = getGeneratedPolicyId(policyId, type);

        PolicyDefinitionRequest policyDefinitionRequest = PolicyDefinitionRequest.builder()
                .id(policyId)
                .context(contextMap)
                .policyRequest(policyRequest)
                .build();

        System.out.println("$$$");
        System.out.println(policyRequest.toJsonString());
        System.out.println(policyDefinitionRequest.toJsonString());
        System.out.println(policyRequest);
        System.out.println(policyDefinitionRequest);

        return policyDefinitionRequest;
    }

	@SneakyThrows
	public PolicyDefinitionRequest setPolicyIdAndGetObject(String assetId, JsonNode jsonNode, String type) {
		
		JsonNode contentPolicy= ((ObjectNode) jsonNode).get("content");
		
		((ObjectNode) contentPolicy).remove("@id");
		
		//Use submodel id to generate unique policy id for asset use policy type as prefix asset/usage
		String policyId = getGeneratedPolicyId(assetId, type);

        List<Object> contextList = List.of(
                Map.of("@vocab", "https://w3id.org/edc/v0.0.1/ns/")
        );

		
		return PolicyDefinitionRequest.builder()
				.id(policyId)
				.context(contextList)
				.policyRequest(contentPolicy)
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
	

	public List<PermissionRequest> getPermissions(String assetId, List<ActionRequest> actions) {

		ArrayList<PermissionRequest> permission = new ArrayList<>();
		if (actions != null) {
			actions.forEach(action -> {
                Map<String, Object> logicalGroup = action.getAction();
				PermissionRequest permissionRequest = PermissionRequest
						.builder()
                        .action(Map.of("@id", "odrl:use"))
						.constraint(logicalGroup)
						.build();
				permission.add(permissionRequest);
			});
		}
		return permission;
	}
}
