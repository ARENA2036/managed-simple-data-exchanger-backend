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

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Configuration
@ConfigurationProperties(prefix = "sde.constructx")
public class ConstructXPartnerDirectory {

	private List<Partner> partners = new ArrayList<>();

	public List<String> partnerIds() {
		return partners.stream().map(Partner::getId).filter(Objects::nonNull).distinct().toList();
	}

	public List<String> dspUrlsOf(String partnerId) {
		return partners.stream()
				.filter(partner -> Objects.equals(partner.getId(), partnerId))
				.map(Partner::getDspUrl)
				.filter(Objects::nonNull)
				.distinct()
				.toList();
	}

	@Data
	@NoArgsConstructor
	@AllArgsConstructor
	public static class Partner {
		private String id;
		private String name;
		private String dspUrl;
	}
}
