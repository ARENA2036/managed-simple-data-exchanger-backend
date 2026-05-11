/********************************************************************************
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

package org.eclipse.tractusx.sde.edc.model.contractoffers;

import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import lombok.SneakyThrows;

@Component
public class ContractOfferRequestFactory {


	@SneakyThrows
	public ObjectNode getContractOfferRequest(String providerUrl, String counterPartyId, Integer limit, Integer offset,
			String filterExpression) {
        // TODO DBK: create an edc version related formatSchema for the filterexpression
		if (!StringUtils.isBlank(filterExpression))
			filterExpression = String.format(", %s", filterExpression);
		else
			filterExpression = "";
		
		String formatSchema = """
				{
				 "@context": {
				     "@vocab": "https://w3id.org/edc/v0.0.1/ns/",
				     "odrl": "http://www.w3.org/ns/odrl/2/",
				     "dct": "http://purl.org/dc/terms/"
				 },
				 "@type": "CatalogRequest",
				 "protocol": "dataspace-protocol-http:2025-1",
				 "counterPartyAddress": "%s/2025-1",
				 "counterPartyId":"%s",
				 "querySpec": {
				 "offset": %s,
				 "limit": %s
				 %s
				 }
				}
				""";
		String jsonString = String.format(formatSchema, providerUrl, counterPartyId, offset, limit,
				filterExpression);

		return (ObjectNode) new ObjectMapper().readTree(jsonString);
	}

}