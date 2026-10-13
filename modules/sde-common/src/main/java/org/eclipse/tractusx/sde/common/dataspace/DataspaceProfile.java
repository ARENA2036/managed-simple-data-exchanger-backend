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

import java.util.Arrays;
import java.util.Locale;

public enum DataspaceProfile {

	TRACTUSX(ParticipantIdType.BPN),
	CONSTRUCTX(ParticipantIdType.DID);

	private final ParticipantIdType participantIdType;

	DataspaceProfile(ParticipantIdType participantIdType) {
		this.participantIdType = participantIdType;
	}

	public ParticipantIdType participantIdType() {
		return participantIdType;
	}

	public static DataspaceProfile fromValue(String value) {
		if (value == null || value.isBlank()) {
			return TRACTUSX;
		}
		String normalized = value.trim().replace("-", "").replace("_", "").toUpperCase(Locale.ROOT);
		return Arrays.stream(values())
				.filter(profile -> profile.name().equals(normalized))
				.findFirst()
				.orElseThrow(() -> new IllegalArgumentException("Unknown sde.dataspace.profile '" + value
						+ "', expected one of " + Arrays.toString(values())));
	}

	public enum ParticipantIdType {
		BPN, DID
	}
}
