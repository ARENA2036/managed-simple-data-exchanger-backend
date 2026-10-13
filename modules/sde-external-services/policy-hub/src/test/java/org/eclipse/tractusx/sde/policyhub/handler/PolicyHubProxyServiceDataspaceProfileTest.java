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

package org.eclipse.tractusx.sde.policyhub.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import org.eclipse.tractusx.sde.common.dataspace.DataspaceProfileProperties;
import org.eclipse.tractusx.sde.policyhub.api.IPolicyHubExternalServiceApi;
import org.junit.jupiter.api.Test;

class PolicyHubProxyServiceDataspaceProfileTest {

	@Test
	void constructXGetsNoPolicyHubTemplates() {
		IPolicyHubExternalServiceApi api = mock(IPolicyHubExternalServiceApi.class);
		PolicyHubProxyService service = new PolicyHubProxyService(api, new DataspaceProfileProperties("constructx"));

		assertThat(service.getPolicyAttributes()).isEmpty();
		assertThat(service.getPolicyTypes("Access", "Traceability")).isEmpty();
		assertThatThrownBy(() -> service.getPolicyContent("Traceability", "Access", "Membership", "Equals", "active"))
				.isInstanceOf(UnsupportedOperationException.class);
		verifyNoInteractions(api);
	}
}
