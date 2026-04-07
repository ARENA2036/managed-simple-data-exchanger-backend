/********************************************************************************
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.sde.configuration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.eclipse.tractusx.sde.common.configuration.properties.DigitalTwinConfigurationProperties;
import org.eclipse.tractusx.sde.common.entities.PolicyModel;
import org.eclipse.tractusx.sde.core.utils.ValueReplacerUtility;
import org.eclipse.tractusx.sde.edc.constants.EDCAssetConfigurableConstant;
import org.eclipse.tractusx.sde.edc.entities.request.asset.AssetEntryRequest;
import org.eclipse.tractusx.sde.edc.entities.request.asset.AssetEntryRequestFactory;
import org.eclipse.tractusx.sde.edc.facilitator.CreateEDCAssetFacilitator;
import org.eclipse.tractusx.sde.edc.gateways.external.EDCGateway;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class DigitalTwinAssetProviderTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Mock
    private AssetEntryRequestFactory assetFactory;

    @Mock
    private EDCGateway edcGateway;

    @Mock
    private CreateEDCAssetFacilitator createEDCAssetFacilitator;

    @Mock
    private DigitalTwinConfigurationProperties digitalTwinConfigurationProperties;

    @Mock
    private ValueReplacerUtility valueReplacerUtility;

    @Mock
    private EDCAssetConfigurableConstant edcAssetConfigurableConstant;

    private DigitalTwinAssetProvider provider;

    @BeforeEach
    void setUp() {
        provider = new DigitalTwinAssetProvider(
                assetFactory,
                edcGateway,
                createEDCAssetFacilitator,
                digitalTwinConfigurationProperties,
                valueReplacerUtility,
                edcAssetConfigurableConstant);

        when(digitalTwinConfigurationProperties.getDigitalTwinsHostname()).thenReturn("https://dtr.example");
        when(valueReplacerUtility.getRequestFile(DigitalTwinAssetProvider.EDC_DTR_ASSET_LOOKUP_TEMPLATE)).thenReturn("{}");
        lenient().when(valueReplacerUtility.valueReplacer(eq(DigitalTwinAssetProvider.EDC_DTR_CONTRACT_LOOKUP_TEMPLATE), any()))
                .thenReturn("{}");
        lenient().when(edcAssetConfigurableConstant.getDigitalTwinExchangeAccessPolicy()).thenReturn("Membership@active");
        lenient().when(edcAssetConfigurableConstant.getDigitalTwinExchangeUsagePolicy())
                .thenReturn("UsagePurpose@isAnyOf@cx.core.digitalTwinRegistry:1");
        lenient().when(createEDCAssetFacilitator.createAssetWithPoliciesAndContract(any(), any()))
                .thenReturn(Map.of("assetId", "created"));
        lenient().when(createEDCAssetFacilitator.updateAssetAndCreatePoliciesAndContract(any(), any()))
                .thenReturn(Map.of("assetId", "updated"));
    }

    @Test
    void registerDigitalTwinRegistryToEdcCreatesAssetWhenNoAssetExistsAndPathsMatch() {
        when(digitalTwinConfigurationProperties.getDigitalTwinsRegistryPath()).thenReturn("/api/registry");
        when(digitalTwinConfigurationProperties.getDigitalTwinsLookupPath()).thenReturn("/api/registry");

        AssetEntryRequest assetRequest = AssetEntryRequest.builder().id("new-asset").build();
        when(assetFactory.createDigitalTwinRegistryAssetRequest("https://dtr.example/api/registry", "registry"))
                .thenReturn(assetRequest);
        when(edcGateway.getAssetsByFilterExpression(any())).thenReturn(null);

        provider.registerDigitalTwinRegistryToEdc();

        ArgumentCaptor<PolicyModel> policyCaptor = ArgumentCaptor.forClass(PolicyModel.class);
        verify(createEDCAssetFacilitator).createAssetWithPoliciesAndContract(eq(assetRequest), policyCaptor.capture());
        verify(createEDCAssetFacilitator, never()).updateAssetAndCreatePoliciesAndContract(any(), any());

        PolicyModel policyModel = policyCaptor.getValue();
        assertNotNull(policyModel);
        assertEquals(1, policyModel.getAccessPolicies().size());
        assertEquals("Membership", policyModel.getAccessPolicies().get(0).getTechnicalKey());
        assertEquals(1, policyModel.getUsagePolicies().size());
        assertEquals("UsagePurpose", policyModel.getUsagePolicies().get(0).getTechnicalKey());
        assertEquals("isAnyOf", policyModel.getUsagePolicies().get(0).getOperator());
    }

    @Test
    void registerDigitalTwinRegistryToEdcUsesRegistryAndDiscoveryApisWhenPathsDiffer() throws Exception {
        when(digitalTwinConfigurationProperties.getDigitalTwinsRegistryPath()).thenReturn("/api/registry");
        when(digitalTwinConfigurationProperties.getDigitalTwinsLookupPath()).thenReturn("/api/discovery");

        AssetEntryRequest registryRequest = AssetEntryRequest.builder().id("generated-registry").build();
        AssetEntryRequest discoveryRequest = AssetEntryRequest.builder().id("generated-discovery").build();
        when(assetFactory.createDigitalTwinRegistryAssetRequest("https://dtr.example/api/registry", "registry-api"))
                .thenReturn(registryRequest);
        when(assetFactory.createDigitalTwinRegistryAssetRequest("https://dtr.example/api/discovery", "discovery-api"))
                .thenReturn(discoveryRequest);
        when(edcGateway.getAssetsByFilterExpression(any()))
                .thenReturn(jsonArray("""
                        [
                          {"@id":"existing-registry-asset"}
                        ]
                        """))
                .thenReturn(jsonArray("[]"));
        when(edcGateway.getContractDefinitionsByFilterExpression(any())).thenReturn(null);

        provider.registerDigitalTwinRegistryToEdc();

        verify(createEDCAssetFacilitator).updateAssetAndCreatePoliciesAndContract(eq(registryRequest), any());
        verify(createEDCAssetFacilitator).createAssetWithPoliciesAndContract(eq(discoveryRequest), any());
        assertEquals("existing-registry-asset", registryRequest.getId());
    }

    @Test
    void registerDigitalTwinRegistryToEdcUpdatesExistingAssetWhenNoContractExists() throws Exception {
        when(digitalTwinConfigurationProperties.getDigitalTwinsRegistryPath()).thenReturn("/api/registry");
        when(digitalTwinConfigurationProperties.getDigitalTwinsLookupPath()).thenReturn("/api/registry");

        AssetEntryRequest assetRequest = AssetEntryRequest.builder().id("generated-id").build();
        when(assetFactory.createDigitalTwinRegistryAssetRequest("https://dtr.example/api/registry", "registry"))
                .thenReturn(assetRequest);
        when(edcGateway.getAssetsByFilterExpression(any())).thenReturn(jsonArray("""
                [
                  {"@id":"existing-asset"}
                ]
                """));
        when(edcGateway.getContractDefinitionsByFilterExpression(any())).thenReturn(null);

        provider.registerDigitalTwinRegistryToEdc();

        verify(createEDCAssetFacilitator).updateAssetAndCreatePoliciesAndContract(eq(assetRequest), any());
        verify(createEDCAssetFacilitator, never()).createAssetWithPoliciesAndContract(any(), any());
        assertEquals("existing-asset", assetRequest.getId());
    }

    @Test
    void registerDigitalTwinRegistryToEdcDoesNothingWhenSingleContractAlreadyExists() throws Exception {
        when(digitalTwinConfigurationProperties.getDigitalTwinsRegistryPath()).thenReturn("/api/registry");
        when(digitalTwinConfigurationProperties.getDigitalTwinsLookupPath()).thenReturn("/api/registry");

        when(assetFactory.createDigitalTwinRegistryAssetRequest("https://dtr.example/api/registry", "registry"))
                .thenReturn(AssetEntryRequest.builder().id("generated-id").build());
        when(edcGateway.getAssetsByFilterExpression(any())).thenReturn(jsonArray("""
                [
                  {"@id":"existing-asset"}
                ]
                """));
        when(edcGateway.getContractDefinitionsByFilterExpression(any())).thenReturn(jsonObject("""
                {
                  "@id":"existing-contract"
                }
                """));

        provider.registerDigitalTwinRegistryToEdc();

        verify(createEDCAssetFacilitator, never()).createAssetWithPoliciesAndContract(any(), any());
        verify(createEDCAssetFacilitator, never()).updateAssetAndCreatePoliciesAndContract(any(), any());
    }

    @Test
    void registerDigitalTwinRegistryToEdcDoesNothingWhenMultipleContractsExistAcrossAssets() throws Exception {
        when(digitalTwinConfigurationProperties.getDigitalTwinsRegistryPath()).thenReturn("/api/registry");
        when(digitalTwinConfigurationProperties.getDigitalTwinsLookupPath()).thenReturn("/api/registry");

        when(assetFactory.createDigitalTwinRegistryAssetRequest("https://dtr.example/api/registry", "registry"))
                .thenReturn(AssetEntryRequest.builder().id("generated-id").build());
        when(edcGateway.getAssetsByFilterExpression(any())).thenReturn(jsonArray("""
                [
                  {"@id":"asset-1"},
                  {"@id":"asset-2"}
                ]
                """));
        when(edcGateway.getContractDefinitionsByFilterExpression(any()))
                .thenReturn(jsonArray("""
                        [
                          {"@id":"contract-1"}
                        ]
                        """))
                .thenReturn(jsonArray("""
                        [
                          {"@id":"contract-2"}
                        ]
                        """));

        provider.registerDigitalTwinRegistryToEdc();

        verify(createEDCAssetFacilitator, never()).createAssetWithPoliciesAndContract(any(), any());
        verify(createEDCAssetFacilitator, never()).updateAssetAndCreatePoliciesAndContract(any(), any());
    }

    @Test
    void registerDigitalTwinRegistryToEdcTreatsMissingAssetIdAsExistingContractAndSkipsUpdate() throws Exception {
        when(digitalTwinConfigurationProperties.getDigitalTwinsRegistryPath()).thenReturn("/api/registry");
        when(digitalTwinConfigurationProperties.getDigitalTwinsLookupPath()).thenReturn("/api/registry");

        when(assetFactory.createDigitalTwinRegistryAssetRequest("https://dtr.example/api/registry", "registry"))
                .thenReturn(AssetEntryRequest.builder().id("generated-id").build());
        when(edcGateway.getAssetsByFilterExpression(any())).thenReturn(jsonArray("""
                [
                  {}
                ]
                """));

        provider.registerDigitalTwinRegistryToEdc();

        verify(edcGateway, never()).getContractDefinitionsByFilterExpression(any());
        verify(createEDCAssetFacilitator, never()).createAssetWithPoliciesAndContract(any(), any());
        verify(createEDCAssetFacilitator, never()).updateAssetAndCreatePoliciesAndContract(any(), any());
    }

    @Test
    void registerDigitalTwinRegistryToEdcTreatsContractLookupParseFailureAsExistingContractAndSkipsUpdate() throws Exception {
        when(digitalTwinConfigurationProperties.getDigitalTwinsRegistryPath()).thenReturn("/api/registry");
        when(digitalTwinConfigurationProperties.getDigitalTwinsLookupPath()).thenReturn("/api/registry");

        when(assetFactory.createDigitalTwinRegistryAssetRequest("https://dtr.example/api/registry", "registry"))
                .thenReturn(AssetEntryRequest.builder().id("generated-id").build());
        when(edcGateway.getAssetsByFilterExpression(any())).thenReturn(jsonArray("""
                [
                  {"@id":"existing-asset"}
                ]
                """));
        when(valueReplacerUtility.valueReplacer(eq(DigitalTwinAssetProvider.EDC_DTR_CONTRACT_LOOKUP_TEMPLATE), any()))
                .thenReturn("not-json");

        provider.registerDigitalTwinRegistryToEdc();

        verify(createEDCAssetFacilitator, never()).createAssetWithPoliciesAndContract(any(), any());
        verify(createEDCAssetFacilitator, never()).updateAssetAndCreatePoliciesAndContract(any(), any());
    }

    private JsonNode jsonArray(String json) throws Exception {
        return MAPPER.readTree(json);
    }

    private JsonNode jsonObject(String json) throws Exception {
        return MAPPER.readTree(json);
    }
}
