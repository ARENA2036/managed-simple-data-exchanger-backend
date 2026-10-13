/********************************************************************************
 * Copyright (c) 2026 ARENA2036 e.V.
 * Copyright (c) 2023, 2024 Contributors to the Eclipse Foundation
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

import java.net.URI;
import java.util.Map;

import org.eclipse.tractusx.sde.common.utils.TokenUtility;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

/**
 * Supplies the Digital Twin Registry base URI and bearer header for each call.
 *
 * <p>Replaces the former Feign request interceptor, so callers pass the target
 * registry explicitly and it can later be resolved per tenant.</p>
 */
@Component
@RequiredArgsConstructor
public class DigitalTwinsRegistryAccess {

	private final TokenUtility tokenUtility;

	@Value(value = "${digital-twins.hostname:default}")
	private String registryHostname;

	@Value(value = "${digital-twins.authentication.clientSecret}")
	private String clientSecret;

	@Value(value = "${digital-twins.authentication.clientId}")
	private String clientId;

	@Value(value = "${digital-twins.authentication.grantType}")
	private String grantType;

	@Value(value = "${digital-twins.authentication.url:default}")
	private URI tokenUri;

	@Value(value = "${digital-twins.authentication.scope:}")
	private String scope;

	@Value(value = "${digital-twins.managed.thirdparty:false}")
	private boolean managedThirdParty;

	private String accessToken;

	public URI registryUri() {
		return URI.create(registryHostname);
	}

	public Map<String, String> authHeader() {
		if (!isTokenUriConfigured()) {
			return Map.of();
		}
		return Map.of("Authorization", getBearerToken());
	}

	private boolean isTokenUriConfigured() {
		return tokenUri != null && !tokenUri.toString().isBlank() && !"default".equals(tokenUri.toString());
	}

	@SneakyThrows
	private synchronized String getBearerToken() {
		if (accessToken == null || !tokenUtility.isTokenValid(accessToken)) {
			accessToken = managedThirdParty
					? tokenUtility.getToken(tokenUri, grantType, clientId, clientSecret, scope)
					: tokenUtility.getToken(tokenUri, grantType, clientId, clientSecret);
		}
		return "Bearer " + accessToken;
	}
}
