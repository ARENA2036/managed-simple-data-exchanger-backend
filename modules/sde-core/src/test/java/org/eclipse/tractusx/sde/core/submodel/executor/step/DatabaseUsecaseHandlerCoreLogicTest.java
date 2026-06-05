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

package org.eclipse.tractusx.sde.core.submodel.executor.step;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.tractusx.sde.common.constants.CommonConstants;
import org.eclipse.tractusx.sde.common.entities.PolicyModel;
import org.eclipse.tractusx.sde.common.model.Submodel;
import org.eclipse.tractusx.sde.common.submodel.executor.SubmoduleMapperUsecaseStep;
import org.eclipse.tractusx.sde.core.processreport.repository.SubmodelCustomHistoryGenerator;
import org.eclipse.tractusx.sde.core.service.SubmodelService;
import org.eclipse.tractusx.sde.core.utils.SubmoduleUtility;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Tests the database executor step that persists and reads dynamic submodel history rows.
 *
 * <p>The handler resolves the active submodel schema into table names and column lists before
 * delegating to the dynamic history generator. These tests verify save, read, delete-marker, and
 * updated-count delegation with local fakes, keeping dynamic SQL generation out of scope while still
 * protecting the handler's orchestration contract.</p>
 */
class DatabaseUsecaseHandlerCoreLogicTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	void runResolvesSchemaColumnsAndPersistsSubmodelData() {
		FakeHistoryGenerator history = new FakeHistoryGenerator();
		DatabaseUsecaseHandler handler = handler(history, new FakeSubmoduleMapperUsecaseStep());
		handler.init(schema());
		ObjectNode row = MAPPER.createObjectNode().put("partInstanceId", "part-1");

		handler.run(1, row, "process-1", new PolicyModel());

