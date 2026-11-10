/********************************************************************************
 * Copyright (c) 2022,2024 T-Systems International GmbH
 * Copyright (c) 2026 ARENA2036 e.V.
 * Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.sde.edc.model.contractoffers;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import com.fasterxml.jackson.databind.node.ObjectNode;

class ContractOfferRequestFactoryTest {

	private static final String PATTERN = "did:web:wallet.example.com:%s";

	@Test
	void mapsBpnToDidWhenPatternIsSet() {
		assertThat(ContractOfferRequestFactory.toCounterPartyId("BPNL000000000065", PATTERN))
				.isEqualTo("did:web:wallet.example.com:BPNL000000000065");
	}

	@Test
	void keepsDidAsIs() {
		String did = "did:web:other.example.com:BPNL000000000065";
		assertThat(ContractOfferRequestFactory.toCounterPartyId(did, PATTERN)).isEqualTo(did);
	}

	@Test
	void keepsBpnWhenNoPatternConfigured() {
		assertThat(ContractOfferRequestFactory.toCounterPartyId("BPNL000000000065", "")).isEqualTo("BPNL000000000065");
		assertThat(ContractOfferRequestFactory.toCounterPartyId("BPNL000000000065", null)).isEqualTo("BPNL000000000065");
	}

	@Test
	void catalogRequestUsesDid() {
		ContractOfferRequestFactory factory = new ContractOfferRequestFactory();
		ReflectionTestUtils.setField(factory, "counterPartyDidPattern", PATTERN);

		ObjectNode request = factory.getContractOfferRequest("https://provider.example.com/api/v1/dsp",
				"BPNL000000000065", 100, 0, null);

		assertThat(request.get("counterPartyId").asText()).isEqualTo("did:web:wallet.example.com:BPNL000000000065");
	}
}
