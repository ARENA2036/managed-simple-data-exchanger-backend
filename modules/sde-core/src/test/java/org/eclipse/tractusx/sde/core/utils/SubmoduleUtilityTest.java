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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Map;

import org.eclipse.tractusx.sde.common.constants.SubmoduleCommonColumnsConstant;
import org.eclipse.tractusx.sde.common.model.Submodel;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonParser;

/**
 * Protects schema-to-column extraction for submodel CSV and persistence handling.
 *
 * <p>Submodel processing derives CSV headers and database columns from dynamic JSON schema
 * definitions. These tests verify that schema properties, auto-populated fields, technical asset
 * columns, and persistence columns are assembled as expected, and that malformed schemas fail
 * visibly. This helps ensure future schema or template refactorings do not break CSV generation or
 * table-column assumptions.</p>
 */
class SubmoduleUtilityTest {

	private final SubmoduleUtility utility = new SubmoduleUtility();

	@Test
	void getCsvHeaderReadsSchemaPropertiesAndAutoPopulatedFields() {
		assertThat(utility.getCSVHeader(submodel())).containsExactly(
				"manufacturerId",
				"partInstanceId",
				"createdBy",
				"staticField");
	}

	@Test
	void getTableColomnHeaderForCsvAddsTechnicalColumns() {
		assertThat(utility.getTableColomnHeaderForCSV(submodel())).contains(
				SubmoduleCommonColumnsConstant.SHELL_ID,
				SubmoduleCommonColumnsConstant.SUBMODULE_ID,
				SubmoduleCommonColumnsConstant.ASSET_ID,
				SubmoduleCommonColumnsConstant.ACCESS_POLICY_ID,
				SubmoduleCommonColumnsConstant.USAGE_POLICY_ID,
				SubmoduleCommonColumnsConstant.CONTRACT_DEFINATION_ID);
	}

	@Test
	void getTableColomnHeaderAddsPersistenceColumns() {
		assertThat(utility.getTableColomnHeader(submodel())).contains(
				SubmoduleCommonColumnsConstant.PROCESS_ID,
				SubmoduleCommonColumnsConstant.DELETED,
				SubmoduleCommonColumnsConstant.UPDATED,
				SubmoduleCommonColumnsConstant.SHELL_ACCESS_RULE_IDS);
	}

	@Test
	void getTableNameReadsSubmodelProperty() throws Exception {
		assertThat(utility.getTableName(submodel())).isEqualTo("serialpart_v_300");
	}

	@Test
	void getCsvHeaderThrowsWhenSchemaItemsAreMissing() {
		Submodel invalidSubmodel = Submodel.builder()
				.schema(JsonParser.parseString("{}").getAsJsonObject())
				.build();

		assertThatThrownBy(() -> utility.getCSVHeader(invalidSubmodel)).isInstanceOf(Exception.class);
	}

	private Submodel submodel() {
		return Submodel.builder()
				.schema(JsonParser.parseString("""
						{
						  "items": {
						    "properties": {
						      "manufacturerId": {"type": "string"},
						      "partInstanceId": {"type": "string"}
						    }
						  },
						  "addOn": {
						    "autoPopulatedfields": [
						      {"key": "${createdBy}"},
						      {"key": "staticField"}
						    ]
						  }
						}
						""").getAsJsonObject())
				.properties(Map.of("tableName", "serialpart_v_300"))
				.build();
	}
}
