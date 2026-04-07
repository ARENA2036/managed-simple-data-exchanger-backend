/********************************************************************************
 * Copyright (c) 2022, 2023 T-Systems International GmbH
 * Copyright (c) 2022, 2023 Contributors to the Eclipse Foundation
 * Copyright (c) 2025 ARENA2036 e.V.
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
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.SneakyThrows;
import org.eclipse.tractusx.sde.common.utils.JsonUtil;

@NoArgsConstructor
@AllArgsConstructor
@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonSerialize(using = ConstraintRequestSerializer.class)
public class ConstraintRequest {
    @JsonIgnore
    private static final ObjectMapper MAPPER = new ObjectMapper();
    //TODO: adjust like PolicyRequest
    @JsonIgnore
    @Builder.Default
    private boolean useNameSpacePrefix = false;

    private String type;

    @JsonAlias("odrl:leftOperand")
    private Object leftOperand;

    @JsonAlias("odrl:rightOperand")
    private Object rightOperand;

    @JsonAlias("odrl:operator")
    private Object operator;

    @SneakyThrows
    public String toJsonString() {
        return MAPPER.writeValueAsString(this);
    }

    public static String getConstraintLeftOperator(ConstraintRequest c) {
        if (c.getLeftOperand() == null) {
            return null;
        }
        try {
            String leftOperandAsJson = MAPPER.writeValueAsString(c.getLeftOperand());
            if (JsonUtil.isJsonObjectString(leftOperandAsJson)) {
                JsonNode node = MAPPER.readTree(leftOperandAsJson);
                return node.get("@id").asText();
            }
            return leftOperandAsJson;
        } catch (JsonProcessingException e) {
            return null;
        }
    }
}
