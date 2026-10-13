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

import org.eclipse.tractusx.sde.common.dataspace.DataspaceProfileProperties;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

class BPNDiscoveryUseCaseHandlerDataspaceProfileTest {

	@Test
	void constructXSkipsBpnDiscoveryAndReturnsRowUnchanged() {
		BpnDiscoveryProxyService proxy = mock(BpnDiscoveryProxyService.class);
		BPNDiscoveryUseCaseHandler handler = new BPNDiscoveryUseCaseHandler(proxy,
				new DataspaceProfileProperties("constructx"));
		ObjectNode row = new ObjectMapper().createObjectNode().put("manufacturer_part_id", "37754B7-76");

		assertThat(handler.run(1, row, "process-1", null)).isSameAs(row);
		verifyNoInteractions(proxy);
	}
}
