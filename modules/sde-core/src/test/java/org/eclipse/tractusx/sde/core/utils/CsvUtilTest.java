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

import java.nio.charset.StandardCharsets;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;

/**
 * Covers the low-level CSV response helper used by download endpoints.
 *
 * <p>The tests verify that generated CSV data uses the expected semicolon delimiter, handles empty
 * input predictably, and creates an HTTP attachment response with the headers clients rely on. This
 * is intentionally tested without Spring context or external systems because the behavior is pure
 * formatting and should remain fast, deterministic, and easy to diagnose during refactoring.</p>
 */
class CsvUtilTest {

	@Test
	void writeCsvWritesRowsWithSemicolonDelimiter() throws Exception {
		String csv = new String(CsvUtil.writeCsv(List.of(
				List.of("manufacturerId", "partInstanceId"),
				List.of("BPNL00000003CML1", "urn:uuid:123"))).readAllBytes(), StandardCharsets.UTF_8);

		assertThat(csv).contains("manufacturerId;partInstanceId");
		assertThat(csv).contains("BPNL00000003CML1;urn:uuid:123");
	}

	@Test
	void writeCsvWritesEmptyInputAsEmptyStream() throws Exception {
		assertThat(CsvUtil.writeCsv(List.of()).readAllBytes()).isEmpty();
	}

	@Test
	void generateCsvReturnsAttachmentResponse() throws Exception {
		ResponseEntity<Resource> response = new CsvUtil().generateCSV("sample.csv",
				List.of(List.of("header"), List.of("value")));

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getHeaders().getFirst(HttpHeaders.CONTENT_DISPOSITION))
				.isEqualTo("attachment; filename=sample.csv");
		assertThat(response.getHeaders().getContentType().toString()).isEqualTo("application/csv");
		assertThat(response.getBody()).isNotNull();
		assertThat(new String(response.getBody().getInputStream().readAllBytes(), StandardCharsets.UTF_8))
				.contains("header")
				.contains("value");
	}
}
