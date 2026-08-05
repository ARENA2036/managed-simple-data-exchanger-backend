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

package org.eclipse.tractusx.sde.core.service;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.eclipse.tractusx.sde.common.entities.Policies;
import org.eclipse.tractusx.sde.common.enums.ProgressStatusEnum;
import org.eclipse.tractusx.sde.common.exception.NoDataFoundException;
import org.eclipse.tractusx.sde.common.model.Acknowledgement;
import org.eclipse.tractusx.sde.common.model.PagingResponse;
import org.eclipse.tractusx.sde.common.model.Submodel;
import org.eclipse.tractusx.sde.core.processreport.entity.ConsumerDownloadHistoryEntity;
import org.eclipse.tractusx.sde.core.processreport.mapper.ConsumerDownloadHistoryMapper;
import org.eclipse.tractusx.sde.core.processreport.model.ConsumerDownloadHistory;
import org.eclipse.tractusx.sde.core.processreport.repository.ConsumerDownloadHistoryRepository;
import org.eclipse.tractusx.sde.edc.entities.request.policies.PolicyConstraintBuilderService;
import org.eclipse.tractusx.sde.edc.facilitator.EDRRequestHelper;
import org.eclipse.tractusx.sde.edc.model.edr.EDRCachedResponse;
import org.eclipse.tractusx.sde.edc.model.request.ConsumerRequest;
import org.eclipse.tractusx.sde.edc.model.request.Offer;
import org.eclipse.tractusx.sde.edc.services.ConsumerControlPanelService;
import org.eclipse.tractusx.sde.edc.services.ContractNegotiationService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockHttpServletResponse;

