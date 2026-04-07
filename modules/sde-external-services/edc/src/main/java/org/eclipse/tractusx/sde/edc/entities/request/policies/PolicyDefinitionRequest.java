/********************************************************************************
 * Copyright (c) 2022 BMW GmbH
 * Copyright (c) 2022,2024 T-Systems International GmbH
 * Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
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

import com.fasterxml.jackson.annotation.*;
import com.fasterxml.jackson.annotation.JsonInclude.Include;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(value = Include.NON_NULL)
@JsonSerialize(using = PolicyDefinitionRequestSerializer.class)
public class PolicyDefinitionRequest {
    @JsonIgnore
    private static final ObjectMapper MAPPER = new ObjectMapper();

    @JsonIgnore
    @Builder.Default
    private boolean useNameSpacePrefix = false;

	@JsonAlias("@context")
	private Object context;

	@JsonAlias("@type")
	@Builder.Default
	private String polityRootType = "PolicyDefinition";

    @JsonAlias("odrl:profile") //odrl
    private String profile;

	@JsonProperty("@id")
    private String id;
	
    @JsonAlias("edc:policy")
    private PolicyRequest policy;

    @SneakyThrows
    public String toJsonString() {
        return MAPPER.writeValueAsString(this);
    }
}
