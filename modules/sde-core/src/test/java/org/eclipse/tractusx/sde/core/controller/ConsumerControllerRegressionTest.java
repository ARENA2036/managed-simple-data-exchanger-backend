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

package org.eclipse.tractusx.sde.core.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.eclipse.tractusx.sde.common.exception.ValidationException;
import org.eclipse.tractusx.sde.common.model.Acknowledgement;
import org.eclipse.tractusx.sde.common.model.PagingResponse;
import org.eclipse.tractusx.sde.core.processreport.model.ConsumerDownloadHistory;
import org.eclipse.tractusx.sde.core.service.ConsumerService;
import org.eclipse.tractusx.sde.edc.model.request.ConsumerRequest;
import org.eclipse.tractusx.sde.edc.model.request.Offer;
import org.eclipse.tractusx.sde.edc.model.request.QueryDataOfferRequest;
import org.eclipse.tractusx.sde.edc.model.response.QueryDataOfferModel;
import org.eclipse.tractusx.sde.edc.services.ConsumerControlPanelService;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletResponse;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Covers the consumer-facing controller contract for offer search, policy lookup, subscription, and
 * download history.
 *
 * <p>These tests preserve the current defaults for pagination and file type selection, verify that
 * request objects are passed to the correct consumer services, and cover negative validation for
 * missing search criteria. The purpose is to catch regressions in high-value consumer workflows
 * while avoiding live EDC or provider dependencies by using local stubs.</p>
 */
class ConsumerControllerRegressionTest {

	private final ConsumerControlPanelServiceStub consumerControlPanelService = new ConsumerControlPanelServiceStub();
	private final ConsumerServiceStub consumerService = new ConsumerServiceStub();
	private final ConsumerController controller = new ConsumerController(consumerControlPanelService, consumerService);

	@Test
	void queryDataOffersDefaultsPaginationAndDelegatesSearchParameters() throws Exception {
		Object body = controller.queryOnDataOffers(null, "BPNL00000003CML1", "serialpart", null, null).getBody();

		assertThat(body).isEqualTo(consumerControlPanelService.queryResult);
		assertThat(consumerControlPanelService.lastManufacturerPartId).isNull();
		assertThat(consumerControlPanelService.lastBpnNumber).isEqualTo("BPNL00000003CML1");
		assertThat(consumerControlPanelService.lastSubmodel).isEqualTo("serialpart");
		assertThat(consumerControlPanelService.lastOffset).isZero();
		assertThat(consumerControlPanelService.lastLimit).isEqualTo(10);
	}

	@Test
	void queryDataOffersRejectsMissingManufacturerPartIdAndBpnNumberBeforeServiceCall() {
		assertThatThrownBy(() -> controller.queryOnDataOffers(null, null, null, null, null))
				.isInstanceOf(ValidationException.class)
				.hasMessageContaining("manufacturerPartId");

		assertThat(consumerControlPanelService.queryCalls).isZero();
	}

	@Test
	void offerPolicyDetailsDelegatesBodyListToControlPanelService() throws Exception {
		List<QueryDataOfferRequest> request = List.of(new QueryDataOfferRequest());

		Object body = controller.getEDCPolicy(request).getBody();

		assertThat(body).isEqualTo(consumerControlPanelService.policyResult);
		assertThat(consumerControlPanelService.lastPolicyRequest).isSameAs(request);
	}

	@Test
	void subscribeDataOffersDelegatesRequestAndReturnsGeneratedProcessId() {
		ConsumerRequest request = consumerRequest();

		Object body = controller.subscribeDataOffers(request).getBody();

		assertThat(body).isInstanceOf(String.class);
		assertThat(body.toString()).isNotBlank();
		assertThat(consumerControlPanelService.lastSubscribedRequest).isSameAs(request);
		assertThat(consumerControlPanelService.lastSubscribeProcessId).isEqualTo(body);
	}

	@Test
	void subscribeDownloadDataOffersAsyncReturnsServiceAcknowledgement() {
		ConsumerRequest request = consumerRequest();

		Object body = controller.subscribeAndDownloadDataOffersAsync(request).getBody();

		assertThat(body).isEqualTo(consumerService.asyncAcknowledgement);
		assertThat(consumerService.lastAsyncRequest).isSameAs(request);
	}

	@Test
	void downloadDataOffersDefaultsTypeToCsvAndDelegatesResponse() throws Exception {
		MockHttpServletResponse response = new MockHttpServletResponse();

		controller.downloadFileFromEDCUsingifAlreadyTransferStatusCompleted("process-1", "csv", response);

		assertThat(consumerService.lastDownloadProcessId).isEqualTo("process-1");
		assertThat(consumerService.lastDownloadType).isEqualTo("csv");
		assertThat(consumerService.lastDownloadResponse).isSameAs(response);
	}

