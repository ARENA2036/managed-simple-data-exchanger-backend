/********************************************************************************
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

import java.util.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.tractusx.sde.common.configuration.properties.SDEConfigurationProperties;
import org.eclipse.tractusx.sde.common.entities.Policies;
import org.eclipse.tractusx.sde.common.entities.PolicyModel;
import org.eclipse.tractusx.sde.common.mapper.JsonObjectMapper;
import org.eclipse.tractusx.sde.edc.constants.EDCAssetConfigurableConstant;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.databind.JsonNode;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
public class PolicyConstraintBuilderService {

	private static final String U = "u";

	private static final String A = "a";

	private final PolicyRequestFactory policyRequestFactory;

	private final JsonObjectMapper jsonobjectMapper;

	private final EDCAssetConfigurableConstant edcAssetConfigurableConstant;

	private final SDEConfigurationProperties sdeConfigurationProperties;

//	private final IPolicyHubProxyService policyHubProxyService;
//
//	public JsonNode getAccessPolicy(String assetId, PolicyModel policy) {
//		
//		return jsonobjectMapper.objectToJsonNode(policyRequestFactory.setPolicyIdAndGetObject(assetId,
//				policyHubProxyService.getPolicyContent(
//						mapPolicy(PolicyTypeIdEnum.ACCESS, ConstraintOperandIdEnum.OR, policy.getAccessPolicies(), "a")),
//				"a"));
//	}
//
//	public JsonNode getUsagePolicy(String assetId, PolicyModel policy) {
//		
//		return jsonobjectMapper.objectToJsonNode(policyRequestFactory.setPolicyIdAndGetObject(assetId,
//				policyHubProxyService.getPolicyContent(
//						mapPolicy(PolicyTypeIdEnum.USAGE, ConstraintOperandIdEnum.AND, policy.getUsagePolicies(), "u")),
//				"u"));
//	}
//
//	private PolicyContentRequest mapPolicy(PolicyTypeIdEnum policyType, ConstraintOperandIdEnum constraintOperandId,
//			List<Policies> policies, String type) {
//
//		List<Constraint> constraintsList = new ArrayList<>();
//		policies.forEach(policy -> {
//			
//			List<String> valueList = policy.getValue();
//
//			//if (type.equals("a"))
//			//	valueList = getAndOwnerBPNIfNotExist(policy, valueList);
//			
//			OperatorIdEnum operator = OperatorIdEnum.EQUALS;
//
//			if (valueList.size() > 1) {
//				operator = OperatorIdEnum.IN;
//			}
//
//			for (String value : valueList) {
//				if (StringUtils.isNotBlank(value)) {
//					constraintsList.add(
//							Constraint.builder()
//							.key(policy.getTechnicalKey())
//							.operator(operator)
//							.value(value)
//							.build());
//				}
//			}
//		});
//
//		return PolicyContentRequest.builder()
//				.policyType(policyType)
//				.constraintOperand(constraintOperandId)
//				.constraints(constraintsList)
//				.build();
//	}

	public JsonNode getAccessPolicy(String policyId, String assetId, PolicyModel policy)  {
		return jsonobjectMapper.objectToJsonNode(
				policyRequestFactory.getPolicy(policyId, assetId, getPoliciesConstraints(policy.getAccessPolicies(), A), A));
	}

	public JsonNode getUsagePolicy( String policyId, String assetId, PolicyModel policy) {
		return jsonobjectMapper.objectToJsonNode(
				policyRequestFactory.getPolicy(policyId, assetId, getPoliciesConstraints(policy.getUsagePolicies(), U), U));
	}

	public List<ActionRequest> getUsagePoliciesConstraints(List<Policies> policies) {
		return getPoliciesConstraints(policies, U);
	}

    public List<ActionRequest> getPoliciesConstraints(List<Policies> policies, String type) {

        List<ConstraintRequest> allConstraints = new ArrayList<>();

        if (policies != null && !policies.isEmpty()) {
            policies.forEach(policy -> {
                preparePolicyConstraint(allConstraints, policy, policy.getValue());
            });
        }

        boolean hasFrameworkAgreement = allConstraints.stream()
                .anyMatch(c -> c.getLeftOperand() != null &&
                        "FrameworkAgreement".equalsIgnoreCase(c.getLeftOperand()));

        if (!hasFrameworkAgreement) {
            allConstraints.add(ConstraintRequest.builder()
                    .leftOperand("FrameworkAgreement")
                    .operator("eq")
                    .rightOperand("DataExchangeGovernance:1.0")
                    .build());
        }

        List<ActionRequest> actionList = new ArrayList<>();
        if (!allConstraints.isEmpty()) {

            allConstraints.sort(
                    Comparator.comparing(
                            ConstraintRequest::getLeftOperand,
                            Comparator.nullsLast(String::compareTo)
                    )
            );

            ActionRequest action = new ActionRequest();
            action.addProperty("and", allConstraints);
            actionList.add(action);
        }

        return actionList;
    }

    private ActionRequest prepareActionRequest(String operator, List<ConstraintRequest> constraintList) {

        constraintList.sort(
                Comparator.comparing(
                        ConstraintRequest::getLeftOperand,
                        Comparator.nullsLast(String::compareTo)
                )
        );

        String logicalOperator = operator.replace("odrl:", "");

        ActionRequest action = new ActionRequest();
        action.addProperty(logicalOperator, constraintList);
        return action;
    }

    private static final Set<String> ALLOWED_OPERANDS = Set.of(
            "Membership",
            "FrameworkAgreement",
            "BusinessPartnerNumber",
            "BusinessPartnerGroup",
            "inForceDate"
    );

    private void preparePolicyConstraint(List<ConstraintRequest> constraints, Policies policy, List<String> values) {
        if (values == null || values.isEmpty()) return;

        String key = policy.getTechnicalKey();

        if (!ALLOWED_OPERANDS.contains(key)) {
            System.out.println("***" + " Skipping unsupported policy operand: {}"+ key);

            return;
        }

        String operator;
        switch (key) {
            case "Membership":
            case "FrameworkAgreement":
            case "inForceDate":
                operator = "eq";
                break;
            case "BusinessPartnerNumber":
            case "BusinessPartnerGroup":
                operator = "isAnyOf";
                break;
            default:
                operator = "eq";
        }

        for (String value : values) {
            if (StringUtils.isNotBlank(value)) {
                Object rightOperand = "isAnyOf".equals(operator)
                        ? List.of(value)
                        : value;

                ConstraintRequest request = ConstraintRequest.builder()
                        .leftOperand(key)
                        .operator(operator)
                        .rightOperand(rightOperand)
                        .build();

                constraints.add(request);
            }
        }
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
