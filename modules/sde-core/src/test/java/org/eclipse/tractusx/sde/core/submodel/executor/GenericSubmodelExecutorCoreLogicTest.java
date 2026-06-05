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

package org.eclipse.tractusx.sde.core.submodel.executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.tractusx.sde.common.constants.CommonConstants;
import org.eclipse.tractusx.sde.common.entities.PolicyModel;
import org.eclipse.tractusx.sde.common.entities.csv.RowData;
import org.eclipse.tractusx.sde.common.exception.NoDataFoundException;
import org.eclipse.tractusx.sde.common.model.Submodel;
import org.eclipse.tractusx.sde.common.submodel.executor.BPNDiscoveryUsecaseStep;
import org.eclipse.tractusx.sde.common.submodel.executor.DatabaseUsecaseStep;
import org.eclipse.tractusx.sde.common.submodel.executor.DigitalTwinUsecaseStep;
import org.eclipse.tractusx.sde.common.submodel.executor.EDCUsecaseStep;
import org.eclipse.tractusx.sde.common.submodel.executor.SubmoduleMapperUsecaseStep;
import org.eclipse.tractusx.sde.common.submodel.executor.create.steps.impl.CsvParse;
import org.eclipse.tractusx.sde.common.submodel.executor.create.steps.impl.GenerateUrnUUID;
import org.eclipse.tractusx.sde.common.submodel.executor.create.steps.impl.JsonRecordFormating;
import org.eclipse.tractusx.sde.common.submodel.executor.create.steps.impl.JsonRecordValidate;
import org.eclipse.tractusx.sde.submodelserver.handler.SubmodelServerHandler;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Verifies the central provider-side executor pipeline without external systems.
 *
 * <p>The generic executor is responsible for calling the CSV/JSON preparation steps and then the
 * digital twin, EDC, BPN discovery, database, and submodel server steps in the correct order. These
 * tests use local fakes to protect that orchestration contract, including delete and read paths,
 * because a refactoring error here would affect every submodel ingestion workflow even when
 * individual step implementations still compile.</p>
 */
class GenericSubmodelExecutorCoreLogicTest {

	private static final ObjectMapper MAPPER = new ObjectMapper();

	@Test
	void executeCsvRecordRunsPreparationAndWorkflowStepsInOrder() {
		List<String> calls = new ArrayList<>();
		FakeDatabaseUsecaseStep database = new FakeDatabaseUsecaseStep(calls);
		GenericSubmodelExecutor executor = executor(calls, database, new FakeSubmoduleMapperUsecaseStep(calls));
		executor.init(submodel());

		executor.executeCsvRecord(new RowData(7, "value"), MAPPER.createObjectNode(), "process-1", new PolicyModel(),
				MAPPER.createObjectNode());

		assertThat(calls).containsExactly(
				"csv.init",
				"csv.run:7",
				"uuid.init",
				"uuid.run",
				"validate.init",
				"validate.run:7",
				"dt.init",
				"dt.run:7",
				"edc.init",
				"edc.run:7",
				"bpn.init",
				"bpn.run:7",
				"db.init",
				"db.run:7",
				"server.init",
				"server.run:7");
	}

	@Test
	void executeJsonRecordRunsFormatterBeforeSharedWorkflowSteps() {
		List<String> calls = new ArrayList<>();
		FakeDatabaseUsecaseStep database = new FakeDatabaseUsecaseStep(calls);
		GenericSubmodelExecutor executor = executor(calls, database, new FakeSubmoduleMapperUsecaseStep(calls));
		executor.init(submodel());

		executor.executeJsonRecord(3, MAPPER.createObjectNode(), "process-1", new PolicyModel(), MAPPER.createObjectNode());

		assertThat(calls).containsExactly(
				"json.init",
				"json.run:3",
				"uuid.init",
				"uuid.run",
				"validate.init",
				"validate.run:3",
				"dt.init",
				"dt.run:3",
				"edc.init",
				"edc.run:3",
				"bpn.init",
				"bpn.run:3",
				"db.init",
				"db.run:3",
				"server.init",
				"server.run:3");
	}

	@Test
	void executeDeleteRecordDeletesExternalResourcesBeforeMarkingDatabaseRowDeleted() {
		List<String> calls = new ArrayList<>();
		FakeDatabaseUsecaseStep database = new FakeDatabaseUsecaseStep(calls);
		GenericSubmodelExecutor executor = executor(calls, database, new FakeSubmoduleMapperUsecaseStep(calls));
		executor.init(submodel());

		executor.executeDeleteRecord(5, new JsonObject(), "delete-process", "reference-process");

		assertThat(calls).containsExactly(
				"edc.init",
				"edc.delete:5",
				"dt.init",
				"dt.delete:5",
				"db.init",
				"db.saveDeleted:5");
	}