	@Test
	void viewDownloadHistoryDefaultsPagination() throws Exception {
		Object body = controller.viewConsumerDownloadHistory(null, null).getBody();

		assertThat(body).isEqualTo(consumerService.historyResponse);
		assertThat(consumerService.lastHistoryPage).isZero();
		assertThat(consumerService.lastHistoryPageSize).isEqualTo(10);
	}

	@Test
	void viewDownloadHistoryDetailsDelegatesProcessId() throws Exception {
		Object body = controller.viewConsumerDownloadHistoryDetails("process-1").getBody();

		assertThat(body).isEqualTo(consumerService.historyDetails);
		assertThat(consumerService.lastHistoryDetailsProcessId).isEqualTo("process-1");
	}

	private ConsumerRequest consumerRequest() {
		return ConsumerRequest.builder()
				.offers(List.of(new Offer()))
				.usagePolicies(List.of())
				.build();
	}

	private static final class ConsumerControlPanelServiceStub extends ConsumerControlPanelService {

		private final Set<QueryDataOfferModel> queryResult = new HashSet<>(
				List.of(QueryDataOfferModel.builder().assetId("asset-1").connectorId("connector-1").build()));
		private final List<QueryDataOfferModel> policyResult = List.of(QueryDataOfferModel.builder()
				.offerId("offer-1")
				.build());
		private int queryCalls;
		private String lastManufacturerPartId;
		private String lastBpnNumber;
		private String lastSubmodel;
		private Integer lastOffset;
		private Integer lastLimit;
		private List<QueryDataOfferRequest> lastPolicyRequest;
		private ConsumerRequest lastSubscribedRequest;
		private String lastSubscribeProcessId;

		private ConsumerControlPanelServiceStub() {
			super(null, null, null, null, null, null, null, null);
		}

		@Override
		public Set<QueryDataOfferModel> queryOnDataOffers(String manufacturerPartId, String searchBpnNumber,
				String submodel, Integer offset, Integer limit) {
			queryCalls++;
			lastManufacturerPartId = manufacturerPartId;
			lastBpnNumber = searchBpnNumber;
			lastSubmodel = submodel;
			lastOffset = offset;
			lastLimit = limit;
			return queryResult;
		}

		@Override
		public List<QueryDataOfferModel> getEDCPolicy(List<QueryDataOfferRequest> queryDataOfferRequest) {
			lastPolicyRequest = queryDataOfferRequest;
			return policyResult;
		}

		@Override
		public void subscribeDataOffers(ConsumerRequest consumerRequest, String processId) {
			lastSubscribedRequest = consumerRequest;
			lastSubscribeProcessId = processId;
		}
	}

	private static final class ConsumerServiceStub extends ConsumerService {

		private final Acknowledgement asyncAcknowledgement = Acknowledgement.builder().id("async-process").build();
		private final PagingResponse historyResponse = PagingResponse.builder().page(0).pageSize(10).build();
		private final ConsumerDownloadHistory historyDetails = ConsumerDownloadHistory.builder()
				.processId("process-1")
				.build();
		private ConsumerRequest lastAsyncRequest;
		private String lastDownloadProcessId;
		private String lastDownloadType;
		private MockHttpServletResponse lastDownloadResponse;
		private Integer lastHistoryPage;
		private Integer lastHistoryPageSize;
		private String lastHistoryDetailsProcessId;

		private ConsumerServiceStub() {
			super(null, null, null, null, null, null, null, new ObjectMapper());
		}

		@Override
		public Acknowledgement subscribeAndDownloadDataOffersAsync(ConsumerRequest consumerRequest) {
			lastAsyncRequest = consumerRequest;
			return asyncAcknowledgement;
		}

		@Override
		public void downloadFileFromEDCUsingifAlreadyTransferStatusCompleted(String referenceProcessId, String type,
				jakarta.servlet.http.HttpServletResponse response) {
			lastDownloadProcessId = referenceProcessId;
			lastDownloadType = type;
			lastDownloadResponse = (MockHttpServletResponse) response;
		}

		@Override
		public PagingResponse viewDownloadHistory(Integer page, Integer pageSize) {
			lastHistoryPage = page;
			lastHistoryPageSize = pageSize;
			return historyResponse;
		}

		@Override
		public ConsumerDownloadHistory viewConsumerDownloadHistoryDetails(String processId) {
			lastHistoryDetailsProcessId = processId;
			return historyDetails;
		}
	}
}
