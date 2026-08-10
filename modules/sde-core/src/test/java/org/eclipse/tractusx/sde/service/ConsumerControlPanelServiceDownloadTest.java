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
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.apache.commons.lang3.tuple.Pair;
import org.eclipse.tractusx.sde.bpndiscovery.handler.BpnDiscoveryProxyService;
import org.eclipse.tractusx.sde.edc.entities.request.policies.ActionRequest;
import org.eclipse.tractusx.sde.edc.entities.request.policies.PolicyConstraintBuilderService;
import org.eclipse.tractusx.sde.edc.facilitator.ContractNegotiateManagementHelper;
import org.eclipse.tractusx.sde.edc.facilitator.EDRRequestHelper;
import org.eclipse.tractusx.sde.edc.gateways.database.ContractNegotiationInfoRepository;
import org.eclipse.tractusx.sde.edc.model.edr.EDRCachedByIdResponse;
import org.eclipse.tractusx.sde.edc.model.edr.EDRCachedResponse;
import org.eclipse.tractusx.sde.edc.model.request.Offer;
import org.eclipse.tractusx.sde.edc.model.request.QueryDataOfferRequest;
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
class ConsumerControlPanelServiceDownloadTest {

    private static final String STATUS = "status";
    private static final String CONNECTOR_ID = "provider";
    private static final String RECIPIENT_URL = "https://provider.example/api/v1/dsp";
    private static final String DOWNLOAD_ENDPOINT = "https://provider.example/data";

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
    void subcribeAndDownloadOfferNegotiatesAndReturnsDownloadedPayload() {
        Offer offer = offer("asset-1");
        List<ActionRequest> action = List.of();

        EDRCachedResponse edr = completedEdr("asset-1");
        when(contractNegotiationService.verifyOrCreateContractNegotiation(CONNECTOR_ID, Map.of(), RECIPIENT_URL, action,
                offer)).thenReturn(edr);

        EDRCachedByIdResponse authorizationToken = authorizationToken();
        when(contractNegotiationService.getAuthorizationTokenForDataDownload("transfer-1"))
                .thenReturn(authorizationToken);
        when(eDRRequestHelper.getDataFromProvider(authorizationToken, DOWNLOAD_ENDPOINT + "?type=json"))
                .thenReturn("provider-payload");

        Map<String, Object> resultFields = consumerControlPanelService.subcribeAndDownloadOffer(offer, action, true,
                "json");

        assertEquals("SUCCESS", resultFields.get(STATUS));
        assertEquals(edr, resultFields.get("edr"));
        assertEquals("provider-payload", resultFields.get("data"));
    }

    @Test
    void subcribeAndDownloadOfferSkipsDownloadWhenImmediateDownloadIsNotRequested() {
        Offer offer = offer("asset-1");
        List<ActionRequest> action = List.of();

        EDRCachedResponse edr = completedEdr("asset-1");
        when(contractNegotiationService.verifyOrCreateContractNegotiation(CONNECTOR_ID, Map.of(), RECIPIENT_URL, action,
                offer)).thenReturn(edr);

        Map<String, Object> resultFields = consumerControlPanelService.subcribeAndDownloadOffer(offer, action, false,
                "json");

        assertEquals("SUCCESS", resultFields.get(STATUS));
        assertEquals(edr, resultFields.get("edr"));
        assertFalse(resultFields.containsKey("data"));
        verify(contractNegotiationService, never()).getAuthorizationTokenForDataDownload(any());
        verify(eDRRequestHelper, never()).getDataFromProvider(any(), any());
    }

    @Test
    void subcribeAndDownloadOfferFailsWhenAgreementExistsButTransferWasNeverInitiated() {
        Offer offer = offer("asset-1");
        List<ActionRequest> action = List.of();

        EDRCachedResponse edrWithoutTransfer = EDRCachedResponse.builder()
                .assetId("asset-1")
                .agreementId("agreement-1")
                .build();
        when(contractNegotiationService.verifyOrCreateContractNegotiation(CONNECTOR_ID, Map.of(), RECIPIENT_URL, action,
                offer)).thenReturn(edrWithoutTransfer);

        Map<String, Object> resultFields = consumerControlPanelService.subcribeAndDownloadOffer(offer, action, true,
                "json");

        assertEquals("FAILED", resultFields.get(STATUS));
        assertEquals(edrWithoutTransfer, resultFields.get("edr"));
        assertTrue(resultFields.get("error").toString().contains("data transfer is not completed"));
        assertFalse(resultFields.containsKey("data"));
        verify(eDRRequestHelper, never()).getDataFromProvider(any(), any());
    }

