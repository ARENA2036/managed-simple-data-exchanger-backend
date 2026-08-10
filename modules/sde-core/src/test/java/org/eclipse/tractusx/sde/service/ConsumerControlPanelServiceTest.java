/********************************************************************************
 * Copyright (c) 2022, 2024 T-Systems International GmbH
 * Copyright (c) 2026 ARENA2036 e.V.
 * Copyright (c) 2022, 2024 Contributors to the Eclipse Foundation
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
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.tractusx.sde.bpndiscovery.handler.BpnDiscoveryProxyService;
import org.eclipse.tractusx.sde.bpndiscovery.model.response.BpnDiscoveryResponse;
import org.eclipse.tractusx.sde.bpndiscovery.model.response.BpnDiscoverySearchResponse;
import org.eclipse.tractusx.sde.common.entities.Policies;
import org.eclipse.tractusx.sde.edc.entities.request.policies.PolicyConstraintBuilderService;
import org.eclipse.tractusx.sde.edc.facilitator.ContractNegotiateManagementHelper;
import org.eclipse.tractusx.sde.edc.facilitator.EDRRequestHelper;
import org.eclipse.tractusx.sde.edc.gateways.database.ContractNegotiationInfoRepository;
import org.eclipse.tractusx.sde.edc.model.edr.EDRCachedByIdResponse;
import org.eclipse.tractusx.sde.edc.model.request.ConsumerRequest;
import org.eclipse.tractusx.sde.edc.model.request.Offer;
import org.eclipse.tractusx.sde.edc.model.response.QueryDataOfferModel;
import org.eclipse.tractusx.sde.edc.services.ConsumerControlPanelService;
import org.eclipse.tractusx.sde.edc.services.ContractNegotiationService;
import org.eclipse.tractusx.sde.edc.services.LookUpDTTwin;
import org.eclipse.tractusx.sde.edc.util.EDCAssetUrlCacheService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ConsumerControlPanelServiceTest {

    private static final String PART_ID = "part-1";
    private static final String SUBMODEL = "pcf";
    private static final String BPN_ONE = "BPNL000000000001";
    private static final String BPN_TWO = "BPNL000000000002";
    private static final String CONNECTOR_OFFER_URL = "https://provider.example/api/v1/dsp";
    private static final String SEMANTIC_VERSION = "1.0.0";

    @Mock
    private ContractNegotiateManagementHelper contractNegotiateManagement;

    @Mock
    private ContractNegotiationInfoRepository contractNegotiationInfoRepository;

    @Mock
    private PolicyConstraintBuilderService policyConstraintBuilderService;

    @Mock
    private EDRRequestHelper eDRRequestHelper;

    @Mock
    private BpnDiscoveryProxyService bpnDiscoveryProxyService;

    @Mock
    private EDCAssetUrlCacheService eDCAssetUrlCacheService;

    @Mock
    private ContractNegotiationService contractNegotiationService;

    @Mock
    private LookUpDTTwin lookUpDTTwin;

    @InjectMocks
    private ConsumerControlPanelService consumerControlPanelService;

    @Test
    void testQueryOnDataOfferEmpty() {
        BpnDiscoverySearchResponse build = BpnDiscoverySearchResponse.builder()
                .bpns(List.of())
                .build();
        when(bpnDiscoveryProxyService.bpnDiscoverySearchData(any())).thenReturn(build);

        Set<QueryDataOfferModel> queryOnDataOffers = consumerControlPanelService.queryOnDataOffers("example", "", "",
                0, 0);

        assertTrue(queryOnDataOffers.isEmpty());
    }

    @Test
    void testSubscribeDataOffers1() {
        ArrayList<Offer> offerRequestList = new ArrayList<>();
        List<Policies> usagePolicies = new ArrayList<>();
        Policies usagePolicy = Policies.builder()
                .technicalKey("BusinessPartnerNumber")
                .value(List.of("BPN123456789"))
                .build();

        usagePolicies.add(usagePolicy);

        Policies usagePolicy1 = Policies.builder()
                .technicalKey("A")
                .value(List.of("A"))
                .build();

        usagePolicies.add(usagePolicy1);

        Policies usagePolicy2 = Policies.builder()
                .technicalKey("B")
                .value(List.of("B"))
                .build();
        usagePolicies.add(usagePolicy2);

        ConsumerRequest consumerRequest = new ConsumerRequest(offerRequestList,
                usagePolicies, "csv");
        String processId = UUID.randomUUID().toString();

        consumerControlPanelService.subscribeDataOffers(consumerRequest, processId);

        verify(policyConstraintBuilderService).getUsagePoliciesConstraints(usagePolicies);
        verify(contractNegotiationInfoRepository, never()).save(any());
    }

    @Test
    void queryOnDataOffersUsesProvidedBpnAndSkipsBpnDiscovery() {
        when(eDCAssetUrlCacheService.getDDTRUrl(BPN_ONE)).thenReturn(List.of());

        Set<QueryDataOfferModel> queryOnDataOffers = consumerControlPanelService.queryOnDataOffers(PART_ID, BPN_ONE,
                SUBMODEL, 0, 10);

        assertTrue(queryOnDataOffers.isEmpty());
        verify(bpnDiscoveryProxyService, never()).bpnDiscoverySearchData(any());
        verify(eDCAssetUrlCacheService).getDDTRUrl(BPN_ONE);
    }

    @Test
    void queryOnDataOffersAggregatesResultsFromEveryDiscoveredBpn() {
        when(bpnDiscoveryProxyService.bpnDiscoverySearchData(any())).thenReturn(discovered(BPN_ONE, BPN_TWO));

        QueryDataOfferModel dtrOfferOne = dtrOffer("dtr-asset-1");
        QueryDataOfferModel dtrOfferTwo = dtrOffer("dtr-asset-2");
        when(eDCAssetUrlCacheService.getDDTRUrl(BPN_ONE)).thenReturn(List.of(dtrOfferOne));
        when(eDCAssetUrlCacheService.getDDTRUrl(BPN_TWO)).thenReturn(List.of(dtrOfferTwo));

        EDRCachedByIdResponse tokenOne = token("Bearer token-1");
        EDRCachedByIdResponse tokenTwo = token("Bearer token-2");
        when(eDCAssetUrlCacheService.getTokenWithoutRefresh(BPN_ONE, dtrOfferOne)).thenReturn(tokenOne);
        when(eDCAssetUrlCacheService.getTokenWithoutRefresh(BPN_TWO, dtrOfferTwo)).thenReturn(tokenTwo);

        QueryDataOfferModel submodelOfferOne = submodelOffer("submodel-asset-1");
        QueryDataOfferModel submodelOfferTwo = submodelOffer("submodel-asset-2");
        when(lookUpDTTwin.lookUpTwin(tokenOne, dtrOfferOne, PART_ID, BPN_ONE, SUBMODEL, 0, 10))
                .thenReturn(List.of(submodelOfferOne));
        when(lookUpDTTwin.lookUpTwin(tokenTwo, dtrOfferTwo, PART_ID, BPN_TWO, SUBMODEL, 0, 10))
                .thenReturn(List.of(submodelOfferTwo));

        Set<QueryDataOfferModel> queryOnDataOffers = consumerControlPanelService.queryOnDataOffers(PART_ID, "",
                SUBMODEL, 0, 10);

        assertEquals(Set.of(submodelOfferOne, submodelOfferTwo), queryOnDataOffers);
    }

    @Test
    void queryOnDataOffersSkipsTokenAndTwinLookupWhenNoDtrOfferIsAvailable() {
        when(bpnDiscoveryProxyService.bpnDiscoverySearchData(any())).thenReturn(discovered(BPN_ONE));
        when(eDCAssetUrlCacheService.getDDTRUrl(BPN_ONE)).thenReturn(List.of());

        Set<QueryDataOfferModel> queryOnDataOffers = consumerControlPanelService.queryOnDataOffers(PART_ID, "",
                SUBMODEL, 0, 10);

        assertTrue(queryOnDataOffers.isEmpty());
        verify(eDCAssetUrlCacheService, never()).getTokenWithoutRefresh(any(), any());
        verify(lookUpDTTwin, never()).lookUpTwin(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void queryOnDataOffersRequestsTokenOnlyOnceForDuplicateDtrOffers() {
        when(bpnDiscoveryProxyService.bpnDiscoverySearchData(any())).thenReturn(discovered(BPN_ONE));

        QueryDataOfferModel dtrOffer = dtrOffer("dtr-asset-1");
        QueryDataOfferModel duplicateDtrOffer = dtrOffer("dtr-asset-1");
        when(eDCAssetUrlCacheService.getDDTRUrl(BPN_ONE)).thenReturn(List.of(dtrOffer, duplicateDtrOffer));

        EDRCachedByIdResponse edrToken = token("Bearer token-1");
        when(eDCAssetUrlCacheService.getTokenWithoutRefresh(BPN_ONE, dtrOffer)).thenReturn(edrToken);

        QueryDataOfferModel submodelOffer = submodelOffer("submodel-asset-1");
        when(lookUpDTTwin.lookUpTwin(edrToken, dtrOffer, PART_ID, BPN_ONE, SUBMODEL, 0, 10))
                .thenReturn(List.of(submodelOffer));

        Set<QueryDataOfferModel> queryOnDataOffers = consumerControlPanelService.queryOnDataOffers(PART_ID, "",
                SUBMODEL, 0, 10);

        assertEquals(Set.of(submodelOffer), queryOnDataOffers);
        verify(eDCAssetUrlCacheService, times(1)).getTokenWithoutRefresh(BPN_ONE, dtrOffer);
    }

    @Test
    void queryOnDataOffersUsesRefreshableTokenWhenEdrRefreshIsEnabled() {
        ReflectionTestUtils.setField(consumerControlPanelService, "withEdrRefresh", true);

        when(bpnDiscoveryProxyService.bpnDiscoverySearchData(any())).thenReturn(discovered(BPN_ONE));

        QueryDataOfferModel dtrOffer = dtrOffer("dtr-asset-1");
        when(eDCAssetUrlCacheService.getDDTRUrl(BPN_ONE)).thenReturn(List.of(dtrOffer));

        EDRCachedByIdResponse edrToken = token("Bearer refreshed-token");
        when(eDCAssetUrlCacheService.verifyAndGetToken(BPN_ONE, dtrOffer)).thenReturn(edrToken);

        QueryDataOfferModel submodelOffer = submodelOffer("submodel-asset-1");
        when(lookUpDTTwin.lookUpTwin(edrToken, dtrOffer, PART_ID, BPN_ONE, SUBMODEL, 0, 10))
                .thenReturn(List.of(submodelOffer));

        Set<QueryDataOfferModel> queryOnDataOffers = consumerControlPanelService.queryOnDataOffers(PART_ID, "",
                SUBMODEL, 0, 10);

        assertEquals(Set.of(submodelOffer), queryOnDataOffers);
        verify(eDCAssetUrlCacheService).verifyAndGetToken(BPN_ONE, dtrOffer);
        verify(eDCAssetUrlCacheService, never()).getTokenWithoutRefresh(any(), any());
    }

    @Test
    void queryOnDataOffersSkipsTwinLookupWhenEdrTokenIsNull() {
        when(bpnDiscoveryProxyService.bpnDiscoverySearchData(any())).thenReturn(discovered(BPN_ONE));

        QueryDataOfferModel dtrOffer = dtrOffer("dtr-asset-1");
        when(eDCAssetUrlCacheService.getDDTRUrl(BPN_ONE)).thenReturn(List.of(dtrOffer));
        when(eDCAssetUrlCacheService.getTokenWithoutRefresh(BPN_ONE, dtrOffer)).thenReturn(null);

        Set<QueryDataOfferModel> queryOnDataOffers = consumerControlPanelService.queryOnDataOffers(PART_ID, "",
                SUBMODEL, 0, 10);

        assertTrue(queryOnDataOffers.isEmpty());
        verify(lookUpDTTwin, never()).lookUpTwin(any(), any(), any(), any(), any(), any(), any());
    }

    @Test
    void queryOnDataOffersDeduplicatesEqualOffersFoundForDifferentBpns() {
        when(bpnDiscoveryProxyService.bpnDiscoverySearchData(any())).thenReturn(discovered(BPN_ONE, BPN_TWO));

        QueryDataOfferModel dtrOfferOne = dtrOffer("dtr-asset-1");
        QueryDataOfferModel dtrOfferTwo = dtrOffer("dtr-asset-2");
        when(eDCAssetUrlCacheService.getDDTRUrl(BPN_ONE)).thenReturn(List.of(dtrOfferOne));
        when(eDCAssetUrlCacheService.getDDTRUrl(BPN_TWO)).thenReturn(List.of(dtrOfferTwo));

        EDRCachedByIdResponse tokenOne = token("Bearer token-1");
        EDRCachedByIdResponse tokenTwo = token("Bearer token-2");
        when(eDCAssetUrlCacheService.getTokenWithoutRefresh(BPN_ONE, dtrOfferOne)).thenReturn(tokenOne);
        when(eDCAssetUrlCacheService.getTokenWithoutRefresh(BPN_TWO, dtrOfferTwo)).thenReturn(tokenTwo);

        when(lookUpDTTwin.lookUpTwin(tokenOne, dtrOfferOne, PART_ID, BPN_ONE, SUBMODEL, 0, 10))
                .thenReturn(List.of(submodelOffer("shared-submodel-asset")));
        when(lookUpDTTwin.lookUpTwin(tokenTwo, dtrOfferTwo, PART_ID, BPN_TWO, SUBMODEL, 0, 10))
                .thenReturn(List.of(submodelOffer("shared-submodel-asset")));

        Set<QueryDataOfferModel> queryOnDataOffers = consumerControlPanelService.queryOnDataOffers(PART_ID, "",
                SUBMODEL, 0, 10);

        assertEquals(1, queryOnDataOffers.size());
        assertEquals(Set.of(submodelOffer("shared-submodel-asset")), queryOnDataOffers);
    }

    private static BpnDiscoverySearchResponse discovered(String... bpns) {
        return BpnDiscoverySearchResponse.builder()
                .bpns(Arrays.stream(bpns)
                        .map(bpn -> BpnDiscoveryResponse.builder()
                                .type("manufacturerPartId")
                                .key(PART_ID)
                                .value(bpn)
                                .build())
                        .toList())
                .build();
    }

    private static QueryDataOfferModel dtrOffer(String assetId) {
        return QueryDataOfferModel.builder()
                .assetId(assetId)
                .connectorOfferUrl(CONNECTOR_OFFER_URL)
                .sematicVersion(SEMANTIC_VERSION)
                .connectorId("provider")
                .build();
    }

    private static QueryDataOfferModel submodelOffer(String assetId) {
        return QueryDataOfferModel.builder()
                .assetId(assetId)
                .connectorOfferUrl(CONNECTOR_OFFER_URL)
                .sematicVersion(SEMANTIC_VERSION)
                .manufacturerPartId(PART_ID)
                .build();
    }

    private static EDRCachedByIdResponse token(String authorization) {
        return EDRCachedByIdResponse.builder()
                .authorization(authorization)
                .endpoint("https://provider.example/edr")
                .build();
    }
}
