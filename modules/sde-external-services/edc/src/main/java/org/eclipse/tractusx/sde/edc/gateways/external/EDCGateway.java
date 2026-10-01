/********************************************************************************
 * Copyright (c) 2022 BMW GmbH
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

package org.eclipse.tractusx.sde.edc.gateways.external;

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.apache.commons.lang3.StringUtils;
import org.eclipse.tractusx.sde.edc.api.EDCFeignClientApi;
import org.eclipse.tractusx.sde.edc.entities.request.asset.AssetEntryRequest;
import org.eclipse.tractusx.sde.edc.entities.request.businesspartnergroup.BusinessPartnerGroupRequest;
import org.eclipse.tractusx.sde.edc.entities.request.contractdefinition.ContractDefinitionRequest;
import org.eclipse.tractusx.sde.edc.exceptions.EDCGatewayException;
import org.eclipse.tractusx.sde.edc.facilitator.AbstractEDCStepsHelper;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;

import feign.FeignException;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class EDCGateway extends AbstractEDCStepsHelper {

	private final EDCFeignClientApi edcFeignClientApi;
	
	public boolean assetExistsLookup(String id) {
		try {
			edcFeignClientApi.getAsset(providerManagementUri(), id, getProviderAuthHeader());
		} catch (FeignException e) {
			if (e.status() == HttpStatus.NOT_FOUND.value()) {
				return false;
			}
			throw e;
		}
		return true;
	}
	
	public boolean containsAssetsByFilterExpression(ObjectNode requestBody) {
		try {
			JsonNode result = edcFeignClientApi.getAssetByFilterExpression(providerManagementUri(), requestBody, getProviderAuthHeader());
			if (result.isArray() && result.isEmpty())
				return false;
		} catch (FeignException e) {
			if (e.status() == HttpStatus.NOT_FOUND.value()) {
				return false;
			}
			throw e;
		}
		return true;
	}
	
	public JsonNode getAssetsByFilterExpression(ObjectNode requestBody) {
		try {
			return edcFeignClientApi.getAssetByFilterExpression(providerManagementUri(), requestBody, getProviderAuthHeader());
		} catch (FeignException e) {
			if (e.status() == HttpStatus.NOT_FOUND.value()) {
				return null;
			}
			throw e;
		}
	}

	public JsonNode getContractDefinitionsByFilterExpression(ObjectNode requestBody) {
		try {
			return edcFeignClientApi.getContractDefinitionsByFilterExpression(providerManagementUri(), requestBody, getProviderAuthHeader());
		} catch (FeignException e) {
			if (e.status() == HttpStatus.NOT_FOUND.value()) {
				return null;
			}
			throw e;
		}
	}

	public String createAsset(AssetEntryRequest request) {
		try {
			return edcFeignClientApi.createAsset(providerManagementUri(), request, getProviderAuthHeader());
		} catch (FeignException e) {
			if (e.status() == HttpStatus.CONFLICT.value()) {
				throw new EDCGatewayException("Asset already exists");
			}
			throw new EDCGatewayException(e.getMessage());
		}
	}
	
	public void updateAsset(AssetEntryRequest request) {
		try {
			edcFeignClientApi.updateAsset(providerManagementUri(), request, getProviderAuthHeader());
		} catch (FeignException e) {
			if (e.status() == HttpStatus.NOT_FOUND.value()) {
				throw new EDCGatewayException("Asset to update doesn't exists");
			}
			throw new EDCGatewayException(e.getMessage());
		}
	}

	
	public boolean policyExistsLookup(String policyId) {
		try {
			edcFeignClientApi.getPolicy(providerManagementUri(), policyId, getProviderAuthHeader());
		} catch (FeignException e) {
			if (e.status() == HttpStatus.NOT_FOUND.value()) {
				return false;
			}
			throw e;
		}
		return true;
	}
	
	@SneakyThrows
    public JsonNode createPolicyDefinition(JsonNode request) {
        try {
            return edcFeignClientApi.createPolicy(providerManagementUri(), request, getProviderAuthHeader());
        } catch (FeignException fe) {
            log.error("Exception Request: {}", fe.request());
            log.error("Exception Status: {}", fe.status());
            log.error("Exception Message: {}", fe.getMessage());

            if (fe.responseBody().isPresent()) {
                try {
                    String body = StandardCharsets.UTF_8.decode(fe.responseBody().get()).toString();
//                    log.error("Full EDC Response Body:\n{}", body);
                } catch (Exception ex) {
                    log.error("Failed to read response body", ex);
                }
            } else {
                log.error("No response body from EDC");
            }

            throw new EDCGatewayException("EDC error: " + fe.getMessage());
        }
    }
	
	@SneakyThrows
	public JsonNode updatePolicyDefinition(String policyUUId, JsonNode request) {
		try {
			return edcFeignClientApi.updatePolicy(providerManagementUri(), policyUUId, request, getProviderAuthHeader());
		} catch (FeignException e) {
			throw new EDCGatewayException(e.getMessage());
		}
	}

	
	public boolean contractDefinitionExistsLookup(String contractDefinitionId) {
		try {
			edcFeignClientApi.getContractDefination(providerManagementUri(), contractDefinitionId, getProviderAuthHeader());
		} catch (FeignException e) {
			if (e.status() == HttpStatus.NOT_FOUND.value()) {
				return false;
			}
			throw e;
		}
		return true;
	}
	
	public String createContractDefinition(ContractDefinitionRequest request) {
		try {
			return edcFeignClientApi.createContractDefination(providerManagementUri(), request, getProviderAuthHeader());
		} catch (FeignException e) {
			throw new EDCGatewayException(e.getMessage());
		}
	}
	
	public void updateContractDefinition(ContractDefinitionRequest request) {
		try {
			edcFeignClientApi.updateContractDefination(providerManagementUri(), request, getProviderAuthHeader());
		} catch (FeignException e) {
			throw new EDCGatewayException(e.getMessage());
		}
	}
	
	public void addBPNintoPCFBusinessPartnerGroup(String bpnNumber, String groupName) {
		
		try {
			if (StringUtils.isNotBlank(bpnNumber)) {
				edcFeignClientApi.getBusinessPartnerGroups(providerManagementUri(), bpnNumber, getProviderAuthHeader());
			}else {
				log.info("BPN found empty not adding in business partner group {}", groupName);
			}
		} catch (FeignException e) {
			log.info("BPN not exist in group so adding business partner group {}, {}", groupName, e.getMessage());
			if (e.status() == HttpStatus.NOT_FOUND.value()) {
				edcFeignClientApi.createBusinessPartnerGroups(providerManagementUri(),
						BusinessPartnerGroupRequest.builder().id(bpnNumber).groups(List.of(groupName)).build(),
						getProviderAuthHeader());
				
			}
		}
	}

	public void deleteBPNfromPCFBusinessPartnerGroup(String bpnNumber, String groupName) {
		try {
			if (StringUtils.isNotBlank(bpnNumber)) {
				edcFeignClientApi.deleteBusinessPartnerGroups(providerManagementUri(), bpnNumber, getProviderAuthHeader());
			}else {
				log.info("BPN found empty not going to delete from business partner group {}", groupName);
			}
		} catch (FeignException e) {
			log.error("Unable to perform delete bpn {} from business partner group {}, {}", bpnNumber, groupName, e.getMessage());
		}
	}
}