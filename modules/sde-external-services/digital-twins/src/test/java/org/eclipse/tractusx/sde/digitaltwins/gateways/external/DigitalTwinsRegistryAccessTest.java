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

package org.eclipse.tractusx.sde.digitaltwins.gateways.external;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.net.URI;

import org.eclipse.tractusx.sde.common.utils.TokenUtility;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class DigitalTwinsRegistryAccessTest {

	@Test
	void registryUriComesFromConfiguredHostname() {
		DigitalTwinsRegistryAccess access = access(mock(TokenUtility.class), "");

		assertThat(access.registryUri()).isEqualTo(URI.create("http://dtr.example/semantics/registry"));
	}

	@Test
	void noAuthorizationHeaderWhenTokenUrlIsEmpty() {
		TokenUtility tokenUtility = mock(TokenUtility.class);

		assertThat(access(tokenUtility, "").authHeader()).isEmpty();
		verifyNoInteractions(tokenUtility);
	}

	@Test
	void bearerHeaderWhenTokenUrlIsConfigured() {
		TokenUtility tokenUtility = mock(TokenUtility.class);
		when(tokenUtility.getToken(any(URI.class), anyString(), anyString(), anyString())).thenReturn("abc");

		assertThat(access(tokenUtility, "http://idp.example/token").authHeader())
				.containsEntry("Authorization", "Bearer abc");
	}

	private static DigitalTwinsRegistryAccess access(TokenUtility tokenUtility, String tokenUrl) {
		DigitalTwinsRegistryAccess access = new DigitalTwinsRegistryAccess(tokenUtility);
		ReflectionTestUtils.setField(access, "registryHostname", "http://dtr.example/semantics/registry");
		ReflectionTestUtils.setField(access, "tokenUri", URI.create(tokenUrl));
		ReflectionTestUtils.setField(access, "grantType", "client_credentials");
		ReflectionTestUtils.setField(access, "clientId", "client");
		ReflectionTestUtils.setField(access, "clientSecret", "secret");
		return access;
	}
}