    @Test
    void subcribeAndDownloadOfferReportsFailureWhenProviderDownloadFails() {
        Offer offer = offer("asset-1");
        List<ActionRequest> action = List.of();

        when(contractNegotiationService.verifyOrCreateContractNegotiation(CONNECTOR_ID, Map.of(), RECIPIENT_URL, action,
                offer)).thenReturn(completedEdr("asset-1"));
        when(contractNegotiationService.getAuthorizationTokenForDataDownload("transfer-1"))
                .thenThrow(new RuntimeException("provider unreachable"));

        Map<String, Object> resultFields = consumerControlPanelService.subcribeAndDownloadOffer(offer, action, true,
                "json");

        assertEquals("FAILED", resultFields.get(STATUS));
        assertTrue(resultFields.get("error").toString().contains("provider unreachable"));
        assertFalse(resultFields.containsKey("data"));
    }

    @Test
    void subcribeAndDownloadOfferReportsFailureWhenNoEdrIsReturned() {
        Offer offer = offer("asset-1");
        List<ActionRequest> action = List.of();

        when(contractNegotiationService.verifyOrCreateContractNegotiation(CONNECTOR_ID, Map.of(), RECIPIENT_URL, action,
                offer)).thenReturn(null);

        Map<String, Object> resultFields = consumerControlPanelService.subcribeAndDownloadOffer(offer, action, true,
                "json");

        assertEquals("FAILED", resultFields.get(STATUS));
        assertTrue(resultFields.get("error").toString()
                .startsWith("Unable to complete subscribeAndDownloadDataOffers because:"));
        assertFalse(resultFields.containsKey("edr"));
        assertFalse(resultFields.containsKey("data"));
    }

    @Test
    void subcribeAndDownloadOfferStripsTrailingSlashFromConnectorOfferUrl() {
        Offer offer = Offer.builder()
                .connectorId(CONNECTOR_ID)
                .connectorOfferUrl(RECIPIENT_URL + "/")
                .offerId("offer-1")
                .assetId("asset-1")
                .build();
        List<ActionRequest> action = List.of();

        when(contractNegotiationService.verifyOrCreateContractNegotiation(CONNECTOR_ID, Map.of(), RECIPIENT_URL, action,
                offer)).thenReturn(completedEdr("asset-1"));

        Map<String, Object> resultFields = consumerControlPanelService.subcribeAndDownloadOffer(offer, action, false,
                "json");

        assertEquals("SUCCESS", resultFields.get(STATUS));
        verify(contractNegotiationService).verifyOrCreateContractNegotiation(CONNECTOR_ID, Map.of(), RECIPIENT_URL,
                action, offer);
    }

