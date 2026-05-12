/********************************************************************************
 * Copyright (c) 2023,2024 T-Systems International GmbH
 * Copyright (c) 2023,2024 Contributors to the Eclipse Foundation
 * Copyright (c) 2025 ARENA2036 e.V.
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

package org.eclipse.tractusx.sde.configuration;

import java.util.*;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.NullNode;
import org.eclipse.tractusx.sde.common.configuration.properties.DigitalTwinConfigurationProperties;
import org.eclipse.tractusx.sde.common.entities.Policies;
import org.eclipse.tractusx.sde.common.entities.PolicyModel;
import org.eclipse.tractusx.sde.common.utils.PolicyOperationUtil;
import org.eclipse.tractusx.sde.core.utils.ValueReplacerUtility;
import org.eclipse.tractusx.sde.edc.constants.EDCAssetConfigurableConstant;
import org.eclipse.tractusx.sde.edc.entities.request.asset.AssetEntryRequest;
import org.eclipse.tractusx.sde.edc.entities.request.asset.AssetEntryRequestFactory;
import org.eclipse.tractusx.sde.edc.facilitator.CreateEDCAssetFacilitator;
import org.eclipse.tractusx.sde.edc.gateways.external.EDCGateway;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import static org.eclipse.tractusx.sde.common.utils.JsonObjectUtility.*;

@Slf4j
@Service
@ConditionalOnProperty(name = "digital-twins.asset.registration.enabled", havingValue = "true", matchIfMissing = true)
@RequiredArgsConstructor
public class DigitalTwinAssetProvider {

    public static final String EDC_DTR_CONTRACT_LOOKUP_TEMPLATE = "edc_request_template/edc_contract_definition_lookup.json";
    public static final String EDC_DTR_ASSET_LOOKUP_TEMPLATE = "edc_request_template/edc_dtr_asset_lookup.json";
    private static final ObjectMapper MAPPER = new ObjectMapper();

    private final AssetEntryRequestFactory assetFactory;
    private final EDCGateway edcGateway;
    private final CreateEDCAssetFacilitator createEDCAssetFacilitator;
    private final DigitalTwinConfigurationProperties digitalTwinConfigurationProperties;
    private final ValueReplacerUtility valueReplacerUtility;
    private final EDCAssetConfigurableConstant edcAssetConfigurableConstant;


    @PostConstruct
    @SneakyThrows
    public void registerDigitalTwinRegistryToEdc() {

        if (digitalTwinConfigurationProperties.getDigitalTwinsRegistryPath()
                .equals(digitalTwinConfigurationProperties.getDigitalTwinsLookupPath())) {
            registerDigitalTwinRegistryToEdc("registry", digitalTwinConfigurationProperties.getDigitalTwinsRegistryPath());
        } else {
            registerDigitalTwinRegistryToEdc("registry-api", digitalTwinConfigurationProperties.getDigitalTwinsRegistryPath());
            registerDigitalTwinRegistryToEdc("discovery-api", digitalTwinConfigurationProperties.getDigitalTwinsLookupPath());
        }
    }

    private void registerDigitalTwinRegistryToEdc(String registryType, String registryAPI) throws JsonProcessingException {
        //Check if the digital twin registry asset is already present
        ObjectNode requestBody = (ObjectNode) MAPPER
                .readTree(valueReplacerUtility.getRequestFile(EDC_DTR_ASSET_LOOKUP_TEMPLATE));
        JsonNode dtrAssets = edcGateway.getAssetsByFilterExpression(requestBody);
        //set up the createAssetRequest
        String baseUrl = digitalTwinConfigurationProperties.getDigitalTwinsHostname() + registryAPI;
        AssetEntryRequest createAssetRequest = assetFactory.createDigitalTwinRegistryAssetRequest(baseUrl, registryType);

        //No digital twin registry asset is already present
        if (!containsDtrAsset(dtrAssets)) {
            //create the asset, the related policies and a contract definition
            Map<String, String> createEDCAsset = createEDCAssetFacilitator.createAssetWithPoliciesAndContract(createAssetRequest, createPolicies());
            log.info("Digital twin {} asset creates: {}", registryType, createEDCAsset.toString());
        }
        //One or multiple digital twin registry assets already present
        else {
            log.info("Digital twin {} asset already exists in edc connector", registryType);
            //Multiple digital twin registry assets already present
            if (dtrAssets.size() > 1) {
                log.warn("Multiple Digital Twin Registry Assets exist in edc connector:");
                createDtrContractIfPossible(registryType, dtrAssets, createAssetRequest);
            }
            //One digital twin registry asset is already present
            else {
                // Check if one or multiple contracts already present if no create contract and policies therefor
                createDtrContractIfPossible(registryType, dtrAssets, createAssetRequest);
            }
        }
    }

    private void createDtrContractIfPossible(String registryType, JsonNode dtrAssets, AssetEntryRequest createAssetRequest) {
        //find and collect all contracts over all assets
        List<JsonNode> contractDefinitions = findAllDtrContractDefinitions(dtrAssets);
        //check if also multiple dtr contracts exist
        if (contractDefinitions.size() > 1) {
            log.error("Multiple Digital Twin Registry Contracts exist in edc connector");
            contractDefinitions.stream()
                    .map(contract -> getValueFromJsonObjectAsString(contract, "@id"))
                    .distinct()
                    .forEach(contractId -> log.error("-Located Digital Twin Registry Contract: {}", contractId));
        }
        // One digital twin registry contract is already present
        else if (contractDefinitions.size() == 1) {
            // log the already present Contract
            JsonNode contractDefinition = contractDefinitions.get(0);
            log.info("Digital Twin Registry Contract found in edc connector: {}", contractDefinition.toPrettyString());
        }
        // No digital twin registry contract is present
        // -> therefor update the present asset and create the related policies and contract
        else {
            String dtrAssetId = getFirstValueFromJsonArray(dtrAssets, "@id");
            createAssetRequest.setId(dtrAssetId);
            Map<String, String> createEDCAsset = createEDCAssetFacilitator.updateAssetAndCreatePoliciesAndContract(createAssetRequest, createPolicies());
            log.info("Digital twin {} asset updated: {}", registryType, createEDCAsset.toString());
        }
    }

    private PolicyModel createPolicies() {
        List<Policies> accessPolicy = PolicyOperationUtil
                .getStringPolicyAsPolicyList(edcAssetConfigurableConstant.getDigitalTwinExchangeAccessPolicy());

        List<Policies> usagePolicy = PolicyOperationUtil
                .getStringPolicyAsPolicyList(edcAssetConfigurableConstant.getDigitalTwinExchangeUsagePolicy());

        return PolicyModel.builder()
                .accessPolicies(accessPolicy)
                .usagePolicies(usagePolicy)
                .build();
    }

    private List<JsonNode> findAllDtrContractDefinitions(JsonNode dtrAssets) {
        List<JsonNode> contractDefinitions = new ArrayList<>();

        for (JsonNode asset : dtrAssets) {
            String assetId = getValueFromJsonObjectAsString(asset, "@id");
            JsonNode dtrContracts = findDtrContractDefinitionsByAsset(assetId);
            if (dtrContracts == null) {
                continue;
            }
            if (!dtrContracts.isNull() && dtrContracts.isArray()) {
                dtrContracts.iterator().forEachRemaining(contractDefinitions::add);
            } else {
                contractDefinitions.add(dtrContracts);
            }
        }
        return contractDefinitions;
    }


    private JsonNode findDtrContractDefinitionsByAsset(String assetId) {
        if (assetId == null || assetId.isBlank()) {
            return NullNode.getInstance();
        }

        try {
            JsonNode contractDefinitionRequestBody = MAPPER.readTree(
                    valueReplacerUtility.valueReplacerUsingFileTemplate(EDC_DTR_CONTRACT_LOOKUP_TEMPLATE, Map.of("assetId", assetId)));
            return edcGateway.getContractDefinitionsByFilterExpression((ObjectNode) contractDefinitionRequestBody);
        } catch (JsonProcessingException e) {
            log.warn("Error parsing EDC Contract Definitions for asset {}", assetId, e);
        }
        return NullNode.getInstance();
    }

    private boolean containsDtrAsset(JsonNode dtrAssets) {
        return Optional.ofNullable(dtrAssets)
                .map(result -> result.isArray() && !result.isEmpty())
                .orElse(false);
    }

}