	@Test
	void readCreatedTwinsForDeleteThrowsWhenDatabaseReturnsNoRows() {
		List<String> calls = new ArrayList<>();
		FakeDatabaseUsecaseStep database = new FakeDatabaseUsecaseStep(calls);
		database.readCreatedTwins = List.of();
		GenericSubmodelExecutor executor = executor(calls, database, new FakeSubmoduleMapperUsecaseStep(calls));
		executor.init(submodel());

		assertThatThrownBy(() -> executor.readCreatedTwinsforDelete("reference-process"))
				.isInstanceOf(NoDataFoundException.class)
				.hasMessageContaining("reference-process");

		assertThat(database.lastReadCreatedTwinsDeletedFlag).isEqualTo(CommonConstants.DELETED_Y);
	}

	@Test
	void readCreatedTwinsDetailsFormatsDatabaseResultThroughMapperStep() {
		List<String> calls = new ArrayList<>();
		FakeDatabaseUsecaseStep database = new FakeDatabaseUsecaseStep(calls);
		JsonObject raw = new JsonObject();
		raw.addProperty("raw", "value");
		database.readCreatedTwinsDetails = raw;
		FakeSubmoduleMapperUsecaseStep mapper = new FakeSubmoduleMapperUsecaseStep(calls);
		GenericSubmodelExecutor executor = executor(calls, database, mapper);
		executor.init(submodel());

		JsonObject result = executor.readCreatedTwinsDetails("urn:uuid:1");

		assertThat(result.get("formatted").getAsBoolean()).isTrue();
		assertThat(mapper.lastInput).isSameAs(raw);
		assertThat(calls).containsExactly("db.init", "mapper.init", "db.details:urn:uuid:1", "mapper.map");
	}

	private static GenericSubmodelExecutor executor(List<String> calls, FakeDatabaseUsecaseStep database,
			FakeSubmoduleMapperUsecaseStep mapper) {
		return new GenericSubmodelExecutor(
				new FakeCsvParse(calls),
				new FakeJsonRecordFormating(calls),
				new FakeGenerateUrnUUID(calls),
				new FakeJsonRecordValidate(calls),
				new FakeSubmodelServerHandler(calls),
				new FakeDigitalTwinUsecaseStep(calls),
				new FakeEDCUsecaseStep(calls),
				new FakeBPNDiscoveryUsecaseStep(calls),
				database,
				mapper);
	}

	private static Submodel submodel() {
		return Submodel.builder()
				.schema(JsonParser.parseString("""
						{
						  "id": "serial-part",
						  "items": {
						    "properties": {},
						    "required": [],
						    "dependentRequired": {}
						  },
						  "addOn": {
						    "identifier": "${partInstanceId}",
						    "lookupShellSpecificAssetIdsSpecs": {},
						    "shortIdSpecs": [],
						    "responseTemplate": {}
						  }
						}
						""").getAsJsonObject())
				.build();
	}

	private static final class FakeCsvParse extends CsvParse {
		private final List<String> calls;

		private FakeCsvParse(List<String> calls) {
			super(null);
			this.calls = calls;
		}

		@Override
		public void init(JsonObject submodelSchema) {
			calls.add("csv.init");
		}

		@Override
		public ObjectNode run(RowData rowData, ObjectNode rowjObject, String processId) {
			calls.add("csv.run:" + rowData.position());
			return rowjObject;
		}
	}

	private static final class FakeJsonRecordFormating extends JsonRecordFormating {
		private final List<String> calls;

		private FakeJsonRecordFormating(List<String> calls) {
			super(null);
			this.calls = calls;
		}

		@Override
		public void init(JsonObject submodelSchema) {
			calls.add("json.init");
		}

		@Override
		public ObjectNode run(Integer rowIndex, ObjectNode rowjObject, String processId) {
			calls.add("json.run:" + rowIndex);
			return rowjObject;
		}
	}

	private static final class FakeGenerateUrnUUID extends GenerateUrnUUID {
		private final List<String> calls;

		private FakeGenerateUrnUUID(List<String> calls) {
			this.calls = calls;
		}

		@Override
		public void init(JsonObject submodelSchema) {
			calls.add("uuid.init");
		}

		@Override
		public ObjectNode run(ObjectNode jsonObject, String processId) {
			calls.add("uuid.run");
			return jsonObject;
		}
	}

	private static final class FakeJsonRecordValidate extends JsonRecordValidate {
		private final List<String> calls;

		private FakeJsonRecordValidate(List<String> calls) {
			this.calls = calls;
		}

		@Override
		public void init(JsonObject submodelSchema) {
			calls.add("validate.init");
		}

		@Override
		public boolean run(Integer rowIndex, JsonNode inputJsonObject) {
			calls.add("validate.run:" + rowIndex);
			return true;
		}
	}

	private static final class FakeSubmodelServerHandler extends SubmodelServerHandler {
		private final List<String> calls;

