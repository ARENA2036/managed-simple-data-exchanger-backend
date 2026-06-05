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

package org.eclipse.tractusx.sde.core.utils;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

/**
 * Documents the current persistence conversion contract for simple string lists.
 *
 * <p>The backend stores several list-like fields in a single database column. These tests verify the
 * exact comma-joining and comma-splitting behavior, including null and empty-value edge cases, so a
 * future refactoring of entities or converters does not silently change how existing rows are read or
 * written. The tests intentionally cover both normal values and boundary inputs because these
 * converters run implicitly through JPA and failures would otherwise appear far away from the real
 * cause.</p>
 */
class ListToStringConverterTest {

	private final ListToStringConverter converter = new ListToStringConverter();

	@Test
	void convertToDatabaseColumnJoinsValuesWithComma() {
		assertThat(converter.convertToDatabaseColumn(List.of("a", "b", "c"))).isEqualTo("a,b,c");
	}

	@Test
	void convertToDatabaseColumnReturnsNullForNullInput() {
		assertThat(converter.convertToDatabaseColumn(null)).isNull();
	}

	@Test
	void convertToEntityAttributeSplitsCommaSeparatedValue() {
		assertThat(converter.convertToEntityAttribute("a,b,c")).containsExactly("a", "b", "c");
	}

	@Test
	void convertToEntityAttributeReturnsEmptyListForNullInput() {
		assertThat(converter.convertToEntityAttribute(null)).isEmpty();
	}

	@Test
	void convertToEntityAttributeDocumentsEmptyDatabaseValue() {
		assertThat(converter.convertToEntityAttribute("")).containsExactly("");
	}
}
