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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.eclipse.tractusx.sde.common.configuration.properties.PCFAssetStaticPropertyHolder;
import org.eclipse.tractusx.sde.common.configuration.properties.SDEConfigurationProperties;
import org.eclipse.tractusx.sde.common.constants.SubmoduleCommonColumnsConstant;
import org.eclipse.tractusx.sde.common.exception.CsvHandlerDigitalTwinUseCaseException;
import org.eclipse.tractusx.sde.digitaltwins.entities.request.ShellLookupRequest;
import org.eclipse.tractusx.sde.digitaltwins.entities.response.ShellDescriptorResponse;
import org.eclipse.tractusx.sde.digitaltwins.facilitator.DigitalTwinsFacilitator;
import org.eclipse.tractusx.sde.digitaltwins.facilitator.DigitalTwinsUtility;
import org.junit.jupiter.api.Test;

import com.google.gson.JsonObject;

/**
 * Tests focused helper behavior in the digital-twin executor step.
 *
 * <p>The full run method coordinates many external registry and access-rule operations. These tests
 * protect the stable public seams that are cheap to test locally: shell lookup result handling,
 * deletion delegation, and identifier validation. They make important DTR edge cases visible without
 * starting a Spring context or contacting a registry.</p>
 */
class DigitalTwinUseCaseHandlerCoreLogicTest {

	@Test
	void checkShellAndGetIdIfExistReturnsSingleShellId() throws Exception {
		FakeDigitalTwinsFacilitator facilitator = new FakeDigitalTwinsFacilitator();
		facilitator.shellLookupResult = List.of("shell-1");
		DigitalTwinUseCaseHandler handler = handler(facilitator);

		String shellId = handler.checkShellAndGetIdIfExist(new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode(),
				Map.of("manufacturerPartId", "part-1"));

		assertThat(shellId).isEqualTo("shell-1");
	}

	@Test
	void checkShellAndGetIdIfExistThrowsWhenRegistryReturnsMultipleShellIds() {
		FakeDigitalTwinsFacilitator facilitator = new FakeDigitalTwinsFacilitator();
		facilitator.shellLookupResult = List.of("shell-1", "shell-2");
		DigitalTwinUseCaseHandler handler = handler(facilitator);

		assertThatThrownBy(() -> handler.checkShellAndGetIdIfExist(
				new com.fasterxml.jackson.databind.ObjectMapper().createObjectNode(), Map.of("manufacturerPartId", "part-1")))
				.isInstanceOf(CsvHandlerDigitalTwinUseCaseException.class)
				.hasMessageContaining("Multiple ids found");
	}

	@Test
	void deleteDelegatesShellAndSubmodelIdsToDigitalTwinFacilitator() {
		FakeDigitalTwinsFacilitator facilitator = new FakeDigitalTwinsFacilitator();
		DigitalTwinUseCaseHandler handler = handler(facilitator);
		JsonObject row = new JsonObject();
		row.addProperty(SubmoduleCommonColumnsConstant.SHELL_ID, "shell-1");
		row.addProperty(SubmoduleCommonColumnsConstant.SUBMODULE_ID, "submodel-1");

		handler.delete(1, row, "delete-process", "reference-process");

		assertThat(facilitator.calls).containsExactly("deleteSubmodel:shell-1:submodel-1");
	}

	@Test
	void isValidDocumentsAcceptedAndRejectedIdentifierPatterns() {
		assertThat(DigitalTwinUseCaseHandler.isValid("Asset_1")).isTrue();
		assertThat(DigitalTwinUseCaseHandler.isValid("Asset-1")).isTrue();
		assertThat(DigitalTwinUseCaseHandler.isValid("1Asset")).isFalse();
		assertThat(DigitalTwinUseCaseHandler.isValid("Asset!")).isFalse();
		assertThat(DigitalTwinUseCaseHandler.isValid(null)).isFalse();
	}

	private static DigitalTwinUseCaseHandler handler(FakeDigitalTwinsFacilitator facilitator) {
		SDEConfigurationProperties properties = new SDEConfigurationProperties();
		properties.setManufacturerId("BPNL00000003CML1");
		return new DigitalTwinUseCaseHandler(facilitator, new FakeDigitalTwinsUtility(), properties,
				null, null, new PCFAssetStaticPropertyHolder());
	}

	private static final class FakeDigitalTwinsFacilitator extends DigitalTwinsFacilitator {
		private List<String> shellLookupResult = List.of();
		private final List<String> calls = new ArrayList<>();

		private FakeDigitalTwinsFacilitator() {
			super(null, null, null);
		}

		@Override
		public List<String> shellLookup(ShellLookupRequest request) {
			return shellLookupResult;
		}

		@Override
		public void deleteSubmodelfromShellById(String shellId, String subModelId) {
			calls.add("deleteSubmodel:" + shellId + ":" + subModelId);
		}

		@Override
		public List<ShellDescriptorResponse> getShellDescriptorsWithSubmodelDetails(List<String> shellIds) {
			return List.of();
		}
	}

	private static final class FakeDigitalTwinsUtility extends DigitalTwinsUtility {
		@Override
		public ShellLookupRequest getShellLookupRequest(Map<String, String> specificAssetIds) {
			ShellLookupRequest request = new ShellLookupRequest();
			specificAssetIds.forEach(request::addLocalIdentifier);
			return request;
		}
	}
}
