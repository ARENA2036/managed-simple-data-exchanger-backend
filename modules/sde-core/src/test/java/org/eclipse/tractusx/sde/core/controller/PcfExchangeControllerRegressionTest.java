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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.eclipse.tractusx.sde.common.model.PagingResponse;
import org.eclipse.tractusx.sde.edc.model.request.ConsumerRequest;
import org.eclipse.tractusx.sde.pcfexchange.enums.PCFRequestStatusEnum;
import org.eclipse.tractusx.sde.pcfexchange.enums.PCFTypeEnum;
import org.eclipse.tractusx.sde.pcfexchange.request.PcfRequestModel;
import org.eclipse.tractusx.sde.pcfexchange.service.IPCFExchangeService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * Regression tests for the PCF exchange controller surface.
 *
 * <p>The PCF endpoints coordinate provider and consumer product-carbon-footprint flows and depend on
 * request headers, query parameters, and service delegation being stable. These tests document the
 * current HTTP-level behavior for happy paths and negative paths so refactorings in the PCF module or
 * controller mappings do not silently alter the integration contract expected by callers.</p>
 */
class PcfExchangeControllerRegressionTest {

	private PcfExchangeServiceStub pcfExchangeService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		pcfExchangeService = new PcfExchangeServiceStub();
		mockMvc = MockMvcBuilders.standaloneSetup(new PcfExchangeController(pcfExchangeService)).build();
	}

	@Test
	void getProductPcfAcceptsRequestAndPassesHeaderValuesToService() throws Exception {
		mockMvc.perform(get("/pcf/productIds/{productId}", "product-123")
				.header("Edc-Bpn", "BPNL00000003CML1")
				.param("requestId", "request-123")
				.param("message", "please provide pcf"))
				.andExpect(status().isAccepted())
				.andExpect(jsonPath("$.msg").value("PCF request accepted"));

		assertThat(pcfExchangeService.savedRequestId).isEqualTo("request-123");
		assertThat(pcfExchangeService.savedProductId).isEqualTo("product-123");
		assertThat(pcfExchangeService.savedBpnNumber).isEqualTo("BPNL00000003CML1");
		assertThat(pcfExchangeService.savedMessage).isEqualTo("please provide pcf");
	}

	@Test
	void getProductPcfRejectsMissingEdcBpnHeaderBeforeCallingService() throws Exception {
		mockMvc.perform(get("/pcf/productIds/{productId}", "product-123")
				.param("requestId", "request-123"))
				.andExpect(status().isBadRequest());

		assertThat(pcfExchangeService.savedRequestId).isNull();
	}

	@Test
	void uploadPcfSubmodelPassesBodyAndOptionalRequestMetadataToService() throws Exception {
		mockMvc.perform(put("/pcf/productIds/{productId}", "product-123")
				.header("Edc-Bpn", "BPNL00000003CML1")
				.param("requestId", "request-123")
				.param("message", "pcf response")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"id":"pcf-1","productId":"product-123"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.msg").value("PCF response recieved"));

		assertThat(pcfExchangeService.receivedProductId).isEqualTo("product-123");
		assertThat(pcfExchangeService.receivedBpnNumber).isEqualTo("BPNL00000003CML1");
		assertThat(pcfExchangeService.receivedRequestId).isEqualTo("request-123");
		assertThat(pcfExchangeService.receivedMessage).isEqualTo("pcf response");
		assertThat(pcfExchangeService.receivedPcfData.get("id").asText()).isEqualTo("pcf-1");
	}

	@Test
	void uploadPcfSubmodelRejectsMissingBodyBeforeCallingService() throws Exception {
		mockMvc.perform(put("/pcf/productIds/{productId}", "product-123")
				.header("Edc-Bpn", "BPNL00000003CML1")
				.contentType(MediaType.APPLICATION_JSON))
				.andExpect(status().isBadRequest());

		assertThat(pcfExchangeService.receivedProductId).isNull();
	}

	private static final class PcfExchangeServiceStub implements IPCFExchangeService {

		private String savedRequestId;
		private String savedProductId;
		private String savedBpnNumber;
		private String savedMessage;
		private String receivedProductId;
		private String receivedBpnNumber;
		private String receivedRequestId;
		private String receivedMessage;
		private JsonNode receivedPcfData;

		@Override
		public String actionOnPcfRequestAndSendNotificationToConsumer(PcfRequestModel pcfRequestModel) {
			return "accepted";
		}

		@Override
		public Object requestForPcfDataExistingOffer(String productId, ConsumerRequest consumerRequest) {
			return "requested";
		}

		@Override
		public PcfRequestModel savePcfRequestData(String requestId, String productId, String bpnNumber,
				String message) {
			this.savedRequestId = requestId;
			this.savedProductId = productId;
			this.savedBpnNumber = bpnNumber;
			this.savedMessage = message;
			return new PcfRequestModel();
		}

		@Override
		public PagingResponse getPcfData(PCFRequestStatusEnum status, PCFTypeEnum type, Integer page,
				Integer pageSize) {
			return PagingResponse.builder().page(page).pageSize(pageSize).build();
		}

		@Override
		public void recievedPCFData(String productId, String bpnNumber, String requestId, String message,
				JsonNode pcfData) {
			this.receivedProductId = productId;
			this.receivedBpnNumber = bpnNumber;
			this.receivedRequestId = requestId;
			this.receivedMessage = message;
			this.receivedPcfData = pcfData;
		}

		@Override
		public Object viewForPcfDataOffer(String requestId) {
			return new PcfRequestModel();
		}

		@Override
		public Object requestForPcfNotExistDataOffer(PcfRequestModel pcfRequestModel) {
			return "requested";
		}
	}
}