		private FakeSubmodelServerHandler(List<String> calls) {
			super(null, null);
			this.calls = calls;
		}

		@Override
		public void init(JsonObject submodelSchema) {
			calls.add("server.init");
		}

		@Override
		public JsonNode run(Integer rowIndex, ObjectNode assetInfo, ObjectNode submodelData, PolicyModel policy,
				String processId) {
			calls.add("server.run:" + rowIndex);
			return assetInfo;
		}
	}

	private static final class FakeDigitalTwinUsecaseStep implements DigitalTwinUsecaseStep {
		private final List<String> calls;

		private FakeDigitalTwinUsecaseStep(List<String> calls) {
			this.calls = calls;
		}

		@Override
		public void init(JsonObject submodelSchema) {
			calls.add("dt.init");
		}

		@Override
		public JsonNode run(Integer rowIndex, ObjectNode jsonObject, String processId, PolicyModel policy) {
			calls.add("dt.run:" + rowIndex);
			return jsonObject;
		}

		@Override
		public void delete(Integer rowIndex, JsonObject jsonObject, String delProcessId, String refProcessId) {
			calls.add("dt.delete:" + rowIndex);
		}
	}

	private static final class FakeEDCUsecaseStep implements EDCUsecaseStep {
		private final List<String> calls;

		private FakeEDCUsecaseStep(List<String> calls) {
			this.calls = calls;
		}

		@Override
		public void init(JsonObject submodelSchema) {
			calls.add("edc.init");
		}

		@Override
		public JsonNode run(Integer rowIndex, ObjectNode jsonObject, String processId, PolicyModel policy) {
			calls.add("edc.run:" + rowIndex);
			return jsonObject;
		}

		@Override
		public void delete(Integer rowIndex, JsonObject jsonObject, String delProcessId, String refProcessId) {
			calls.add("edc.delete:" + rowIndex);
		}
	}

	private static final class FakeBPNDiscoveryUsecaseStep implements BPNDiscoveryUsecaseStep {
		private final List<String> calls;

		private FakeBPNDiscoveryUsecaseStep(List<String> calls) {
			this.calls = calls;
		}

		@Override
		public void init(JsonObject submodelSchema) {
			calls.add("bpn.init");
		}

		@Override
		public JsonNode run(Integer rowIndex, ObjectNode jsonObject, String processId, PolicyModel policy) {
			calls.add("bpn.run:" + rowIndex);
			return jsonObject;
		}

		@Override
		public void delete(Integer rowIndex, JsonObject jsonObject, String delProcessId, String refProcessId) {
			calls.add("bpn.delete:" + rowIndex);
		}
	}

	private static final class FakeDatabaseUsecaseStep implements DatabaseUsecaseStep {
		private final List<String> calls;
		private List<JsonObject> readCreatedTwins = List.of();
		private JsonObject readCreatedTwinsDetails = new JsonObject();
		private String lastReadCreatedTwinsDeletedFlag;

		private FakeDatabaseUsecaseStep(List<String> calls) {
			this.calls = calls;
		}

		@Override
		public void init(JsonObject submodelSchema) {
			calls.add("db.init");
		}

		@Override
		public JsonNode run(Integer rowIndex, ObjectNode jsonObject, String processId, PolicyModel policy) {
			calls.add("db.run:" + rowIndex);
			return jsonObject;
		}

		@Override
		public void saveSubmoduleWithDeleted(Integer rowIndex, JsonObject jsonObject, String delProcessId,
				String refProcessId) {
			calls.add("db.saveDeleted:" + rowIndex);
		}

		@Override
		public List<JsonObject> readCreatedTwins(String processId, String isDeleted) {
			lastReadCreatedTwinsDeletedFlag = isDeleted;
			return readCreatedTwins;
		}

		@Override
		public JsonObject readCreatedTwinsBySpecifyColomn(String sematicId, String value) {
			return null;
		}

		@Override
		public JsonObject readCreatedTwinsDetails(String uuid) {
			calls.add("db.details:" + uuid);
			return readCreatedTwinsDetails;
		}

		@Override
		public int getUpdatedData(String processId) {
			return 0;
		}
	}

	private static final class FakeSubmoduleMapperUsecaseStep implements SubmoduleMapperUsecaseStep {
		private final List<String> calls;
		private JsonObject lastInput;

		private FakeSubmoduleMapperUsecaseStep(List<String> calls) {
			this.calls = calls;
		}

		@Override
		public void init(JsonObject submodelSchema) {
			calls.add("mapper.init");
		}

		@Override
		public JsonObject mapJsonbjectToFormatedResponse(JsonObject jsonObject) {
			calls.add("mapper.map");
			lastInput = jsonObject;
			JsonObject formatted = new JsonObject();
			formatted.addProperty("formatted", true);
			return formatted;
		}
	}
}
