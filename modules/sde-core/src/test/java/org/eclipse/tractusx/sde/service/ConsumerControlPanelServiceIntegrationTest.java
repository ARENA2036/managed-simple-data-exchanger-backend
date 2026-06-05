/********************************************************************************
 * Copyright (c) 2026 ARENA2036 e.V.
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.sde.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.eclipse.tractusx.sde.bpndiscovery.handler.BpnDiscoveryProxyService;
import org.eclipse.tractusx.sde.bpndiscovery.model.request.BpnDiscoverySearchRequest;
import org.eclipse.tractusx.sde.bpndiscovery.model.response.BpnDiscoveryResponse;
import org.eclipse.tractusx.sde.bpndiscovery.model.response.BpnDiscoverySearchResponse;
import org.eclipse.tractusx.sde.common.configuration.properties.EDCVersionConfigurationProperties;
import org.eclipse.tractusx.sde.common.configuration.properties.SDEConfigurationProperties;
import org.eclipse.tractusx.sde.common.entities.Policies;
import org.eclipse.tractusx.sde.common.mapper.JsonObjectMapper;
import org.eclipse.tractusx.sde.edc.constants.EDCAssetConfigurableConstant;
import org.eclipse.tractusx.sde.edc.entities.database.ContractNegotiationInfoEntity;
import org.eclipse.tractusx.sde.edc.entities.request.policies.ActionRequest;
import org.eclipse.tractusx.sde.edc.entities.request.policies.ConstraintRequest;
import org.eclipse.tractusx.sde.edc.entities.request.policies.PolicyConstraintBuilderService;
import org.eclipse.tractusx.sde.edc.entities.request.policies.PolicyRequestFactory;
import org.eclipse.tractusx.sde.edc.facilitator.ContractNegotiateManagementHelper;
import org.eclipse.tractusx.sde.edc.facilitator.EDRRequestHelper;
import org.eclipse.tractusx.sde.edc.gateways.database.ContractNegotiationInfoRepository;
import org.eclipse.tractusx.sde.edc.model.contractnegotiation.AcknowledgementId;
import org.eclipse.tractusx.sde.edc.model.contractnegotiation.ContractNegotiationDto;
import org.eclipse.tractusx.sde.edc.model.edr.EDRCachedByIdResponse;
import org.eclipse.tractusx.sde.edc.model.request.ConsumerRequest;
import org.eclipse.tractusx.sde.edc.model.request.Offer;
import org.eclipse.tractusx.sde.edc.model.response.QueryDataOfferModel;
import org.eclipse.tractusx.sde.edc.services.ConsumerControlPanelService;
import org.eclipse.tractusx.sde.edc.services.ContractNegotiationService;
import org.eclipse.tractusx.sde.edc.services.LookUpDTTwin;
import org.eclipse.tractusx.sde.edc.util.EDCAssetUrlCacheService;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;

@SpringJUnitConfig(classes = ConsumerControlPanelServiceIntegrationTest.TestConfiguration.class)
@TestPropertySource(properties = {
        "edr.refresh.enable=false",
        "edc.version.minor=11",
        "manufacturerId=BPNL000000000001",
        "submodel.datasource.hostname=http://submodel.example",
        "edc.hostname=http://edc.example",
        "spring.security.oauth2.resourceserver.jwt.issuer-uri=http://issuer.example",
        "digital-twins.authentication.clientId=test-client",
        "bpdm.provider.edc.dsp.api=http://bpdm.example/api"
})
class ConsumerControlPanelServiceIntegrationTest {

    @MockitoBean
    private ContractNegotiateManagementHelper contractNegotiateManagement;

    @MockitoBean
    private ContractNegotiationInfoRepository contractNegotiationInfoRepository;

    @MockitoBean
    private EDRRequestHelper edrRequestHelper;

    @MockitoBean
    private BpnDiscoveryProxyService bpnDiscoveryProxyService;

    @MockitoBean
    private EDCAssetUrlCacheService edcAssetUrlCacheService;

    @MockitoBean
    private ContractNegotiationService contractNegotiationService;

    @MockitoBean
    private LookUpDTTwin lookUpDTTwin;

    @jakarta.annotation.Resource
    private ConsumerControlPanelService consumerControlPanelService;

    @Test
    void queryOnDataOffersDiscoversBpnFetchesDtrTokenAndLooksUpTwin() {
        BpnDiscoveryResponse discoveredBpn = BpnDiscoveryResponse.builder()
                .type("manufacturerPartId")
                .key("part-1")
                .value("BPNL000000000001")
                .build();
        when(bpnDiscoveryProxyService.bpnDiscoverySearchData(org.mockito.ArgumentMatchers.any()))
                .thenReturn(BpnDiscoverySearchResponse.builder()
                        .bpns(List.of(discoveredBpn))
                        .build());

        QueryDataOfferModel dtrOffer = QueryDataOfferModel.builder()
                .assetId("dtr-asset")
                .connectorOfferUrl("https://provider.example/api/v1/dsp")
                .sematicVersion("1.0.0")
                .connectorId("provider")
                .build();
        when(edcAssetUrlCacheService.getDDTRUrl("BPNL000000000001")).thenReturn(List.of(dtrOffer));

        EDRCachedByIdResponse edrToken = EDRCachedByIdResponse.builder()
                .authorization("Bearer token")
                .endpoint("https://provider.example/edr")
                .build();
        when(edcAssetUrlCacheService.getTokenWithoutRefresh("BPNL000000000001", dtrOffer)).thenReturn(edrToken);

        QueryDataOfferModel submodelOffer = QueryDataOfferModel.builder()
                .assetId("submodel-asset")
                .connectorOfferUrl("https://provider.example/api/v1/dsp")
                .sematicVersion("1.0.0")
                .manufacturerPartId("part-1")
                .build();
        when(lookUpDTTwin.lookUpTwin(edrToken, dtrOffer, "part-1", "BPNL000000000001", "pcf", 0, 10))
                .thenReturn(List.of(submodelOffer));

        Set<QueryDataOfferModel> offers = consumerControlPanelService.queryOnDataOffers("part-1", "", "pcf", 0, 10);

        assertEquals(Set.of(submodelOffer), offers);

        ArgumentCaptor<BpnDiscoverySearchRequest> requestCaptor =
                ArgumentCaptor.forClass(BpnDiscoverySearchRequest.class);
        verify(bpnDiscoveryProxyService).bpnDiscoverySearchData(requestCaptor.capture());
        BpnDiscoverySearchRequest request = requestCaptor.getValue();
        assertEquals("manufacturerPartId", request.getSearchFilter().get(0).getType());
        assertEquals(List.of("part-1"), request.getSearchFilter().get(0).getKeys());
    }

    @Test
    void subscribeDataOffersBuildsUsagePolicyNegotiatesContractAndPersistsFinalState() {
        Offer offer = Offer.builder()
                .connectorId("provider")
                .connectorOfferUrl("https://provider.example/api/v1/dsp")
                .offerId("offer-1")
                .assetId("asset-1")
                .build();
        List<Policies> usagePolicies = List.of(
                Policies.builder()
                        .technicalKey("BusinessPartnerNumber")
                        .value(List.of("BPNL000000000001"))
                        .build(),
                Policies.builder()
                        .technicalKey("UsagePurpose")
                        .value(List.of("cx.core.pcf:1"))
                        .build());
        ConsumerRequest request = new ConsumerRequest(List.of(offer), usagePolicies, "csv");

        when(contractNegotiateManagement.negotiateContract(
                eq("https://provider.example/api/v1/dsp"),
                eq("provider"),
                eq("offer-1"),
                eq("asset-1"),
                org.mockito.ArgumentMatchers.anyList(),
                anyMap()))
                .thenReturn(AcknowledgementId.builder().id("negotiation-1").build());
        when(contractNegotiateManagement.checkContractNegotiationStatus("negotiation-1"))
                .thenReturn(ContractNegotiationDto.builder()
                        .id("negotiation-1")
                        .state("FINALIZED")
                        .build());

        consumerControlPanelService.subscribeDataOffers(request, "process-1");

        ArgumentCaptor<List<ActionRequest>> actionCaptor = ArgumentCaptor.forClass(List.class);
        verify(contractNegotiateManagement).negotiateContract(
                eq("https://provider.example/api/v1/dsp"),
                eq("provider"),
                eq("offer-1"),
                eq("asset-1"),
                actionCaptor.capture(),
                anyMap());

        List<ActionRequest> actions = actionCaptor.getValue();
        assertEquals(1, actions.size());
        Object constraintGroup = actions.get(0).getAction().get("and");
        assertTrue(constraintGroup instanceof List<?>);
        List<ConstraintRequest> constraints = ((List<?>) constraintGroup).stream()
                .map(ConstraintRequest.class::cast)
                .toList();
        assertEquals(Set.of("BusinessPartnerNumber", "UsagePurpose"), constraints.stream()
                .map(ConstraintRequest::getLeftOperand)
                .collect(Collectors.toSet()));
        assertTrue(constraints.stream()
                .filter(constraint -> "UsagePurpose".equals(constraint.getLeftOperand()))
                .map(ConstraintRequest::getRightOperand)
                .anyMatch(List.of("cx.core.pcf:1")::equals));

        ArgumentCaptor<ContractNegotiationInfoEntity> entityCaptor =
                ArgumentCaptor.forClass(ContractNegotiationInfoEntity.class);
        verify(contractNegotiationInfoRepository).save(entityCaptor.capture());
        ContractNegotiationInfoEntity savedEntity = entityCaptor.getValue();
        assertNotNull(savedEntity.getId());
        assertEquals("process-1", savedEntity.getProcessId());
        assertEquals("provider", savedEntity.getConnectorId());
        assertEquals("offer-1", savedEntity.getOfferId());
        assertEquals("negotiation-1", savedEntity.getContractNegotiationId());
        assertEquals("FINALIZED", savedEntity.getStatus());
        assertNotNull(savedEntity.getDateTime());
    }

    @Configuration(proxyBeanMethods = false)
    @Import({
            ConsumerControlPanelService.class,
            PolicyConstraintBuilderService.class,
            PolicyRequestFactory.class,
            EDCAssetConfigurableConstant.class,
            EDCVersionConfigurationProperties.class,
            SDEConfigurationProperties.class
    })
    static class TestConfiguration {

        @Bean
        JsonObjectMapper jsonObjectMapper() {
            return new JsonObjectMapper() {
            };
        }
    }
}
