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

package org.eclipse.tractusx.sde.core.submodel.executor.step;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

/**
 * Covers the response-cleanup logic used before persisted submodel data is returned to clients.
 *
 * <p>The handler removes null, blank, empty object, and empty array values from generated response
 * JSON. These tests lock down that behavior for nested structures and invalid input, because response
 * templates are dynamic and a small cleanup regression would change public payload shapes across many
 * submodels.</p>
 */
class SubmoduleResponseHandlerCoreLogicTest {

	@Test
	void removeNullAndEmptyElementsFromJsonRemovesNestedEmptyValuesAndKeepsScalars() {
		String cleaned = SubmoduleResponseHandler.removeNullAndEmptyElementsFromJson("""
				{
				  "keep": "value",
				  "blank": " ",
				  "nullValue": null,
				  "emptyObject": {},
				  "emptyArray": [],
				  "nested": {
				    "nestedKeep": "42",
				    "nestedBlank": "",
				    "nestedEmptyObject": {},
				    "nestedArray": [
				      {"childKeep": "x", "childBlank": ""},
				      {},
				      "",
				      "keptValue"
				    ]
				  }
				}
				""");

		JsonObject json = JsonParser.parseString(cleaned).getAsJsonObject();

		assertThat(json.has("keep")).isTrue();
		assertThat(json.has("blank")).isFalse();
		assertThat(json.has("nullValue")).isFalse();
		assertThat(json.has("emptyObject")).isFalse();
		assertThat(json.has("emptyArray")).isFalse();
		assertThat(json.getAsJsonObject("nested").has("nestedKeep")).isTrue();
		assertThat(json.getAsJsonObject("nested").has("nestedBlank")).isFalse();
		assertThat(json.getAsJsonObject("nested").getAsJsonArray("nestedArray")).hasSize(2);
	}

	@Test
	void removeNullAndEmptyElementsFromJsonReturnsNullInputUnchanged() {
		assertThat(SubmoduleResponseHandler.removeNullAndEmptyElementsFromJson(null)).isNull();
	}

	@Test
	void removeNullAndEmptyElementsFromJsonReturnsInvalidJsonUnchanged() {
		String invalidJson = "{not-json";

		assertThat(SubmoduleResponseHandler.removeNullAndEmptyElementsFromJson(invalidJson)).isEqualTo(invalidJson);
	}
}
