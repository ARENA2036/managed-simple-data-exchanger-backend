/********************************************************************************
 * Copyright (c) 2024 T-Systems International GmbH
 * Copyright (c) 2026 ARENA2036 e.V.
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

package org.eclipse.tractusx.sde.edc.services;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.eclipse.tractusx.sde.common.entities.Policies;
import org.eclipse.tractusx.sde.common.entities.PolicyModel;
import org.eclipse.tractusx.sde.edc.api.ContractOfferCatalogApi;
import org.eclipse.tractusx.sde.edc.constants.EDCAssetConstant;
import org.eclipse.tractusx.sde.edc.facilitator.AbstractEDCStepsHelper;
import org.eclipse.tractusx.sde.edc.model.contractoffers.ContractOfferRequestFactory;
import org.eclipse.tractusx.sde.edc.model.response.QueryDataOfferModel;
import org.eclipse.tractusx.sde.edc.util.UtilityFunctions;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class CatalogResponseBuilder extends AbstractEDCStepsHelper {

	private final ContractOfferCatalogApi contractOfferCatalogApiProxy;
	private final ContractOfferRequestFactory contractOfferRequestFactory;

	//TODO Check if the counterPartyId is a DID
	public List<QueryDataOfferModel> queryOnDataOffers(String providerUrl, String counterPartyId, Integer offset, Integer limit,
			String filterExpression) {

		providerUrl = UtilityFunctions.removeLastSlashOfUrl(providerUrl);

		if (!providerUrl.endsWith(protocolPath) && appendProtocolPath)
			providerUrl = providerUrl + protocolPath;

		String sproviderUrl = providerUrl;

		List<QueryDataOfferModel> queryOfferResponse = new ArrayList<>();

		JsonNode contractOfferCatalog = contractOfferCatalogApiProxy.getContractOffersCatalog(
				contractOfferRequestFactory.getContractOfferRequest(sproviderUrl, counterPartyId, limit, offset, filterExpression));

		JsonNode jOffer = getDataset(contractOfferCatalog);
		if (jOffer == null) {
			return queryOfferResponse;
		}

		handleContractOffer(jOffer, sproviderUrl, contractOfferCatalog, queryOfferResponse);

		return queryOfferResponse;
	}

	private void handleContractOffer(JsonNode jOffer, String sproviderUrl, JsonNode contractOfferCatalog, List<QueryDataOfferModel> queryOfferResponse) {
		if (jOffer.isArray()) {
			jOffer.forEach(
					offer -> handleContractOffer(sproviderUrl, contractOfferCatalog, offer, queryOfferResponse));
		} else {
			handleContractOffer(sproviderUrl, contractOfferCatalog, jOffer, queryOfferResponse);
		}
	}

	public void handleContractOffer(String sproviderUrl, JsonNode contractOfferCatalog, JsonNode offer, List<QueryDataOfferModel> queryOfferResponse) {

		JsonNode contractOffers = getHasPolicy(offer);
		
		String edcstr = EDCAssetConstant.ASSET_PREFIX;
		
		QueryDataOfferModel build = QueryDataOfferModel.builder()
				.assetId(getFieldFromJsonNode(offer, edcstr + EDCAssetConstant.ASSET_PROP_ID))
				.connectorOfferUrl(sproviderUrl)
				.title(getFieldFromJsonNode(offer, edcstr + EDCAssetConstant.ASSET_PROP_NAME))
				.type(getFieldFromJsonNode(offer, edcstr + EDCAssetConstant.ASSET_PROP_TYPE))
				.description(getFieldFromJsonNode(offer, edcstr + EDCAssetConstant.ASSET_PROP_DESCRIPTION))
				.created(getFieldFromJsonNode(offer, edcstr + EDCAssetConstant.ASSET_PROP_CREATED))
				.modified(getFieldFromJsonNode(offer, edcstr + EDCAssetConstant.ASSET_PROP_MODIFIED))
				.publisher(getFieldFromJsonNode(contractOfferCatalog, edcstr + "participantId"))
				.version(getFieldFromJsonNode(offer, edcstr + EDCAssetConstant.ASSET_PROP_VERSION))
				.fileName(getFieldFromJsonNode(offer, edcstr + EDCAssetConstant.ASSET_PROP_FILENAME))
				.fileContentType(getFieldFromJsonNode(offer, edcstr + EDCAssetConstant.ASSET_PROP_CONTENTTYPE))
				.connectorId(getFieldFromJsonNode(contractOfferCatalog, edcstr+"participantId"))
				.build();
		
		if (contractOffers.isArray()) {
			contractOffers.forEach(
					contractOffer -> queryOfferResponse.add(buildContractOffer(build, contractOffer)));
		} else {
			queryOfferResponse.add(buildContractOffer(build, contractOffers));
		}

	}

	public QueryDataOfferModel buildContractOffer(QueryDataOfferModel build, JsonNode contractOffer) {
		
		build.setOfferId(getFieldFromJsonNode(contractOffer, "@id"));
		build.setHasPolicy(contractOffer);
		checkAndSetPolicyPermission(build, contractOffer);
		return build;
	}
	
	private void checkAndSetPolicyPermission(QueryDataOfferModel build, JsonNode policy) {

		if (policy != null && policy.isArray()) {
			policy.forEach(pol -> {
				JsonNode permission = getPermission(pol);
				checkAndSetPolicyPermissionsConstraints(build, permission);
			});
		} else if (policy != null) {
			JsonNode permission = getPermission(policy);
			checkAndSetPolicyPermissionsConstraints(build, permission);
		}
	}

	private void checkAndSetPolicyPermissionsConstraints(QueryDataOfferModel build, JsonNode permissions) {

		if (permissions != null && permissions.isArray()) {
			permissions.forEach(permission -> {
				checkAndSetPolicyPermissionConstraints(build, permission);
			});
		} else {
			checkAndSetPolicyPermissionConstraints(build, permissions);
		}
	}
	
	
	private void checkAndSetPolicyPermissionConstraints(QueryDataOfferModel build, JsonNode permission) {

		JsonNode constraints = getConstraint(permission);

		List<Policies> usagePolicies = new ArrayList<>();

		if (constraints != null) {
			JsonNode jsonNode = getAnd(constraints);

			if (jsonNode != null && jsonNode.isArray()) {
				jsonNode.forEach(constraint -> setConstraint(usagePolicies, constraint));
			} else if (jsonNode != null) {
				setConstraint(usagePolicies, jsonNode);
			}
		}

		build.setPolicy(PolicyModel.builder().accessPolicies(null).usagePolicies(usagePolicies).build());
	}

	private void setConstraint(List<Policies> usagePolicies, JsonNode jsonNode) {

		// All policy recieved in catalog are usage policy ,
		// accespoliocy already applied for access control,
		// in this constrain all are usage policy

		JsonNode leftOperand = getLeftOperand(jsonNode);
		JsonNode rightOperand = getRightOperand(jsonNode);
		String leftOperandText = leftOperand != null ? leftOperand.asText() : "";
		if(leftOperand != null && leftOperand.isObject()) {
			leftOperandText = getFieldFromJsonNode(leftOperand, "@id");
		}

		String rightOperandText = rightOperand != null ? rightOperand.asText() : "";
		Policies policyResponse = UtilityFunctions.identyAndGetUsagePolicy(leftOperandText, rightOperandText);
		if (policyResponse != null)
			usagePolicies.add(policyResponse);
	}

	private String getFieldFromJsonNode(JsonNode jnode, String fieldName) {
		if (jnode.get(fieldName) != null)
			return jnode.get(fieldName).asText();
		else
			return "";
	}

	private static JsonNode getHasPolicy(JsonNode offer) {
		return Optional.ofNullable(offer.get("odrl:hasPolicy"))
				.orElseGet(() -> offer.get("hasPolicy"));
	}


	private static JsonNode getPermission(JsonNode pol) {
		return Optional.ofNullable(pol.get("odrl:permission"))
				.orElseGet(() -> pol.get("permission"));
	}

	private static JsonNode getConstraint(JsonNode permission) {
		return Optional.ofNullable(permission.get("odrl:constraint"))
				.orElseGet(() -> permission.get("constraint"));
	}


	private static JsonNode getDataset(JsonNode contractOfferCatalog) {
		return Optional.ofNullable(contractOfferCatalog.get("dcat:dataset"))
				.orElseGet(() -> contractOfferCatalog.get("dataset"));
	}

	private static JsonNode getLeftOperand(JsonNode jsonNode) {
		return Optional.ofNullable(jsonNode.get("odrl:leftOperand")
		).orElseGet(() -> jsonNode.get("leftOperand"));
	}

	private static JsonNode getRightOperand(JsonNode jsonNode) {
		return Optional.ofNullable(jsonNode.get("odrl:rightOperand")
		).orElseGet(() -> jsonNode.get("rightOperand"));
	}


	private static JsonNode getAnd(JsonNode constraints) {
		return Optional.ofNullable(constraints.get("odrl:and"))
				.orElseGet(() -> constraints.get("and"));
	}
}