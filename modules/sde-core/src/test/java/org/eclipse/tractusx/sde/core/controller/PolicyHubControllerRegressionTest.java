/********************************************************************************
 * Copyright (c) 2026 ARENA2036 e.V.
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

package org.eclipse.tractusx.sde.core.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.List;

import org.eclipse.tractusx.sde.policyhub.handler.IPolicyHubProxyService;
import org.eclipse.tractusx.sde.policyhub.model.request.PolicyContentRequest;
import org.eclipse.tractusx.sde.policyhub.model.response.PolicyTypeResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Regression tests for the Policy Hub proxy controller.
 *
 * <p>The controller is intentionally thin but important: it forwards policy attribute, type, and
 * content requests to the Policy Hub proxy service. These tests verify required request parameters,
 * service delegation, negative binding behavior, and the currently observed POST body binding
 * contract. Keeping this documented in tests makes future controller annotation or request-model
 * changes explicit instead of accidental.</p>
 */
class PolicyHubControllerRegressionTest {

	private final ObjectMapper mapper = new ObjectMapper();
	private PolicyHubProxyServiceStub policyHubProxyService;
	private MockMvc mockMvc;

	@BeforeEach
	void setUp() {
		PolicyHubController controller = new PolicyHubController();
		policyHubProxyService = new PolicyHubProxyServiceStub();
		ReflectionTestUtils.setField(controller, "policyHubProxyService", policyHubProxyService);
		mockMvc = MockMvcBuilders.standaloneSetup(controller).build();
	}

	@Test
	void policyAttributesReturnsAttributesFromService() throws Exception {
		mockMvc.perform(get("/policy-hub/policy-attributes"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0]").value("BusinessPartnerNumber"));
	}

	@Test
	void policyTypesPassesQueryParametersToService() throws Exception {
		mockMvc.perform(get("/policy-hub/policy-types")
				.param("type", "ACCESS")
				.param("useCase", "sde"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$[0].technicalKey").value("BusinessPartnerNumber"));

		assertThat(policyHubProxyService.lastPolicyType).isEqualTo("ACCESS");
		assertThat(policyHubProxyService.lastUseCase).isEqualTo("sde");
	}

	@Test
	void policyContentPassesRequiredAndOptionalQueryParametersToService() throws Exception {
		mockMvc.perform(get("/policy-hub/policy-content")
				.param("useCase", "sde")
				.param("type", "ACCESS")
				.param("credential", "BusinessPartnerNumber")
				.param("operatorId", "eq")
				.param("value", "BPNL00000003CML1"))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.source").value("query"));

		assertThat(policyHubProxyService.lastCredential).isEqualTo("BusinessPartnerNumber");
		assertThat(policyHubProxyService.lastOperatorId).isEqualTo("eq");
		assertThat(policyHubProxyService.lastValue).isEqualTo("BPNL00000003CML1");
	}

	@Test
	void policyContentRejectsMissingRequiredTypeBeforeCallingService() throws Exception {
		mockMvc.perform(get("/policy-hub/policy-content")
				.param("credential", "BusinessPartnerNumber")
				.param("operatorId", "eq"))
				.andExpect(status().isBadRequest());

		assertThat(policyHubProxyService.queryContentCalls).isZero();
	}

	@Test
	void postPolicyContentDocumentsCurrentBodyBindingGap() throws Exception {
		mockMvc.perform(post("/policy-hub/policy-content")
				.contentType(MediaType.APPLICATION_JSON)
				.content("""
						{"policyType":"ACCESS","constraintOperand":"AND"}
						"""))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.source").value("body"));

		assertThat(policyHubProxyService.lastPolicyContentRequest).isNotNull();
		assertThat(policyHubProxyService.lastPolicyContentRequest.getPolicyType()).isNull();
		assertThat(policyHubProxyService.lastPolicyContentRequest.getConstraintOperand()).isNull();
	}

	private final class PolicyHubProxyServiceStub implements IPolicyHubProxyService {

		private String lastPolicyType;
		private String lastUseCase;
		private String lastCredential;
		private String lastOperatorId;
		private String lastValue;
		private int queryContentCalls;
		private PolicyContentRequest lastPolicyContentRequest;

		@Override
		public List<String> getPolicyAttributes() {
			return List.of("BusinessPartnerNumber");
		}

		@Override
		public List<PolicyTypeResponse> getPolicyTypes(String type, String useCase) {
			lastPolicyType = type;
			lastUseCase = useCase;
			return List.of(PolicyTypeResponse.builder().technicalKey("BusinessPartnerNumber").build());
		}

		@Override
		public JsonNode getPolicyContent(String useCase, String type, String credential, String operatorId,
				String value) {
			queryContentCalls++;
			lastUseCase = useCase;
			lastPolicyType = type;
			lastCredential = credential;
			lastOperatorId = operatorId;
			lastValue = value;
			return mapper.createObjectNode().put("source", "query");
		}

		@Override
		public JsonNode getPolicyContent(PolicyContentRequest policyContentRequest) {
			lastPolicyContentRequest = policyContentRequest;
			return mapper.createObjectNode().put("source", "body");
		}
	}
}
