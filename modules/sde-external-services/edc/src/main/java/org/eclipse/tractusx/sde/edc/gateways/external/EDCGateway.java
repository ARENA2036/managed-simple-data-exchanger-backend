/********************************************************************************
 * Copyright (c) 2022 BMW GmbH
 * Copyright (c) 2022,2024 T-Systems International GmbH
 * Copyright (c) 2026 ARENA2036 e.V.
 * Copyright (c) 2022,2024,2026 Contributors to the Eclipse Foundation
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

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.tractusx.sde.edc.api.EDCFeignClientApi;
import org.eclipse.tractusx.sde.edc.entities.request.asset.AssetEntryRequest;
import org.eclipse.tractusx.sde.edc.entities.request.businesspartnergroup.BusinessPartnerGroupRequest;
import org.eclipse.tractusx.sde.edc.entities.request.contractdefinition.ContractDefinitionRequest;
import org.eclipse.tractusx.sde.edc.exceptions.EDCGatewayException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
public class EDCGateway {

	private final EDCFeignClientApi edcFeignClientApi;
	private final ObjectMapper objectMapper;
	
	public boolean assetExistsLookup(String id) {
		try {
			edcFeignClientApi.getAsset(id);
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
			JsonNode result = edcFeignClientApi.getAssetByFilterExpression(requestBody);
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
			log.info("=== ASSET FILTER REQUEST ===");
			log.info(objectMapper.writerWithDefaultPrettyPrinter()
					.writeValueAsString(requestBody));

			JsonNode result =
					edcFeignClientApi.getAssetByFilterExpression(requestBody);

			log.info("=== ASSET FILTER RESPONSE ===");
			log.info(objectMapper.writerWithDefaultPrettyPrinter()
					.writeValueAsString(result));
		} catch (FeignException e) {
			if (e.status() == HttpStatus.NOT_FOUND.value()) {
				return null;
			}
			throw e;
		} catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
        return null;
    }

	public JsonNode getContractDefinitionsByFilterExpression(ObjectNode requestBody) {
		try {
			return edcFeignClientApi.getContractDefinitionsByFilterExpression(requestBody);
		} catch (FeignException e) {
			if (e.status() == HttpStatus.NOT_FOUND.value()) {
				return null;
			}
			throw e;
		}
	}

	public String createAsset(AssetEntryRequest request) {
		try {

			log.info("=== EDC ASSET CREATE REQUEST ===");
			log.info("Asset Request: {}", objectMapper.writeValueAsString(request));

			String result = edcFeignClientApi.createAsset(request);

			log.info("=== EDC ASSET CREATE RESPONSE ===");
			log.info("Asset Created: {}", result);

			try {
				ResponseEntity<Object> createdAsset = edcFeignClientApi.getAsset(result);
				log.info("=== EDC ASSET READ BACK ===");
				log.info(objectMapper.writerWithDefaultPrettyPrinter()
						.writeValueAsString(createdAsset));
			} catch (Exception ex) {
				log.warn("Unable to read asset back {}", result);
			}

			return result;

		} catch (FeignException e) {
			if (e.status() == HttpStatus.CONFLICT.value()) {
				throw new EDCGatewayException("Asset already exists");
			}
			throw new EDCGatewayException(e.getMessage());
		} catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }
	
	public void updateAsset(AssetEntryRequest request) {
		try {
			edcFeignClientApi.updateAsset(request);
		} catch (FeignException e) {
			if (e.status() == HttpStatus.NOT_FOUND.value()) {
				throw new EDCGatewayException("Asset to update doesn't exists");
			}
			throw new EDCGatewayException(e.getMessage());
		}
	}

	
	public boolean policyExistsLookup(String policyId) {
		try {
			edcFeignClientApi.getPolicy(policyId);
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
		log.info("=== EDC POLICY CREATE REQUEST ===");
		log.info(objectMapper.writerWithDefaultPrettyPrinter()
				.writeValueAsString(request));
        try {

			JsonNode response = edcFeignClientApi.createPolicy(request);

			log.info("=== EDC POLICY CREATE RESPONSE ===");
			log.info(objectMapper.writerWithDefaultPrettyPrinter()
					.writeValueAsString(response));

			return response;
        } catch (FeignException fe) {
            log.error("Exception Request: {}", fe.request());
            log.error("Exception Status: {}", fe.status());
            log.error("Exception Message: {}", fe.getMessage());

            if (fe.responseBody().isPresent()) {
                try {
                    String body = StandardCharsets.UTF_8.decode(fe.responseBody().get()).toString();
                    log.error("Full EDC Response Body:\n{}", body);
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
			return edcFeignClientApi.updatePolicy(policyUUId, request);
		} catch (FeignException e) {
			throw new EDCGatewayException(e.getMessage());
		}
	}

	
	public boolean contractDefinitionExistsLookup(String contractDefinitionId) {
		try {
			edcFeignClientApi.getContractDefination(contractDefinitionId);
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

			log.info("=== EDC CONTRACT CREATE REQUEST ===");
			log.info(objectMapper.writeValueAsString(request));

			String result =
					edcFeignClientApi.createContractDefination(request);

			log.info("=== EDC CONTRACT CREATE RESPONSE ===");
			log.info("Contract Definition Created: {}", result);

			return result;

		} catch (FeignException | JsonProcessingException e) {
			throw new EDCGatewayException(e.getMessage());
		}
	}
	
	public void updateContractDefinition(ContractDefinitionRequest request) {
		try {
			edcFeignClientApi.updateContractDefination(request);
		} catch (FeignException e) {
			throw new EDCGatewayException(e.getMessage());
		}
	}
	
	public void addBPNintoPCFBusinessPartnerGroup(String bpnNumber, String groupName) {
		
		try {
			if (StringUtils.isNotBlank(bpnNumber)) {
				edcFeignClientApi.getBusinessPartnerGroups(bpnNumber);
			}else {
				log.info("BPN found empty not adding in business partner group {}", groupName);
			}
		} catch (FeignException e) {
			log.info("BPN not exist in group so adding business partner group {}, {}", groupName, e.getMessage());
			if (e.status() == HttpStatus.NOT_FOUND.value()) {
				edcFeignClientApi.createBusinessPartnerGroups(
						BusinessPartnerGroupRequest.builder().id(bpnNumber).groups(List.of(groupName)).build());
				
			}
		}
	}

	public void deleteBPNfromPCFBusinessPartnerGroup(String bpnNumber, String groupName) {
		try {
			if (StringUtils.isNotBlank(bpnNumber)) {
				edcFeignClientApi.deleteBusinessPartnerGroups(bpnNumber);
			}else {
				log.info("BPN found empty not going to delete from business partner group {}", groupName);
			}
		} catch (FeignException e) {
			log.error("Unable to perform delete bpn {} from business partner group {}, {}", bpnNumber, groupName, e.getMessage());
		}
	}
}