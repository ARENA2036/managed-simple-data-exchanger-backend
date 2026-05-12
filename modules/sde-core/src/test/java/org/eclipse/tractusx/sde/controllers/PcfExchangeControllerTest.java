/********************************************************************************
 * Copyright (c) 2024 T-Systems International GmbH
 * Copyright (c) 2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.sde.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.UUID;

import org.eclipse.tractusx.sde.core.controller.PcfExchangeController;
import org.eclipse.tractusx.sde.edc.model.request.ConsumerRequest;
import org.eclipse.tractusx.sde.pcfexchange.request.PcfRequestModel;
import org.eclipse.tractusx.sde.pcfexchange.service.IPCFExchangeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ContextConfiguration(classes = { PcfExchangeController.class })
@ExtendWith(SpringExtension.class)
class PcfExchangeControllerTest {

	@MockitoBean
    private IPCFExchangeService pcfExchangeService;
    
	@Autowired
	private PcfExchangeController pcfExchangeController;
	
	
	@Test
	void testGetPcfConsumerDataSuccess() throws Exception {
		MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/pcf/{type}/requests","CONSUMER")
				.param("status", "")
				.param("offset", String.valueOf(0))
				.param("maxLimit", String.valueOf(10));
		ResultActions actualPerformResult = MockMvcBuilders.standaloneSetup(pcfExchangeController).build()
				.perform(requestBuilder);
		actualPerformResult.andExpect(MockMvcResultMatchers.status().is(200));
	}
	
	@Test
	void testGetPcfProviderDataSuccess() throws Exception {
		MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/pcf/{type}/requests","PROVIDER")
				.param("status", "REQUESTED")
				.param("offset", String.valueOf(0))
				.param("maxLimit", String.valueOf(10));
		ResultActions actualPerformResult = MockMvcBuilders.standaloneSetup(pcfExchangeController).build()
				.perform(requestBuilder);
		actualPerformResult.andExpect(MockMvcResultMatchers.status().is(200));
	}
	
	@Test
	void testGetPcfProviderDataFailure() throws Exception {
		MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/pcf/{type}/requests","")
				.param("status", "REQUESTED")
				.param("offset", String.valueOf(0))
				.param("maxLimit", String.valueOf(10));
		ResultActions actualPerformResult = MockMvcBuilders.standaloneSetup(pcfExchangeController).build()
				.perform(requestBuilder);
		actualPerformResult.andExpect(MockMvcResultMatchers.status().is(404));
	}
	
	@Test
	void testGetPcfByProductSuccess() throws Exception {
		MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/pcf/productIds/{productId}","test_product")
				.header("Edc-Bpn", "BPNL001000TS0100")
				.param("requestId", UUID.randomUUID().toString())
				.param("message", "This is test request");
		ResultActions actualPerformResult = MockMvcBuilders.standaloneSetup(pcfExchangeController).build()
				.perform(requestBuilder);
		actualPerformResult.andExpect(MockMvcResultMatchers.status().isAccepted());
	}
	
	@Test
	void testUploadPcfSubmodelSuccess() throws Exception {
		MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.put("/pcf/productIds/{productId}","test_product")
				.header("Edc-Bpn", "BPNL001000TS0100")
				.param("requestId", UUID.randomUUID().toString())
				.param("message", "This is test request")
				.contentType("application/json")
				.content(getPCFJsonResponse());
		ResultActions actualPerformResult = MockMvcBuilders.standaloneSetup(pcfExchangeController).build()
				.perform(requestBuilder);
		actualPerformResult.andExpect(MockMvcResultMatchers.status().isOk());
	}
	
	@Test
	void testUploadPcfSubmodelSuccessForceUpdate() throws Exception {
		MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.put("/pcf/productIds/{productId}","test_product")
				.header("Edc-Bpn", "BPNL001000TS0100")
				.contentType("application/json")
				.content(getPCFJsonResponse());
		ResultActions actualPerformResult = MockMvcBuilders.standaloneSetup(pcfExchangeController).build()
				.perform(requestBuilder);
		actualPerformResult.andExpect(MockMvcResultMatchers.status().isOk());
	}
	
	@Test
	void testUploadPcfSubmodelFailure() throws Exception {
		MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.put("/pcf/productIds/{productId}","test_product")
				.header("Edc-Bpn", "")
				.param("requestId", UUID.randomUUID().toString())
				.param("message", "This is test request");
		ResultActions actualPerformResult = MockMvcBuilders.standaloneSetup(pcfExchangeController).build()
				.perform(requestBuilder);
		actualPerformResult.andExpect(MockMvcResultMatchers.status().is(400));
	}
	
	@Test
	void testRequestForPcfDataOffer() throws Exception {
		MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post("/pcf/request/{productId}","test_product")
				.contentType("application/json")
				.content(new ConsumerRequest().toString());
		ResultActions actualPerformResult = MockMvcBuilders.standaloneSetup(pcfExchangeController).build()
				.perform(requestBuilder);
		actualPerformResult.andExpect(MockMvcResultMatchers.status().is(400));
	}
	
	@Test
	void testViewForPcfDataOfferSuccess() throws Exception {
		MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.get("/pcf/request/{requestId}",UUID.randomUUID().toString());
		ResultActions actualPerformResult = MockMvcBuilders.standaloneSetup(pcfExchangeController).build()
				.perform(requestBuilder);
		actualPerformResult.andExpect(MockMvcResultMatchers.status().is(200));
	}
	
	@Test
	void testActionOnPcfRequestAndSendNotificationToConsumerSuccess() throws Exception {
		
		when(pcfExchangeService.actionOnPcfRequestAndSendNotificationToConsumer(any()))
		.thenReturn(new String());
		
		MockHttpServletRequestBuilder requestBuilder = MockMvcRequestBuilders.post("/pcf/actionsonrequest")
				.contentType("application/json")
				.content(new PcfRequestModel().toString());
		ResultActions actualPerformResult = MockMvcBuilders.standaloneSetup(pcfExchangeController).build()
				.perform(requestBuilder);
		actualPerformResult.andExpect(MockMvcResultMatchers.status().is(400));
	}
	
	
	
	private String getPCFJsonResponse() {
		return """
				{
				    "row_data": [
				        {
				            "id": "3893bb5d-da16-4dc1-9185-11d97476c7a7",
				            "specVersion": "2.0.1-20230314",
				            "partialFullPcf": "Cradle-to-gate",
				            "precedingPfId": "3893bb5d-da16-4dc1-9185-11d97476c7b7",
				            "version": 0,
				            "created": "2022-05-22T21:47:32Z",
				            "extWBCSD_pfStatus": "Active",
				            "validityPeriodStart": "",
				            "validityPeriodEnd": "",
				            "comment": "Comment for version 42.",
				            "pcfLegalStatement": "This PCF (Product Carbon Footprint) is for information purposes only. It is based upon the standards mentioned above.",
				            "companyName": "My Corp",
				            "companyId": "urn:uuid:51131FB5-42A2-4267-A402-0ECFEFAD16A9",
				            "productDescription": "Ethanol, 95% solution",
				            "productId": "urn:gtin:47123450605077",
				            "extWBCSD_productCodeCpc": "011-99000",
				            "productName": "My Product Name",
				            "declaredUnit": "liter",
				            "unitaryProductAmount": 1000.0,
				            "productMassPerDeclaredUnit": 0.456,
				            "exemptedEmissionsPercent": 0.0,
				            "exemptedEmissionsDescription": "No exemption",
				            "extWBCSD_packagingEmissionsIncluded": "true",
				            "boundaryProcessesDescription": "Electricity consumption included as an input in the production phase",
				            "geographyCountrySubdivision": "US-NY",
				            "geographyCountry": "DE",
				            "geographyRegionOrSubregion": "Africa",
				            "referencePeriodStart": "2022-01-01T00:00:01Z",
				            "referencePeriodEnd": "2022-12-31T23:59:59Z",
				            "crossSectoralStandard": "GHG Protocol Product standard",
				            "extWBCSD_operator": "PEF",
				            "ruleName": "urn:tfs-initiative.com:PCR:The Product Carbon Footprint Guideline for the Chemical Industry:version:v2.0",
				            "extWBCSD_otherOperatorName": "NSF",
				            "extWBCSD_characterizationFactors": "AR5",
				            "extWBCSD_allocationRulesDescription": "In accordance with Catena-X PCF Rulebook",
				            "extTFS_allocationWasteIncineration": "cut-off",
				            "primaryDataShare": 56.12,
				            "secondaryEmissionFactorSource": "ecoinvent 3.8",
				            "coveragePercent": 100,
				            "technologicalDQR": 2.0,
				            "temporalDQR": 2.0,
				            "geographicalDQR": 2.0,
				            "completenessDQR": 2.0,
				            "reliabilityDQR": 2.0,
				            "pcfExcludingBiogenic": 2.0,
				            "pcfIncludingBiogenic": 1.0,
				            "fossilGhgEmissions": 0.5,
				            "biogenicCarbonEmissionsOtherThanCO2": 1.0,
				            "biogenicCarbonWithdrawal": 0.0,
				            "dlucGhgEmissions": 0.4,
				            "extTFS_luGhgEmissions": 0.3,
				            "aircraftGhgEmissions": 0.0,
				            "extWBCSD_packagingGhgEmissions": 0,
				            "distributionStagePcfExcludingBiogenic": 1.5,
				            "distributionStagePcfIncludingBiogenic": 0.0,
				            "distributionStageFossilGhgEmissions": 0.5,
				            "distributionStageBiogenicCarbonEmissionsOtherThanCO2": 1.0,
				            "distributionStageBiogenicCarbonWithdrawal": 0.5,
				            "extTFS_distributionStageDlucGhgEmissions": 1.0,
				            "extTFS_distributionStageLuGhgEmissions": 1.1,
				            "carbonContentTotal": 2.5,
				            "extWBCSD_fossilCarbonContent": 0.1,
				            "carbonContentBiogenic": 0.0,
				            "assetLifeCyclePhase": "AsPlanned"
				        }
				    ],
				    "access_policies": [
				        {
				            "technicalKey": "BusinessPartnerNumber",
				            "value": [
				                "BPNL001000TS0100"
				            ]
				        },
				        {
				            "technicalKey": "Membership",
				            "value": [
				                "active"
				            ]
				        },
				        {
				            "technicalKey": "companyRole.dismantler",
				            "value": [
				                "active"
				            ]
				        }
				    ],
				    "usage_policies": [
				        {
				            "technicalKey": "Membership",
				            "value": [
				                "active"
				            ]
				        },
				        {
				            "technicalKey": "companyRole.dismantler",
				            "value": [
				                "active"
				            ]
				        },
				        {
				            "technicalKey": "FrameworkAgreement.pcf",
				            "value": [
				                "active:v1.0.0"
				            ]
				        },
				        {
				            "technicalKey": "FrameworkAgreement.sustainability",
				            "value": [
				                "active:v1.0.0"
				            ]
				        },
				        {
				            "technicalKey": "PURPOSE",
				            "value": []
				        },
				        {
				            "technicalKey": "PURPOSE",
				            "value": []
				        },
				        {
				            "technicalKey": "CUSTOM",
				            "value": []
				        }
				    ]
				}\
				""";
	}
	
}
