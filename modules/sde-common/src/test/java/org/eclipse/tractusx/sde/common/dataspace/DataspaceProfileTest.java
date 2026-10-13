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
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.eclipse.tractusx.sde.common.dataspace.DataspaceProfile.ParticipantIdType;
import org.junit.jupiter.api.Test;

class DataspaceProfileTest {

	@Test
	void defaultsToTractusXWhenNotConfigured() {
		assertThat(new DataspaceProfileProperties("").profile()).isEqualTo(DataspaceProfile.TRACTUSX);
		assertThat(DataspaceProfile.fromValue(null)).isEqualTo(DataspaceProfile.TRACTUSX);
	}

	@Test
	void acceptsCommonSpellings() {
		assertThat(DataspaceProfile.fromValue("constructx")).isEqualTo(DataspaceProfile.CONSTRUCTX);
		assertThat(DataspaceProfile.fromValue("Construct-X")).isEqualTo(DataspaceProfile.CONSTRUCTX);
		assertThat(DataspaceProfile.fromValue(" tractus_x ")).isEqualTo(DataspaceProfile.TRACTUSX);
	}

	@Test
	void participantIdTypeFollowsProfile() {
		assertThat(DataspaceProfile.TRACTUSX.participantIdType()).isEqualTo(ParticipantIdType.BPN);
		assertThat(DataspaceProfile.CONSTRUCTX.participantIdType()).isEqualTo(ParticipantIdType.DID);
		assertThat(new DataspaceProfileProperties("constructx").isConstructX()).isTrue();
	}

	@Test
	void unknownProfileFailsFast() {
		assertThatThrownBy(() -> new DataspaceProfileProperties("gaia-x"))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("gaia-x");
	}
}
