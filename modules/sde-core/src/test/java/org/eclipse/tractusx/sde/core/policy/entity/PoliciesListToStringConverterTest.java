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

package org.eclipse.tractusx.sde.core.policy.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;

import org.eclipse.tractusx.sde.common.entities.Policies;
import org.junit.jupiter.api.Test;

/**
 * Verifies the JSON column conversion used for policy lists.
 *
 * <p>Policy data is stored as serialized JSON and later reconstructed into domain model objects.
 * These tests lock down serialization, deserialization, null handling, empty-list behavior, and the
 * failure mode for invalid JSON. This gives refactorings around policy persistence a fast regression
 * signal before they can corrupt stored access or usage policy definitions.</p>
 */
class PoliciesListToStringConverterTest {

	private final PoliciesListToStringConverter converter = new PoliciesListToStringConverter();

	@Test
	void convertToDatabaseColumnSerializesPolicyListAsJson() {
		String json = converter.convertToDatabaseColumn(List.of(policy()));

		assertThat(json).contains("\"technicalKey\":\"BusinessPartnerNumber\"");
		assertThat(json).contains("\"operator\":\"eq\"");
		assertThat(json).contains("\"value\":[\"BPNL00000003CML1\"]");
	}

	@Test
	void convertToDatabaseColumnReturnsNullForNullInput() {
		assertThat(converter.convertToDatabaseColumn(null)).isNull();
	}

	@Test
	void convertToEntityAttributeDeserializesPolicyJson() {
		List<Policies> policies = converter.convertToEntityAttribute("""
				[{"technicalKey":"BusinessPartnerNumber","operator":"eq","value":["BPNL00000003CML1"]}]
				""");

		assertThat(policies).hasSize(1);
		assertThat(policies.get(0).getTechnicalKey()).isEqualTo("BusinessPartnerNumber");
		assertThat(policies.get(0).getOperator()).isEqualTo("eq");
		assertThat(policies.get(0).getValue()).containsExactly("BPNL00000003CML1");
	}

	@Test
	void convertToEntityAttributeReturnsEmptyListForNullInput() {
		assertThat(converter.convertToEntityAttribute(null)).isEmpty();
	}

	@Test
	void convertToEntityAttributeThrowsForInvalidJson() {
		assertThatThrownBy(() -> converter.convertToEntityAttribute("not-json"))
				.isInstanceOf(Exception.class);
	}

	private Policies policy() {
		return Policies.builder()
				.technicalKey("BusinessPartnerNumber")
				.operator("eq")
				.value(List.of("BPNL00000003CML1"))
				.build();
	}
}