import jakarta.servlet.http.HttpServletResponse;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class ConsumerServiceTest {

    private static final String CONNECTOR_ID = "provider";
    private static final String CONNECTOR_OFFER_URL = "https://provider.example/api/v1/dsp";

    @Mock
    private ConsumerControlPanelService consumerControlPanelService;

    @Mock
    private SubmodelOrchestartorService submodelOrchestartorService;

    @Mock
    private ConsumerDownloadHistoryRepository consumerDownloadHistoryRepository;

    @Mock
    private ConsumerDownloadHistoryMapper consumerDownloadHistoryMapper;

    @Mock
    private PolicyConstraintBuilderService policyConstraintBuilderService;

    @Mock
    private ContractNegotiationService contractNegotiationService;

    @Mock
    private EDRRequestHelper edrRequestHelper;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private ConsumerService consumerService;

    @BeforeEach
    void setUp() {
        consumerService = new ConsumerService(consumerControlPanelService, submodelOrchestartorService,
                consumerDownloadHistoryRepository, consumerDownloadHistoryMapper, policyConstraintBuilderService,
                contractNegotiationService, edrRequestHelper, objectMapper);
    }

    @Test
    void subscribeAndDownloadDataOffersCompletesHistoryWhenEveryOfferSucceeds() {
        Offer offer = offer("asset-1", "offer-1");
        ConsumerRequest consumerRequest = new ConsumerRequest(List.of(offer), List.of(usagePolicy()), "csv");

        when(policyConstraintBuilderService.getUsagePoliciesConstraints(consumerRequest.getUsagePolicies()))
                .thenReturn(List.of());
        when(consumerControlPanelService.subcribeAndDownloadOffer(eq(offer), anyList(), eq(false), eq("csv")))
                .thenReturn(Map.of("status", "SUCCESS", "edr", completedEdr()));

        Map<String, Object> dataWithValue = consumerService.subscribeAndDownloadDataOffers(consumerRequest,
                "process-1", false);

        assertTrue(dataWithValue.isEmpty());

        ConsumerDownloadHistoryEntity saved = savedHistory(2);
        assertEquals(ProgressStatusEnum.COMPLETED.toString(), saved.getStatus());
        assertEquals(Integer.valueOf(1), saved.getDownloadSuccessed());
        assertEquals(Integer.valueOf(0), saved.getDownloadFailed());
        assertEquals(Integer.valueOf(1), saved.getNumberOfItems());
        assertEquals("process-1", saved.getProcessId());
        assertEquals(CONNECTOR_ID, saved.getConnectorId());
        assertEquals(CONNECTOR_OFFER_URL, saved.getProviderUrl());
        assertNotNull(saved.getStartDate());
        assertNotNull(saved.getEndDate());
        assertNotNull(saved.getOffers());
        assertNotNull(saved.getPolicies());

        assertEquals("SUCCESS", offer.getStatus());
        assertEquals("", offer.getDownloadErrorMsg());
        assertEquals("agreement-1", offer.getAgreementId());
        assertEquals("transfer-1", offer.getTransferProcessId());
    }

    @Test
    void subscribeAndDownloadDataOffersFailsHistoryAndKeepsErrorMessageWhenOfferFails() {
        Offer offer = offer("asset-1", "offer-1");
        ConsumerRequest consumerRequest = new ConsumerRequest(List.of(offer), List.of(usagePolicy()), "csv");

        when(policyConstraintBuilderService.getUsagePoliciesConstraints(consumerRequest.getUsagePolicies()))
                .thenReturn(List.of());
        when(consumerControlPanelService.subcribeAndDownloadOffer(eq(offer), anyList(), eq(false), eq("csv")))
                .thenReturn(Map.of("status", "FAILED", "error", "EDC connector is unreachable"));

        Map<String, Object> dataWithValue = consumerService.subscribeAndDownloadDataOffers(consumerRequest,
                "process-2", false);

        assertTrue(dataWithValue.isEmpty());

        ConsumerDownloadHistoryEntity saved = savedHistory(2);
        assertEquals(ProgressStatusEnum.FAILED.toString(), saved.getStatus());
        assertEquals(Integer.valueOf(0), saved.getDownloadSuccessed());
        assertEquals(Integer.valueOf(1), saved.getDownloadFailed());

        assertEquals(ProgressStatusEnum.FAILED.toString(), offer.getStatus());
        assertEquals("EDC connector is unreachable", offer.getDownloadErrorMsg());
    }

    @Test
    void subscribeAndDownloadDataOffersMarksHistoryPartiallyFailedForMixedResults() {
        Offer succeedingOffer = offer("asset-1", "offer-1");
        Offer failingOffer = offer("asset-2", "offer-2");
        ConsumerRequest consumerRequest = new ConsumerRequest(List.of(succeedingOffer, failingOffer),
                List.of(usagePolicy()), "csv");

        when(policyConstraintBuilderService.getUsagePoliciesConstraints(consumerRequest.getUsagePolicies()))
                .thenReturn(List.of());
        when(consumerControlPanelService.subcribeAndDownloadOffer(eq(succeedingOffer), anyList(), eq(false), eq("csv")))
                .thenReturn(Map.of("status", "SUCCESS"));
        when(consumerControlPanelService.subcribeAndDownloadOffer(eq(failingOffer), anyList(), eq(false), eq("csv")))
                .thenReturn(Map.of("status", "FAILED", "error", "policy mismatch"));

        consumerService.subscribeAndDownloadDataOffers(consumerRequest, "process-3", false);

        ConsumerDownloadHistoryEntity saved = savedHistory(2);
        assertEquals(ProgressStatusEnum.PARTIALLY_FAILED.toString(), saved.getStatus());
        assertEquals(Integer.valueOf(1), saved.getDownloadSuccessed());
        assertEquals(Integer.valueOf(1), saved.getDownloadFailed());
        assertEquals(Integer.valueOf(2), saved.getNumberOfItems());
    }

    @Test
    void subscribeAndDownloadDataOffersCollectsJsonPayloadKeyedByAssetId() {
        Offer offer = offer("asset-1", "offer-1");
        ConsumerRequest consumerRequest = new ConsumerRequest(List.of(offer), List.of(usagePolicy()), "json");

        when(policyConstraintBuilderService.getUsagePoliciesConstraints(consumerRequest.getUsagePolicies()))
                .thenReturn(List.of());
        when(consumerControlPanelService.subcribeAndDownloadOffer(eq(offer), anyList(), eq(true), eq("json")))
                .thenReturn(Map.of("status", "SUCCESS", "data", Map.of("partId", "part-1")));

        Map<String, Object> dataWithValue = consumerService.subscribeAndDownloadDataOffers(consumerRequest,
                "process-4", true);

        assertTrue(dataWithValue.containsKey("asset-1"));
        assertEquals("part-1", ((JsonNode) dataWithValue.get("asset-1")).get("partId").asText());
        assertEquals("SUCCESS", offer.getStatus());
        assertEquals(ProgressStatusEnum.COMPLETED.toString(), savedHistory(2).getStatus());
    }

    @Test
    void subscribeAndDownloadDataOffersCollectsCsvPayloadKeyedBySubmodelWithHeaderRow() {
        Offer offer = offer("asset-1", "offer-1");
        ConsumerRequest consumerRequest = new ConsumerRequest(List.of(offer), List.of(usagePolicy()), "csv");

        Map<String, Object> csvRow = new LinkedHashMap<>();
        csvRow.put("uuid", "urn:uuid:1");
        csvRow.put("part_instance_id", "part-1");

        when(policyConstraintBuilderService.getUsagePoliciesConstraints(consumerRequest.getUsagePolicies()))
                .thenReturn(List.of());
        when(consumerControlPanelService.subcribeAndDownloadOffer(eq(offer), anyList(), eq(true), eq("csv")))
                .thenReturn(Map.of("status", "SUCCESS", "data", csvRow));
        when(submodelOrchestartorService.findSubmodel(List.of("uuid", "part_instance_id")))
                .thenReturn(Submodel.builder().id("serialpart").build());

        Map<String, Object> dataWithValue = consumerService.subscribeAndDownloadDataOffers(consumerRequest,
                "process-5", true);

        List<?> rows = (List<?>) dataWithValue.get("serialpart");
        assertEquals(2, rows.size());
        assertEquals(List.of("uuid", "part_instance_id"), rows.get(0));
        assertEquals(List.of("urn:uuid:1", "part-1"), rows.get(1));
        assertEquals("SUCCESS", offer.getStatus());
    }

    @Test
    void subscribeAndDownloadDataOffersAsyncReturnsGeneratedProcessIdWithoutBlocking() {
        ConsumerRequest consumerRequest = new ConsumerRequest(List.of(), List.of(), "csv");

        Acknowledgement acknowledgement = consumerService.subscribeAndDownloadDataOffersAsync(consumerRequest);

        assertNotNull(acknowledgement.getId());
        assertDoesNotThrow(() -> UUID.fromString(acknowledgement.getId()));
    }

    @Test
    void subscribeAndDownloadDataOffersSynchronousStreamsZippedPayloadToResponse() throws Exception {
        Offer offer = offer("asset-1", "offer-1");
        ConsumerRequest consumerRequest = new ConsumerRequest(List.of(offer), List.of(usagePolicy()), "json");

        when(policyConstraintBuilderService.getUsagePoliciesConstraints(consumerRequest.getUsagePolicies()))
                .thenReturn(List.of());
        when(consumerControlPanelService.subcribeAndDownloadOffer(eq(offer), anyList(), eq(true), eq("json")))
                .thenReturn(Map.of("status", "SUCCESS", "data", Map.of("partId", "part-1")));

        MockHttpServletResponse response = new MockHttpServletResponse();

        consumerService.subscribeAndDownloadDataOffersSynchronous(consumerRequest, response);

        assertEquals("application/zip", response.getContentType());
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertTrue(response.getHeader("Content-Disposition").endsWith("-download.zip"));
        assertEquals(List.of("asset-1.json"), zipEntryNames(response.getContentAsByteArray()));
    }

    @Test
    void downloadFileFromEdcReDownloadsStoredOffersAndCompletesHistory() throws Exception {
        Offer offer = offer("asset-1", "offer-1");
        ConsumerDownloadHistoryEntity entity = ConsumerDownloadHistoryEntity.builder()
                .processId("process-old")
                .connectorId(CONNECTOR_ID)
                .providerUrl(CONNECTOR_OFFER_URL)
                .offers(objectMapper.writeValueAsString(List.of(offer)))
                .build();

        when(consumerDownloadHistoryRepository.findByProcessId("reference-1")).thenReturn(entity);
        when(consumerControlPanelService.downloadFileFromEDCUsingifAlreadyTransferStatusCompleted(List.of("asset-1"),
                "json")).thenReturn(Map.of("asset-1", Map.of("status", "SUCCESS", "data", Map.of("partId", "part-1"),
                        "edr", completedEdr())));

        MockHttpServletResponse response = new MockHttpServletResponse();

        consumerService.downloadFileFromEDCUsingifAlreadyTransferStatusCompleted("reference-1", "json", response);

        assertEquals("application/zip", response.getContentType());
        assertEquals(HttpServletResponse.SC_OK, response.getStatus());
        assertEquals(List.of("asset-1.json"), zipEntryNames(response.getContentAsByteArray()));

        verify(consumerDownloadHistoryRepository, times(2)).save(entity);
        assertEquals(ProgressStatusEnum.COMPLETED.toString(), entity.getStatus());
        assertEquals("reference-1", entity.getReferenceProcessId());
        assertEquals(Integer.valueOf(1), entity.getNumberOfItems());
        assertEquals(Integer.valueOf(1), entity.getDownloadSuccessed());
        assertEquals(Integer.valueOf(0), entity.getDownloadFailed());
        assertNotEquals("process-old", entity.getProcessId());
        assertDoesNotThrow(() -> UUID.fromString(entity.getProcessId()));
    }

    @Test
    void downloadFileFromEdcFailsHistoryWhenStoredOfferCannotBeDownloaded() throws Exception {
        Offer offer = offer("asset-1", "offer-1");
        ConsumerDownloadHistoryEntity entity = ConsumerDownloadHistoryEntity.builder()
                .processId("process-old")
                .connectorId(CONNECTOR_ID)
                .providerUrl(CONNECTOR_OFFER_URL)
                .offers(objectMapper.writeValueAsString(List.of(offer)))
                .build();

        when(consumerDownloadHistoryRepository.findByProcessId("reference-2")).thenReturn(entity);
        when(consumerControlPanelService.downloadFileFromEDCUsingifAlreadyTransferStatusCompleted(List.of("asset-1"),
                "json")).thenReturn(Map.of("asset-1", Map.of("status", "FAILED", "error", "no EDR token available")));

        MockHttpServletResponse response = new MockHttpServletResponse();

        consumerService.downloadFileFromEDCUsingifAlreadyTransferStatusCompleted("reference-2", "json", response);

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertTrue(response.getContentAsString().contains("Unable to process your request, please try again"));

        verify(consumerDownloadHistoryRepository, times(2)).save(entity);
        assertEquals(ProgressStatusEnum.FAILED.toString(), entity.getStatus());
        assertEquals(Integer.valueOf(1), entity.getDownloadFailed());
    }

    @Test
    void downloadFileFromEdcReturnsFailureJsonWhenReferenceProcessIsUnknown() throws Exception {
        when(consumerDownloadHistoryRepository.findByProcessId("missing")).thenReturn(null);

        MockHttpServletResponse response = new MockHttpServletResponse();

        consumerService.downloadFileFromEDCUsingifAlreadyTransferStatusCompleted("missing", "json", response);

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertTrue(response.getContentType().startsWith("application/json"));
        assertTrue(response.getContentAsString().contains("Unable to find data offer in SDE for download"));
        verify(consumerDownloadHistoryRepository, never()).save(any(ConsumerDownloadHistoryEntity.class));
        verify(consumerControlPanelService, never())
                .downloadFileFromEDCUsingifAlreadyTransferStatusCompleted(anyList(), any());
    }

    @Test
    void downloadFileFromEdcReturnsFailureJsonWhenStoredOfferListIsEmpty() throws Exception {
        ConsumerDownloadHistoryEntity entity = ConsumerDownloadHistoryEntity.builder()
                .processId("process-old")
                .offers("[]")
                .build();
        when(consumerDownloadHistoryRepository.findByProcessId("reference-3")).thenReturn(entity);

        MockHttpServletResponse response = new MockHttpServletResponse();

        consumerService.downloadFileFromEDCUsingifAlreadyTransferStatusCompleted("reference-3", "json", response);

        assertEquals(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, response.getStatus());
        assertTrue(response.getContentAsString().contains("Unable to find data offer in SDE for download"));
        verify(consumerDownloadHistoryRepository, never()).save(any(ConsumerDownloadHistoryEntity.class));
    }

    @Test
    void viewDownloadHistoryMapsPageMetadataAndItems() {
        ConsumerDownloadHistoryEntity entity = ConsumerDownloadHistoryEntity.builder()
                .processId("process-1")
                .status(ProgressStatusEnum.COMPLETED.toString())
                .build();
        Page<ConsumerDownloadHistoryEntity> page = new PageImpl<>(List.of(entity), PageRequest.of(0, 10), 1);
        when(consumerDownloadHistoryRepository.findAll(any(Pageable.class))).thenReturn(page);

        ConsumerDownloadHistory history = ConsumerDownloadHistory.builder()
                .processId("process-1")
                .status(ProgressStatusEnum.COMPLETED.toString())
                .build();
        when(consumerDownloadHistoryMapper.mapFromCustom(entity)).thenReturn(history);

        PagingResponse pagingResponse = consumerService.viewDownloadHistory(0, 10);

        assertEquals(0, pagingResponse.getPage());
        assertEquals(page.getSize(), pagingResponse.getPageSize());
        assertEquals(1L, pagingResponse.getTotalItems());
        assertEquals(List.of(history), pagingResponse.getItems());
    }

    @Test
    void viewConsumerDownloadHistoryDetailsReturnsMappedHistory() {
        ConsumerDownloadHistoryEntity entity = ConsumerDownloadHistoryEntity.builder()
                .processId("process-1")
                .status(ProgressStatusEnum.COMPLETED.toString())
                .build();
        when(consumerDownloadHistoryRepository.findByProcessId("process-1")).thenReturn(entity);

        ConsumerDownloadHistory history = ConsumerDownloadHistory.builder()
                .processId("process-1")
                .build();
        when(consumerDownloadHistoryMapper.mapFromCustom(entity)).thenReturn(history);

        assertEquals(history, consumerService.viewConsumerDownloadHistoryDetails("process-1"));
    }

    @Test
    void viewConsumerDownloadHistoryDetailsThrowsForUnknownProcessId() {
        when(consumerDownloadHistoryRepository.findByProcessId("missing")).thenReturn(null);

        assertThrows(NoDataFoundException.class, () -> consumerService.viewConsumerDownloadHistoryDetails("missing"));
    }

    private ConsumerDownloadHistoryEntity savedHistory(int expectedSaveCount) {
        ArgumentCaptor<ConsumerDownloadHistoryEntity> captor = ArgumentCaptor
                .forClass(ConsumerDownloadHistoryEntity.class);
        verify(consumerDownloadHistoryRepository, times(expectedSaveCount)).save(captor.capture());
        return captor.getAllValues().get(expectedSaveCount - 1);
    }

    private static List<String> zipEntryNames(byte[] zipContent) throws Exception {
        List<String> names = new ArrayList<>();
        try (ZipInputStream zip = new ZipInputStream(new ByteArrayInputStream(zipContent))) {
            ZipEntry entry = zip.getNextEntry();
            while (entry != null) {
                names.add(entry.getName());
                entry = zip.getNextEntry();
            }
        }
        return names;
    }

    private static Offer offer(String assetId, String offerId) {
        return Offer.builder()
                .connectorId(CONNECTOR_ID)
                .connectorOfferUrl(CONNECTOR_OFFER_URL)
                .offerId(offerId)
                .assetId(assetId)
                .build();
    }

    private static EDRCachedResponse completedEdr() {
        return EDRCachedResponse.builder()
                .assetId("asset-1")
                .agreementId("agreement-1")
                .transferProcessId("transfer-1")
                .build();
    }

    private static Policies usagePolicy() {
        return Policies.builder()
                .technicalKey("BusinessPartnerNumber")
                .value(List.of("BPNL000000000001"))
                .build();
    }
}