    @Test
    @SuppressWarnings("unchecked")
    void downloadFileFromEdcReturnsPerAssetResultForSucceededAndFailedAssets() {
        EDRCachedResponse completed = completedEdr("asset-1");
        when(contractNegotiationService.verifyEDRRequestStatus("asset-1")).thenReturn(completed);

        EDRCachedByIdResponse authorizationToken = authorizationToken();
        when(contractNegotiationService.getAuthorizationTokenForDataDownload("transfer-1"))
                .thenReturn(authorizationToken);
        when(eDRRequestHelper.getDataFromProvider(authorizationToken, DOWNLOAD_ENDPOINT + "?type=csv"))
                .thenReturn("csv-payload");

        EDRCachedResponse transferNotInitiated = EDRCachedResponse.builder()
                .assetId("asset-2")
                .agreementId("agreement-2")
                .build();
        when(contractNegotiationService.verifyEDRRequestStatus("asset-2")).thenReturn(transferNotInitiated);

        Map<String, Object> response = consumerControlPanelService
                .downloadFileFromEDCUsingifAlreadyTransferStatusCompleted(List.of("asset-1", "asset-2"), "csv");

        assertEquals(2, response.size());

        Map<String, Object> succeeded = (Map<String, Object>) response.get("asset-1");
        assertEquals("SUCCESS", succeeded.get(STATUS));
        assertEquals(completed, succeeded.get("edr"));
        assertEquals("csv-payload", succeeded.get("data"));

        Map<String, Object> failed = (Map<String, Object>) response.get("asset-2");
        assertEquals("FAILED", failed.get(STATUS));
        assertEquals(transferNotInitiated, failed.get("edr"));
        assertTrue(failed.get("error").toString().contains("asset-2"));
        assertFalse(failed.containsKey("data"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void downloadFileFromEdcAppendsRequestedTypeToProviderEndpoint() {
        when(contractNegotiationService.verifyEDRRequestStatus("asset-1")).thenReturn(completedEdr("asset-1"));

        EDRCachedByIdResponse authorizationToken = authorizationToken();
        when(contractNegotiationService.getAuthorizationTokenForDataDownload("transfer-1"))
                .thenReturn(authorizationToken);
        when(eDRRequestHelper.getDataFromProvider(authorizationToken, DOWNLOAD_ENDPOINT + "?type=json"))
                .thenReturn("json-payload");

        Map<String, Object> response = consumerControlPanelService
                .downloadFileFromEDCUsingifAlreadyTransferStatusCompleted(List.of("asset-1"), "json");

        Map<String, Object> succeeded = (Map<String, Object>) response.get("asset-1");
        assertEquals("json-payload", succeeded.get("data"));
        verify(eDRRequestHelper).getDataFromProvider(authorizationToken, DOWNLOAD_ENDPOINT + "?type=json");
    }

    @Test
    void getEdcPolicyGroupsRequestsByConnectorUrlAndPublisher() {
        QueryDataOfferRequest firstOnProviderA = queryDataOfferRequest("asset-1", "https://provider-a.example/dsp",
                "BPNL000000000001");
        QueryDataOfferRequest secondOnProviderA = queryDataOfferRequest("asset-2", "https://provider-a.example/dsp",
                "BPNL000000000001");
        QueryDataOfferRequest onProviderB = queryDataOfferRequest("asset-3", "https://provider-b.example/dsp",
                "BPNL000000000002");

        QueryDataOfferModel offerFromProviderA = QueryDataOfferModel.builder()
                .assetId("asset-1")
                .connectorOfferUrl("https://provider-a.example/dsp")
                .sematicVersion("1.0.0")
                .build();
        QueryDataOfferModel offerFromProviderB = QueryDataOfferModel.builder()
                .assetId("asset-3")
                .connectorOfferUrl("https://provider-b.example/dsp")
                .sematicVersion("1.0.0")
                .build();

        when(lookUpDTTwin.getEDCOffer(List.of(firstOnProviderA, secondOnProviderA),
                Pair.of("https://provider-a.example/dsp", "BPNL000000000001")))
                .thenReturn(List.of(offerFromProviderA));
        when(lookUpDTTwin.getEDCOffer(List.of(onProviderB),
                Pair.of("https://provider-b.example/dsp", "BPNL000000000002")))
                .thenReturn(List.of(offerFromProviderB));

        List<QueryDataOfferModel> offers = consumerControlPanelService
                .getEDCPolicy(List.of(firstOnProviderA, secondOnProviderA, onProviderB));

        assertEquals(2, offers.size());
        assertTrue(offers.containsAll(List.of(offerFromProviderA, offerFromProviderB)));
    }

    @Test
    void getEdcPolicyReturnsEmptyListForEmptyRequest() {
        List<QueryDataOfferModel> offers = consumerControlPanelService.getEDCPolicy(List.of());

        assertTrue(offers.isEmpty());
        verify(lookUpDTTwin, never()).getEDCOffer(any(), any());
    }

    private static Offer offer(String assetId) {
        return Offer.builder()
                .connectorId(CONNECTOR_ID)
                .connectorOfferUrl(RECIPIENT_URL)
                .offerId("offer-1")
                .assetId(assetId)
                .build();
    }

    private static EDRCachedResponse completedEdr(String assetId) {
        return EDRCachedResponse.builder()
                .assetId(assetId)
                .agreementId("agreement-1")
                .transferProcessId("transfer-1")
                .contractNegotiationId("negotiation-1")
                .build();
    }

    private static EDRCachedByIdResponse authorizationToken() {
        return EDRCachedByIdResponse.builder()
                .authorization("Bearer token")
                .endpoint(DOWNLOAD_ENDPOINT)
                .build();
    }

    private static QueryDataOfferRequest queryDataOfferRequest(String assetId, String connectorOfferUrl,
            String publisher) {
        QueryDataOfferRequest request = new QueryDataOfferRequest();
        request.setAssetId(assetId);
        request.setConnectorOfferUrl(connectorOfferUrl);
        request.setPublisher(publisher);
        return request;
    }
}
