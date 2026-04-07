/********************************************************************************
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

package org.eclipse.tractusx.sde.edc.mapper;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.tractusx.sde.common.configuration.properties.EDCVersionConfigurationProperties;
import org.eclipse.tractusx.sde.edc.constants.EDCAssetConfigurableConstant;
import org.eclipse.tractusx.sde.edc.entities.request.policies.ActionRequest;
import org.eclipse.tractusx.sde.edc.entities.request.policies.PermissionRequest;
import org.eclipse.tractusx.sde.edc.entities.request.policies.PolicyRequest;
import org.eclipse.tractusx.sde.edc.entities.request.policies.PolicyRequestFactory;
import org.eclipse.tractusx.sde.edc.model.contractnegotiation.ContractNegotiations;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import static org.eclipse.tractusx.sde.edc.entities.request.policies.PolicyConstraintBuilderService.USAGE_POLICY_TYPE;

@Component
@RequiredArgsConstructor
public class ContractMapper {

	private final PolicyRequestFactory policyRequestFactory;
    private final EDCAssetConfigurableConstant edcAssetConfigurableConstant;
    private final EDCVersionConfigurationProperties edcVersion;

	@SneakyThrows
	public ContractNegotiations prepareContractNegotiations(String providerProtocolUrl, String offerId, String assetId,
			String provider, List<ActionRequest> action) {

		String defaultProvider = edcAssetConfigurableConstant.getManufacturerId();
		provider = (provider == null || provider.isEmpty()) ? defaultProvider : provider;

		PolicyRequest policy = preparePolicy(assetId, offerId, provider, action);

		return ContractNegotiations.builder()
				.context(createContext())
				.connectorAddress(edcVersion.getMinor() >= 11 ? providerProtocolUrl + "/2025-1": providerProtocolUrl)
				.protocol(edcVersion.getMinor() >= 11 ?"dataspace-protocol-http:2025-1" : "dataspace-protocol-http")
				.policy(policy)
				.build();

	}

	private @NonNull Object createContext() {
		if(edcVersion.getMinor() >= 11 ){
			return List.of(
					"http://www.w3.org/ns/odrl.jsonld",
					"https://w3id.org/catenax/2025/9/policy/context.jsonld",
					Map.of("@vocab", "https://w3id.org/edc/v0.0.1/ns/")
			);
		}
		return Map.of(
					"odrl", "http://www.w3.org/ns/odrl/2/",
					"cx-policy", "https://w3id.org/catenax/policy/",
					"edc", "https://w3id.org/edc/v0.0.1/ns/");
	}

	private PolicyRequest preparePolicy(String assetId, String offerId, String provider, List<ActionRequest> action) {
		List<PermissionRequest> permissions = policyRequestFactory.getPermissions(action, USAGE_POLICY_TYPE);

		return PolicyRequest.builder()
				.useNameSpacePrefix(edcVersion.getMinor() >= 11)
				.id(offerId)
				.type(edcVersion.getMinor() >= 11 ? "Offer" : "odrl:Offer")
				.target(assetId)
				.assigner(provider)
				.permission(permissions)
				.prohibition(new ArrayList<>())
				.obligation(new ArrayList<>())
				.build();
	}

}