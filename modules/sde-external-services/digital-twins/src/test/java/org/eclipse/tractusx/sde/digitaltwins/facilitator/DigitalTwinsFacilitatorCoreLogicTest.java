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

package org.eclipse.tractusx.sde.digitaltwins.facilitator;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;
import java.util.Map;

import org.eclipse.tractusx.sde.digitaltwins.entities.request.CreateSubModelRequest;
import org.eclipse.tractusx.sde.digitaltwins.entities.request.ShellDescriptorRequest;
import org.eclipse.tractusx.sde.digitaltwins.entities.request.ShellLookupRequest;
import org.eclipse.tractusx.sde.digitaltwins.entities.response.ShellDescriptorResponse;
import org.eclipse.tractusx.sde.digitaltwins.entities.response.ShellLookupResponse;
import org.eclipse.tractusx.sde.digitaltwins.entities.response.SubModelResponse;
import org.eclipse.tractusx.sde.digitaltwins.gateways.external.DigitalTwinsFeignClient;
import org.eclipse.tractusx.sde.digitaltwins.gateways.external.DigitalTwinsRegistryAccess;
import org.eclipse.tractusx.sde.digitaltwins.gateways.external.IAccessRuleManagementApi;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;

/**
 * Verifies the Digital Twin Registry facilitator without a live registry.
 *
 * <p>The facilitator wraps the Feign client, encodes shell identifiers, applies the manufacturer
 * header, passes the registry URI and auth header on every call, and merges existing submodel descriptors when shell details are updated. These tests protect
 * the registry-facing contract at the facilitator boundary while keeping all network calls mocked at
 * the interface level.</p>
 */
class DigitalTwinsFacilitatorCoreLogicTest {

	private static final URI DTR = URI.create("http://dtr.example/semantics/registry");
	private static final Map<String, String> AUTH = Map.of("Authorization", "Bearer test-token");

	@Test
	void shellLookupEncodesAssetIdsAndReturnsResultForOkResponse() throws Exception {
		DigitalTwinsFeignClient client = mock(DigitalTwinsFeignClient.class);
		FakeDigitalTwinsUtility utility = new FakeDigitalTwinsUtility();
		DigitalTwinsFacilitator facilitator = facilitator(client, utility);
		ShellLookupResponse response = new ShellLookupResponse();
		response.setResult(List.of("shell-1"));
		ShellLookupRequest request = new ShellLookupRequest();

		when(client.shellLookup(DTR, List.of("encoded-asset-id"), "BPNL00000003CML1", AUTH))
				.thenReturn(ResponseEntity.ok(response));

		List<String> result = facilitator.shellLookup(request);

		assertThat(result).containsExactly("shell-1");
		assertThat(utility.lastShellLookupRequest).isSameAs(request);
	}

	@Test
	void createShellDescriptorReturnsBodyOnlyForCreatedResponse() {
		DigitalTwinsFeignClient client = mock(DigitalTwinsFeignClient.class);
		DigitalTwinsFacilitator facilitator = facilitator(client, new FakeDigitalTwinsUtility());
		ShellDescriptorRequest request = ShellDescriptorRequest.builder().id("shell-1").build();
		ShellDescriptorResponse body = new ShellDescriptorResponse();
		body.setId("shell-1");

		when(client.createShellDescriptor(DTR, request, AUTH)).thenReturn(ResponseEntity.status(HttpStatus.CREATED).body(body));

		assertThat(facilitator.createShellDescriptor(request)).isSameAs(body);
	}

	@Test
	void updateShellDetailsPreservesExistingSubmodelDescriptorsNotReplacedByNewDescriptor() {
		DigitalTwinsFeignClient client = mock(DigitalTwinsFeignClient.class);
		DigitalTwinsFacilitator facilitator = facilitator(client, new FakeDigitalTwinsUtility());
		ShellDescriptorResponse existingShell = new ShellDescriptorResponse();
		existingShell.setIdShort("existingShortId");
		existingShell.setSubmodelDescriptors(List.of(
				SubModelResponse.builder().id("keep-id").idShort("keep").build(),
				SubModelResponse.builder().id("replace-id").idShort("replace").build()));
		CreateSubModelRequest newSubmodel = CreateSubModelRequest.builder()
				.id("new-id")
				.idShort("replace")
				.build();
		ShellDescriptorRequest updateRequest = ShellDescriptorRequest.builder().build();

		when(client.getShellDescriptorByShellId(DTR, "encoded-shell-1", "BPNL00000003CML1", AUTH))
				.thenReturn(ResponseEntity.ok(existingShell));
		when(client.updateShellDescriptorByShellId(eq(DTR), eq("encoded-shell-1"), eq("BPNL00000003CML1"), any(), eq(AUTH)))
				.thenReturn(ResponseEntity.noContent().build());

		facilitator.updateShellDetails("shell-1", updateRequest, newSubmodel);

		ArgumentCaptor<ShellDescriptorRequest> captor = ArgumentCaptor.forClass(ShellDescriptorRequest.class);
		verify(client).updateShellDescriptorByShellId(eq(DTR), eq("encoded-shell-1"), eq("BPNL00000003CML1"), captor.capture(),
				eq(AUTH));
		ShellDescriptorRequest sent = captor.getValue();
		assertThat(sent.getId()).isEqualTo("shell-1");
		assertThat(sent.getIdShort()).isEqualTo("existingShortId");
		assertThat(sent.getSubmodelDescriptors()).extracting(CreateSubModelRequest::getId)
				.containsExactly("new-id", "keep-id");
	}

	@Test
	void accessRuleMethodsDelegateToAccessRuleApiWithManufacturerHeader() {
		IAccessRuleManagementApi accessApi = mock(IAccessRuleManagementApi.class);
		DigitalTwinsFacilitator facilitator = facilitator(mock(DigitalTwinsFeignClient.class),
				new FakeDigitalTwinsUtility(), accessApi);

		facilitator.deleteAccessControlsRule("rule-1", "BPNL00000003CML1");

		verify(accessApi).deleteAccessControlsRule(DTR, "rule-1", "BPNL00000003CML1", AUTH);
	}

	private static DigitalTwinsFacilitator facilitator(DigitalTwinsFeignClient client, DigitalTwinsUtility utility) {
		return facilitator(client, utility, mock(IAccessRuleManagementApi.class));
	}

	private static DigitalTwinsFacilitator facilitator(DigitalTwinsFeignClient client, DigitalTwinsUtility utility,
			IAccessRuleManagementApi accessRuleApi) {
		DigitalTwinsRegistryAccess registryAccess = mock(DigitalTwinsRegistryAccess.class);
		when(registryAccess.registryUri()).thenReturn(DTR);
		when(registryAccess.authHeader()).thenReturn(AUTH);
		DigitalTwinsFacilitator facilitator = new DigitalTwinsFacilitator(client, utility, accessRuleApi, registryAccess);
		ReflectionTestUtils.setField(facilitator, "manufacturerId", "BPNL00000003CML1");
		return facilitator;
	}

	private static final class FakeDigitalTwinsUtility extends DigitalTwinsUtility {
		private ShellLookupRequest lastShellLookupRequest;

		@Override
		public List<String> encodeAssetIdsObject(ShellLookupRequest request) {
			lastShellLookupRequest = request;
			return List.of("encoded-asset-id");
		}

		@Override
		public String encodeValueAsBase64Utf8(String value) {
			return "encoded-" + value;
		}
	}
}
