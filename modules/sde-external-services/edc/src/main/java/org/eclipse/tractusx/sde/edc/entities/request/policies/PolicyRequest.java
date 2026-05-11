/********************************************************************************
 * Copyright (c) 2022 BMW GmbH
 * Copyright (c) 2022,2024 T-Systems International GmbH
 * Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
 * Copyright (c) 2026 ARENA2036 e.V.
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

package org.eclipse.tractusx.sde.edc.entities.request.policies;

import java.util.List;

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.eclipse.tractusx.sde.edc.model.policies.Obligation;
import org.eclipse.tractusx.sde.edc.model.policies.Prohibition;

import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(using = PolicyRequestSerializer.class)
public class PolicyRequest {
	@JsonIgnore
	private static final ObjectMapper MAPPER = new ObjectMapper();

	@JsonIgnore
	@Builder.Default
	private boolean useNameSpacePrefix = false;

	@JsonProperty("@type")
	@Builder.Default
	private String type = "Set"; //odrl:Set for jupiter Release
	
	@JsonProperty("@context")
	private List<Object> context;
	
	@JsonProperty("@id")
	private String id;
	
	@JsonAlias("odrl:permission")
    private List<PermissionRequest> permission;

	@JsonAlias("odrl:prohibition")
	private List<Prohibition> prohibition;

	@JsonAlias("odrl:obligation")
	private List<Obligation> obligation;

	@JsonAlias("odrl:profile")
	private String profile;

	@JsonAlias("odrl:target")
	private String target;
	
	@JsonAlias("odrl:assigner")
	private  String assigner;
	

	@SneakyThrows
	public String toJsonString() {
		return MAPPER.writeValueAsString(this);
	}
}