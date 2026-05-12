/********************************************************************************
 * Copyright (c) 2022, 2024 T-Systems International GmbH
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

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.eclipse.tractusx.sde.bpndiscovery.handler.BpnDiscoveryProxyService;
import org.eclipse.tractusx.sde.bpndiscovery.model.response.BpnDiscoverySearchResponse;
import org.eclipse.tractusx.sde.common.entities.Policies;
import org.eclipse.tractusx.sde.edc.entities.request.policies.PolicyConstraintBuilderService;
import org.eclipse.tractusx.sde.edc.facilitator.ContractNegotiateManagementHelper;
import org.eclipse.tractusx.sde.edc.facilitator.EDRRequestHelper;
import org.eclipse.tractusx.sde.edc.gateways.database.ContractNegotiationInfoRepository;
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

@ExtendWith(MockitoExtension.class)
class ConsumerControlPanelServiceTest {

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
    void testQueryOnDataOfferEmpty() throws Exception {
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
}
