/********************************************************************************
 * Copyright (c) 2026 T-Systems International GmbH
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

package org.eclipse.tractusx.sde.core.csv.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.eclipse.tractusx.sde.common.entities.csv.CsvContent;
import org.eclipse.tractusx.sde.common.entities.csv.RowData;
import org.eclipse.tractusx.sde.common.exception.CsvHandlerUseCaseException;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

class CsvContentJsonConverterTest {

	private final CsvContentJsonConverter converter = new CsvContentJsonConverter(new ObjectMapper());

	@Test
	void convertMapsHeaderColumnsToJsonKeys() throws CsvHandlerUseCaseException {
		CsvContent csvContent = csvContent(List.of("manufacturerId", "partInstanceId"),
				List.of(new RowData(2, "BPNL00000003CML1;urn:uuid:123"),
						new RowData(3, "BPNL00000003CML2;urn:uuid:456")));

		List<ObjectNode> result = converter.convert(csvContent);

		assertEquals(2, result.size());
		assertEquals("BPNL00000003CML1", result.get(0).get("manufacturerId").asText());
		assertEquals("urn:uuid:123", result.get(0).get("partInstanceId").asText());
		assertEquals("BPNL00000003CML2", result.get(1).get("manufacturerId").asText());
		assertEquals("urn:uuid:456", result.get(1).get("partInstanceId").asText());
	}

	@Test
	void convertRowPreservesEmptyTrailingFields() throws CsvHandlerUseCaseException {
		ObjectNode result = converter.convertRow(List.of("manufacturerId", "optionalField"),
				new RowData(2, "BPNL00000003CML1;"));

		assertEquals("BPNL00000003CML1", result.get("manufacturerId").asText());
		assertEquals("", result.get("optionalField").asText());
	}

	@Test
	void convertRowThrowsExceptionWhenRowFieldCountDiffersFromHeaderCount() {
		CsvHandlerUseCaseException exception = assertThrows(CsvHandlerUseCaseException.class,
				() -> converter.convertRow(List.of("manufacturerId", "partInstanceId"),
						new RowData(2, "BPNL00000003CML1")));

		assertEquals("RowPosition: 2 | Colomn: 0 | Description: This row has the wrong amount of fields BPNL00000003CML1",
				exception.getMessage());
	}

	private CsvContent csvContent(List<String> columns, List<RowData> rows) {
		CsvContent csvContent = new CsvContent();
		csvContent.setColumns(columns);
		csvContent.setRows(rows);
		return csvContent;
	}
}
