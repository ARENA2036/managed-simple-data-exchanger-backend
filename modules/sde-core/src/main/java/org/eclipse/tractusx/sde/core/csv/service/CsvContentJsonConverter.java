/********************************************************************************
 * Copyright (c) 2026 T-Systems International GmbH
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
 * Copyright (c) 2026 ARENA2036 e.V.
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

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

import org.eclipse.tractusx.sde.common.constants.CommonConstants;
import org.eclipse.tractusx.sde.common.entities.csv.CsvContent;
import org.eclipse.tractusx.sde.common.entities.csv.RowData;
import org.eclipse.tractusx.sde.common.exception.CsvHandlerUseCaseException;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

@Component
public class CsvContentJsonConverter {

	private final ObjectMapper objectMapper;

	public CsvContentJsonConverter(ObjectMapper objectMapper) {
		this.objectMapper = objectMapper;
	}

	public List<ObjectNode> convert(CsvContent csvContent) throws CsvHandlerUseCaseException {
		List<ObjectNode> jsonRows = new ArrayList<>();
		for (RowData row : csvContent.getRows()) {
			jsonRows.add(convertRow(csvContent.getColumns(), row));
		}
		return jsonRows;
	}

	public ObjectNode convertRow(List<String> columns, RowData rowData) throws CsvHandlerUseCaseException {
		String[] rowDataFields = rowData.content().split(Pattern.quote(CommonConstants.SEPARATOR), -1);
		if (rowDataFields.length != columns.size()) {
			throw new CsvHandlerUseCaseException(rowData.position(),
					"This row has the wrong amount of fields " + rowData.content());
		}

		ObjectNode rowObject = objectMapper.createObjectNode();
		for (int colomnIndex = 0; colomnIndex < columns.size(); colomnIndex++) {
			rowObject.put(columns.get(colomnIndex), rowDataFields[colomnIndex]);
		}
		return rowObject;
	}
}
