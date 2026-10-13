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

package org.eclipse.tractusx.sde.bpndiscovery.handler;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

import java.util.List;

import org.eclipse.tractusx.sde.bpndiscovery.api.IBpndiscoveryExternalServiceApi;
import org.eclipse.tractusx.sde.bpndiscovery.model.request.BpnDiscoverySearchRequest;
import org.eclipse.tractusx.sde.bpndiscovery.model.response.BpnDiscoveryResponse;
import org.eclipse.tractusx.sde.common.dataspace.ConstructXPartnerDirectory;
import org.eclipse.tractusx.sde.common.dataspace.ConstructXPartnerDirectory.Partner;
import org.eclipse.tractusx.sde.common.dataspace.DataspaceProfileProperties;
import org.junit.jupiter.api.Test;

class BpnDiscoveryProxyServiceDataspaceProfileTest {

	@Test
	void constructXSearchReturnsAllConfiguredPartners() {
		IBpndiscoveryExternalServiceApi api = mock(IBpndiscoveryExternalServiceApi.class);
		ConstructXPartnerDirectory directory = new ConstructXPartnerDirectory();
		directory.setPartners(List.of(new Partner("did:web:a", "A", "http://a/dsp"),
				new Partner("did:web:b", "B", "http://b/dsp")));
		BpnDiscoveryProxyService service = new BpnDiscoveryProxyService(api,
				new DataspaceProfileProperties("constructx"), directory);

		assertThat(service.bpnDiscoverySearchData(new BpnDiscoverySearchRequest()).getBpns())
				.extracting(BpnDiscoveryResponse::getValue).containsExactly("did:web:a", "did:web:b");
		verifyNoInteractions(api);
	}
}
