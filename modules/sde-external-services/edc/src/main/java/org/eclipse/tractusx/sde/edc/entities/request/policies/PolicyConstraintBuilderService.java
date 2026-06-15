/********************************************************************************
 * Copyright (c) 2022,2024 T-Systems International GmbH
 * Copyright (c) 2026 ARENA2036 e.V.
 * Copyright (c) 2022,2024,2026 Contributors to the Eclipse Foundation
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

import java.util.*;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.tractusx.sde.common.configuration.properties.EDCVersionConfigurationProperties;
import org.eclipse.tractusx.sde.common.configuration.properties.SDEConfigurationProperties;
import org.eclipse.tractusx.sde.common.entities.Policies;
import org.eclipse.tractusx.sde.common.entities.PolicyModel;
import org.eclipse.tractusx.sde.common.mapper.JsonObjectMapper;
import org.eclipse.tractusx.sde.edc.constants.EDCAssetConfigurableConstant;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Slf4j
@Service
@RequiredArgsConstructor
public class PolicyConstraintBuilderService {

    public static final String USAGE_POLICY_TYPE = "u";

    public static final String ACCESS_POLICY_TYPE = "a";

    private final PolicyRequestFactory policyRequestFactory;

    private final JsonObjectMapper jsonobjectMapper;

    private final EDCAssetConfigurableConstant edcAssetConfigurableConstant;
    private final EDCVersionConfigurationProperties edcVersion;

    private final SDEConfigurationProperties sdeConfigurationProperties;

    public JsonNode getAccessPolicy(String policyId, String assetId, PolicyModel policy) {
        return jsonobjectMapper.objectToJsonNode(
                policyRequestFactory.getPolicy(
                        policyId,
                        assetId,
                        getPoliciesConstraints(policy.getAccessPolicies(), ACCESS_POLICY_TYPE),
                        ACCESS_POLICY_TYPE));
    }

    public JsonNode getUsagePolicy(String policyId, String assetId, PolicyModel policy) {
        return jsonobjectMapper.objectToJsonNode(
                policyRequestFactory.getPolicy(
                        policyId,
                        assetId,
                        getPoliciesConstraints(policy.getUsagePolicies(), USAGE_POLICY_TYPE),
                        USAGE_POLICY_TYPE));
    }

    public List<ActionRequest> getUsagePoliciesConstraints(List<Policies> policies) {
        return getPoliciesConstraints(policies, USAGE_POLICY_TYPE);
    }

    public List<ActionRequest> getPoliciesConstraints(List<Policies> policies, String type) {

        List<ConstraintRequest> allConstraints = new ArrayList<>();

        if (policies != null && !policies.isEmpty()) {
            policies.forEach(policy -> preparePolicyConstraint(allConstraints, policy, policy.getValue()));
        }

        // Sort constraints safely
        allConstraints.sort(
                Comparator.comparing(
                        ConstraintRequest::getConstraintLeftOperator,
                        Comparator.nullsLast(String::compareTo)
                )
        );

        // Wrap in ActionRequest
        ActionRequest action = new ActionRequest();
        action.addProperty(edcVersion.getMinor() >= 11 ? "and" : "odrl:and", allConstraints);

        return List.of(action);
    }

    private ActionRequest prepareActionRequest(String operator, List<ConstraintRequest> constraintList) {

        constraintList.sort(
                Comparator.comparing(
                        ConstraintRequest::getConstraintLeftOperator,
                        Comparator.nullsLast(String::compareTo)
                )
        );

        String logicalOperator = operator.replace("odrl:", "");

        ActionRequest action = new ActionRequest();
        String operatorPrefix = edcVersion.getMinor() >= 11 ? "" : "odrl:";
        action.addProperty(operatorPrefix + logicalOperator, constraintList);
        return action;
    }

    //RODO Extend to all allowed types
    private static final Set<String> ALLOWED_OPERANDS = Set.of(
            "Membership",
            "FrameworkAgreement",
            "BusinessPartnerNumber",
            "BusinessPartnerGroup",
            "UsagePurpose",
            "inForceDate"
    );

    private void preparePolicyConstraint(List<ConstraintRequest> constraints, Policies policy, List<String> values) {
        if (values == null || values.isEmpty()) return;

        String key = extractTechnicalKey(policy);

        // Skip unsupported operands
        if (!ALLOWED_OPERANDS.contains(key)) {
            log.info("*** Skipping unsupported policy operand: ", key);
            return;
        }

        // Determine operatorAsText
        String operatorAsText = edcVersion.getMinor() >= 11 ? "eq" : "odrl:eq";
        if ("BusinessPartnerGroup".equals(key)||"UsagePurpose".equals(key)) {
            operatorAsText = edcVersion.getMinor() >= 11 ? "isAnyOf" : "odrl:isAnyOf";
        }

        enrichConstraintRequests(constraints, values, operatorAsText, key);
    }

    private void enrichConstraintRequests(List<ConstraintRequest> constraints, List<String> values, String operatorAsText, String key) {
        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                Object rightOperand = operatorAsText.equals("odrl:isAnyOf") || operatorAsText.equals("isAnyOf")
                        ? List.of(value)
                        : value;
                Object leftOperand = edcVersion.getMinor() >= 11 ? key : Map.of("@id", "cx-policy:" + key);
                Object operator = edcVersion.getMinor() >= 11 ? operatorAsText : Map.of("@id", operatorAsText);
                ConstraintRequest request = ConstraintRequest.builder()
                        .useNameSpacePrefix(edcVersion.getMinor() < 11)
                        .leftOperand(leftOperand)
                        .operator(operator)
                        .rightOperand(rightOperand)
                        .build();

                constraints.add(request);
            }
        }
    }

    private static @NonNull String extractTechnicalKey(Policies policy) {
        String key = policy.getTechnicalKey();

        // NEXT STEP REQUIRED NO "CX-POLICY:"
        if (key.equals("cx-policy:Membership"))
            key = "Membership";
        if (key.equals("cx-policy:FrameworkAgreement"))
            key = "FrameworkAgreement";

        log.debug("preparePolicyConstraint");
        log.debug("ALLOWED_OPERANDS: {}", ALLOWED_OPERANDS);
        log.debug("key: {}", key);
        return key;
    }


    private List<String> getAndOwnerBPNIfNotExist(List<String> values) {

        if (!values.isEmpty()
                && !values.contains(sdeConfigurationProperties.getManufacturerId())
                && (values.size() == 1 && !values.get(0).equals(""))) {
            List<String> temp = new ArrayList<>(values);
            temp.add(sdeConfigurationProperties.getManufacturerId());
            values = temp;
        }

        return values;
    }

}