		assertThat(history.savedColumns).containsExactly("partInstanceId", "process_id");
		assertThat(history.savedTable).isEqualTo("serial_part");
		assertThat(history.savedProcessId).isEqualTo("process-1");
		assertThat(history.savedPkColumns).containsExactly("partInstanceId");
		assertThat(history.savedJson).isSameAs(row);
	}

	@Test
	void readCreatedTwinsMapsRawRowsThroughResponseHandler() {
		FakeHistoryGenerator history = new FakeHistoryGenerator();
		JsonObject raw = new JsonObject();
		raw.addProperty("partInstanceId", "part-1");
		history.readRows = List.of(raw);
		FakeSubmoduleMapperUsecaseStep mapper = new FakeSubmoduleMapperUsecaseStep();
		DatabaseUsecaseHandler handler = handler(history, mapper);
		handler.init(schema());

		List<JsonObject> result = handler.readCreatedTwins("process-1", CommonConstants.DELETED_Y);

		assertThat(history.readDeletedFlag).isEqualTo(CommonConstants.DELETED_Y);
		assertThat(mapper.lastInput).isSameAs(raw);
		assertThat(result).hasSize(1);
		assertThat(result.get(0).get("formatted").getAsBoolean()).isTrue();
	}

	@Test
	void saveSubmoduleWithDeletedMarksRowDeletedByIdentifierAndTable() {
		FakeHistoryGenerator history = new FakeHistoryGenerator();
		DatabaseUsecaseHandler handler = handler(history, new FakeSubmoduleMapperUsecaseStep());
		handler.init(schema());
		JsonObject row = new JsonObject();
		row.addProperty("partInstanceId", "part-1");

		handler.saveSubmoduleWithDeleted(1, row, "delete-process", "reference-process");

		assertThat(history.deletedUuid).isEqualTo("part-1");
		assertThat(history.deletedTable).isEqualTo("serial_part");
		assertThat(history.deletedIdentifier).isEqualTo("partInstanceId");
	}

	@Test
	void getUpdatedDataDelegatesToHistoryGeneratorWithUpdatedFlag() {
		FakeHistoryGenerator history = new FakeHistoryGenerator();
		history.updatedCount = 3;
		DatabaseUsecaseHandler handler = handler(history, new FakeSubmoduleMapperUsecaseStep());
		handler.init(schema());

		assertThat(handler.getUpdatedData("process-1")).isEqualTo(3);
		assertThat(history.updatedTable).isEqualTo("serial_part");
		assertThat(history.updatedFlag).isEqualTo(CommonConstants.UPDATED_Y);
		assertThat(history.updatedProcessId).isEqualTo("process-1");
	}

	private static DatabaseUsecaseHandler handler(FakeHistoryGenerator history, FakeSubmoduleMapperUsecaseStep mapper) {
		return new DatabaseUsecaseHandler(history, new FakeSubmodelService(), new FakeSubmoduleUtility(), mapper);
	}

	private static JsonObject schema() {
		return JsonParser.parseString("""
				{
				  "id": "serial-part",
				  "items": {
				    "properties": {},
				    "required": [],
				    "dependentRequired": {}
				  },
				  "addOn": {
				    "identifier": "${partInstanceId}",
				    "databaseIdentifierSpecs": ["${partInstanceId}"],
				    "lookupShellSpecificAssetIdsSpecs": {},
				    "shortIdSpecs": [],
				    "responseTemplate": {}
				  }
				}
				""").getAsJsonObject();
	}

	private static Submodel submodel() {
		return Submodel.builder().id("serial-part").schema(schema()).build();
	}

	private static final class FakeHistoryGenerator extends SubmodelCustomHistoryGenerator {
		private List<String> savedColumns;
		private String savedTable;
		private String savedProcessId;
		private JsonNode savedJson;
		private List<String> savedPkColumns;
		private List<JsonObject> readRows = List.of();
		private String readDeletedFlag;
		private String deletedUuid;
		private String deletedTable;
		private String deletedIdentifier;
		private int updatedCount;
		private String updatedTable;
		private String updatedFlag;
		private String updatedProcessId;

		private FakeHistoryGenerator() {
			super(null);
		}

		@Override
		public int saveSubmodelData(List<String> colNames, String tableEntityName, String processId,
				JsonNode submodelData, List<String> pkColomn) {
			savedColumns = colNames;
			savedTable = tableEntityName;
			savedProcessId = processId;
			savedJson = submodelData;
			savedPkColumns = pkColomn;
			return 1;
		}

		@Override
		public List<JsonObject> findAllSubmoduleAsJsonList(List<String> colNames, String tableEntityName,
				String processId, String fetchNotDeletedRecord) {
			readDeletedFlag = fetchNotDeletedRecord;
			return readRows;
		}

		@Override
		public int saveAspectWithDeleted(String uuid, String tableEntityName, String pkColomn) {
			deletedUuid = uuid;
			deletedTable = tableEntityName;
			deletedIdentifier = pkColomn;
			return 1;
		}

		@Override
		public int countUpdatedRecordCount(String tableEntityName, String updated, String processId) {
			updatedTable = tableEntityName;
			updatedFlag = updated;
			updatedProcessId = processId;
			return updatedCount;
		}
	}

	private static final class FakeSubmodelService extends SubmodelService {
		private FakeSubmodelService() {
			super(null, null, null);
		}

		@Override
		public Submodel findSubmodelByNameAsSubmdelObject(String submodelName) {
			return submodel();
		}
	}

	private static final class FakeSubmoduleUtility extends SubmoduleUtility {
		@Override
		public List<String> getTableColomnHeader(Submodel schemaObj) {
			return List.of("partInstanceId", "process_id");
		}

		@Override
		public String getTableName(Submodel submodel) {
			return "serial_part";
		}
	}

	private static final class FakeSubmoduleMapperUsecaseStep implements SubmoduleMapperUsecaseStep {
		private JsonObject lastInput;

		@Override public void init(JsonObject submodelSchema) { }

		@Override
		public JsonObject mapJsonbjectToFormatedResponse(JsonObject jsonObject) {
			lastInput = jsonObject;
			JsonObject formatted = new JsonObject();
			formatted.addProperty("formatted", true);
			return formatted;
		}
	}
}
