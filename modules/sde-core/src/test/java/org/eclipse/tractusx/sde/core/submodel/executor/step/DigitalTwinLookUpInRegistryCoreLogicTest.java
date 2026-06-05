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

import java.util.List;
import java.util.Map;

import org.eclipse.tractusx.sde.common.exception.CsvHandlerDigitalTwinUseCaseException;
import org.eclipse.tractusx.sde.digitaltwins.entities.request.ShellLookupRequest;
import org.eclipse.tractusx.sde.digitaltwins.entities.response.ShellDescriptorResponse;
import org.eclipse.tractusx.sde.digitaltwins.facilitator.DigitalTwinsFacilitator;
import org.eclipse.tractusx.sde.digitaltwins.facilitator.DigitalTwinsUtility;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Tests local digital-twin lookup behavior used for relation submodel resolution.
 *
 * <p>The lookup helper can search local and remote registries. These tests focus on the local branch,
 * which is deterministic with small fakes: exactly one shell returns its global asset id, while empty
 * or ambiguous lookup results fail with explicit DTR use-case exceptions.</p>
 */
class DigitalTwinLookUpInRegistryCoreLogicTest {

	@Test
	void lookupTwinInLocalRegistryReturnsGlobalAssetIdForSingleShell() {
		FakeDigitalTwinsFacilitator facilitator = new FakeDigitalTwinsFacilitator(List.of("shell-1"));
		ShellDescriptorResponse descriptor = new ShellDescriptorResponse();
		descriptor.setGlobalAssetId("global-asset-id");
		facilitator.descriptors = List.of(descriptor);
		DigitalTwinLookUpInRegistry lookup = new DigitalTwinLookUpInRegistry(new FakeDigitalTwinsUtility(), null, null,
				facilitator);

		String result = lookup.lookupTwinInLocalRrgistry(1, Map.of("manufacturerPartId", "part-1"),
				new ObjectMapper().createObjectNode(), new com.google.gson.JsonObject());

		assertThat(result).isEqualTo("global-asset-id");
	}

	@Test
	void lookupTwinInLocalRegistryThrowsWhenNoShellExists() {
		DigitalTwinLookUpInRegistry lookup = new DigitalTwinLookUpInRegistry(new FakeDigitalTwinsUtility(), null, null,
				new FakeDigitalTwinsFacilitator(List.of()));

		assertThatThrownBy(() -> lookup.lookupTwinInLocalRrgistry(1, Map.of("manufacturerPartId", "part-1"),
				new ObjectMapper().createObjectNode(), new com.google.gson.JsonObject()))
				.isInstanceOf(CsvHandlerDigitalTwinUseCaseException.class)
				.hasMessageContaining("No relational aspect found");
	}

	@Test
	void lookupTwinInLocalRegistryThrowsWhenMultipleShellsExist() {
		DigitalTwinLookUpInRegistry lookup = new DigitalTwinLookUpInRegistry(new FakeDigitalTwinsUtility(), null, null,
				new FakeDigitalTwinsFacilitator(List.of("shell-1", "shell-2")));

		assertThatThrownBy(() -> lookup.lookupTwinInLocalRrgistry(1, Map.of("manufacturerPartId", "part-1"),
				new ObjectMapper().createObjectNode(), new com.google.gson.JsonObject()))
				.isInstanceOf(CsvHandlerDigitalTwinUseCaseException.class)
				.hasMessageContaining("Multiple ids found");
	}

	private static final class FakeDigitalTwinsFacilitator extends DigitalTwinsFacilitator {
		private final List<String> shellIds;
		private List<ShellDescriptorResponse> descriptors = List.of();

		private FakeDigitalTwinsFacilitator(List<String> shellIds) {
			super(null, null, null);
			this.shellIds = shellIds;
		}

		@Override
		public List<String> shellLookup(ShellLookupRequest request) {
			return shellIds;
		}

		@Override
		public List<ShellDescriptorResponse> getShellDescriptorsWithSubmodelDetails(List<String> shellIds) {
			return descriptors;
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
