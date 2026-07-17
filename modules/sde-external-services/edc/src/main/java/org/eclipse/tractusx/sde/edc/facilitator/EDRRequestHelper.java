/********************************************************************************
 * Copyright (c) 2023,2024 T-Systems International GmbH
 * Copyright (c) 2026 ARENA2036 e.V.
 * Copyright (c) 2023,2024 Contributors to the Eclipse Foundation
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

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import org.eclipse.tractusx.sde.common.configuration.properties.EDRConfigurationProperties;
import org.eclipse.tractusx.sde.common.utils.LogUtil;
import org.eclipse.tractusx.sde.edc.api.EDRApiProxy;
import org.eclipse.tractusx.sde.edc.entities.request.policies.ActionRequest;
import org.eclipse.tractusx.sde.edc.mapper.ContractMapper;
import org.eclipse.tractusx.sde.edc.model.contractnegotiation.AcknowledgementId;
import org.eclipse.tractusx.sde.edc.model.contractnegotiation.ContractNegotiations;
import org.eclipse.tractusx.sde.edc.model.edr.EDRCachedByIdResponse;
import org.eclipse.tractusx.sde.edc.model.edr.EDRCachedResponse;
import org.eclipse.tractusx.sde.edc.model.request.Offer;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;

@Service
@RequiredArgsConstructor
@Slf4j
public class EDRRequestHelper extends AbstractEDCStepsHelper {

	private final EDRConfigurationProperties edrProperties;
	@Value("${edr.refresh.auto:true}")
	private boolean edrAutoRefresh;
	private final EDRApiProxy edrApiProxy;
	private final ContractMapper contractMapper;
	private final ObjectMapper mapper = new ObjectMapper();

	@SneakyThrows
	public String edrRequestInitiate(String providerUrl, String providerId, Offer offer, String assetId,
			List<ActionRequest> action, Map<String, String> extensibleProperty) {

		List<Object> context = List.of(
				"http://www.w3.org/ns/odrl.jsonld",
				"https://w3id.org/catenax/2025/9/policy/context.jsonld",
				Map.of("@vocab", "https://w3id.org/edc/v0.0.1/ns/")
		);
		
		String offerId = offer.getOfferId();
		ContractNegotiations contractNegotiations = contractMapper.prepareContractNegotiations(providerUrl, offerId,
				assetId, providerId, action);
		
		contractNegotiations.setContext(context);
		
		log.debug(LogUtil.encode(contractNegotiations.toJsonString()));
		
		if (Optional.ofNullable(offer.getHasPolicy()).isPresent()) {
			JsonNode hasPolicy = offer.getHasPolicy();
//			((ObjectNode) hasPolicy).putPOJO("odrl:assigner", Map.of("@id", providerId));
//			((ObjectNode) hasPolicy).putPOJO("odrl:target", Map.of("@id", assetId));
			((ObjectNode) hasPolicy).remove("odrl:assigner");
			((ObjectNode) hasPolicy).remove("odrl:target");
			((ObjectNode) hasPolicy).putPOJO("assigner", providerId);
			((ObjectNode) hasPolicy).putPOJO("target", assetId);
			contractNegotiations.setPolicy(offer.getHasPolicy());
		}
		
		JsonNode jsonNode = mapper.convertValue(contractNegotiations, JsonNode.class);
		log.debug(LogUtil.encode(jsonNode.toPrettyString()));
		
		AcknowledgementId acknowledgementId = edrApiProxy.edrCacheCreate(new URI(consumerHostWithDataPath), jsonNode,
				getAuthHeader());
		return acknowledgementId.getId();
	}

	@SneakyThrows
	public List<EDRCachedResponse> getEDRCachedByAsset(String assetId) {
		String requestbody = """
				{
				    "@context": {
				        "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
				    },
				    "@type": "QuerySpec",
				    "offset": 0,
				    "limit": 10,
				    "sortOrder": "DESC",
				    "sortField": "assetId",
				    "filterExpression": [
				        {
				            "operandLeft": "assetId",
				            "operator": "=",
				            "operandRight": "%s"
				        }
				    ]
				}
				""";
		JsonNode requestBody = new ObjectMapper().readTree(String.format(requestbody, assetId));

		return edrApiProxy.getEDRCachedByAsset(new URI(consumerHostWithDataPath), requestBody, getAuthHeader());
	}

	@SneakyThrows
	public List<EDRCachedResponse> getEDRCachedByContractNegotiationId(String assetId) {
		String requestbody = """
				{
				    "@context": {
				        "@vocab": "https://w3id.org/edc/v0.0.1/ns/"
				    },
				    "@type": "QuerySpec",
				    "offset": 0,
				    "limit": 10,
				    "sortOrder": "DESC",
				    "sortField": "createdAt",
				    "filterExpression": [
				        {
				            "operandLeft": "contractNegotiationId",
				            "operator": "=",
				            "operandRight": "%s"
				        }
				    ]
				}
				""";
		JsonNode requestBody = new ObjectMapper().readTree(String.format(requestbody, assetId));

		return edrApiProxy.getEDRCachedByAsset(new URI(consumerHostWithDataPath), requestBody, getAuthHeader());
	}

	@SneakyThrows
	public EDRCachedByIdResponse getEDRCachedByTransferProcessId(String transferProcessId) {
		return edrApiProxy.getEDRCachedByTransferProcessId(new URI(consumerHostWithDataPath), transferProcessId, edrProperties.getRefresh().getAuto(),
				getAuthHeader());
	}

	@SneakyThrows
	public Object getDataFromProvider(EDRCachedByIdResponse authorizationToken, String endpoint) {
		Map<String, String> authHeader = new HashMap<>();
		authHeader.put("authorization", authorizationToken.getAuthorization());
		return edrApiProxy.getActualDataFromProviderDataPlane(new URI(endpoint), authHeader);
	}

}