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

package org.eclipse.tractusx.sde.common.dataspace;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.tractusx.sde.common.dataspace.ConstructXPartnerDirectory.Partner;
import org.junit.jupiter.api.Test;

class ConstructXPartnerDirectoryTest {

	@Test
	void resolvesDspUrlsByPartnerDid() {
		ConstructXPartnerDirectory directory = new ConstructXPartnerDirectory();
		directory.setPartners(List.of(
				new Partner("did:web:a", "A", "http://a:9020/dsp"),
				new Partner("did:web:a", "A", "http://a2:9020/dsp"),
				new Partner("did:web:b", "B", "http://b:9020/dsp")));

		assertThat(directory.partnerIds()).containsExactly("did:web:a", "did:web:b");
		assertThat(directory.dspUrlsOf("did:web:a")).containsExactly("http://a:9020/dsp", "http://a2:9020/dsp");
		assertThat(directory.dspUrlsOf("did:web:unknown")).isEmpty();
	}
}
