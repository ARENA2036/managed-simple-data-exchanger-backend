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
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.sde.edc.entities.request.policies;

import static org.assertj.core.api.Assertions.assertThat;
import static org.eclipse.tractusx.sde.edc.entities.request.policies.PolicyConstraintBuilderService.ACCESS_POLICY_TYPE;
import static org.eclipse.tractusx.sde.edc.entities.request.policies.PolicyConstraintBuilderService.USAGE_POLICY_TYPE;
import static org.mockito.Mockito.mock;

import java.util.List;

import org.eclipse.tractusx.sde.common.configuration.properties.EDCVersionConfigurationProperties;
import org.eclipse.tractusx.sde.common.mapper.JsonObjectMapper;
import org.eclipse.tractusx.sde.edc.constants.EDCAssetConfigurableConstant;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

class PolicyRequestFactoryTest {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    @Test
    void edc011SerializesAccessPolicyWithAccessActionAndSaturnTerms() throws Exception {
        JsonNode policy = createPolicy(ACCESS_POLICY_TYPE);

        assertThat(policy.at("/policy/@type").asText()).isEqualTo("Set");
        assertThat(policy.at("/policy/permission/0/action").asText()).isEqualTo("access");
        assertThat(policy.at("/policy/permission/0/constraint/and").isArray()).isTrue();
        assertThat(policy.at("/policy/odrl:permission").isMissingNode()).isTrue();
    }

    @Test
    void edc011KeepsUseActionForContractPolicy() throws Exception {
        JsonNode policy = createPolicy(USAGE_POLICY_TYPE);

        assertThat(policy.at("/policy/permission/0/action").asText()).isEqualTo("use");
    }

    private JsonNode createPolicy(String type) throws Exception {
        EDCAssetConfigurableConstant constants = mock(EDCAssetConfigurableConstant.class);

        EDCVersionConfigurationProperties version = new EDCVersionConfigurationProperties();
        version.setMajor(0);
        version.setMinor(11);
        version.setNano(1);

        ActionRequest constraints = new ActionRequest();
        constraints.addProperty("and", List.of());

        PolicyRequestFactory factory = new PolicyRequestFactory(constants, version, mock(JsonObjectMapper.class));
        PolicyDefinitionRequest definition = factory.getPolicy("policy-id", "registry-asset", List.of(constraints), type);

        return MAPPER.readTree(definition.toJsonString());
    }
}
