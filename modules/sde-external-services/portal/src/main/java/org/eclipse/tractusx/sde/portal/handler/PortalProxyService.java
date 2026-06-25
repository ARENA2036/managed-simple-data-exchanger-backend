/********************************************************************************
 * Copyright (c) 2023,2024 T-Systems International GmbH
 * Copyright (c) 2026 ARENA2036 e.V.
 * Copyright (c) 2023,2024,2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.sde.portal.handler;

import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.sde.portal.api.IPortalExternalServiceApi;
import org.eclipse.tractusx.sde.portal.model.ConnectorInfo;
import org.eclipse.tractusx.sde.portal.model.response.UnifiedBPNValidationStatusEnum;
import org.eclipse.tractusx.sde.portal.model.response.UnifiedBpnValidationResponse;
import org.eclipse.tractusx.sde.portal.utils.MemberCompanyBPNCacheUtilityService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Slf4j
@Service
@RequiredArgsConstructor
public class PortalProxyService {

	private final MemberCompanyBPNCacheUtilityService cacheUtilityService;

	private final IPortalExternalServiceApi portalExternalServiceApi;
	@Value("${bpdm.provider.bpnl:BPNL00000003AYRE}")
	private String providerBPNL;

	@SneakyThrows
	public List<ConnectorInfo> fetchConnectorInfo(List<String> bpns) {
//		log.info("➡️ Calling portalExternalServiceApi.fetchConnectorInfo with: {}", bpns);
		List<ConnectorInfo> response = portalExternalServiceApi.fetchConnectorInfo(bpns);

//		log.info("⬅️ Received ConnectorInfo from portalExternalServiceApi for {} : {}", bpns, response);
		response.stream().filter(entry -> providerBPNL.equals(entry.getBpn())).forEach(connector -> connector.setConnectorEndpoint(List.of("https://dataprovider-edc-controlplane.staging.arena2036-x.de/api/v1/dsp")));

		return response;
	}

	@SneakyThrows
	public UnifiedBpnValidationResponse unifiedBpnValidation(String bpn) {

		List<ConnectorInfo> connectorsInfo = fetchConnectorInfo(List.of(bpn));

		UnifiedBpnValidationResponse unifiedBpnValidationResponse = UnifiedBpnValidationResponse.builder()
				.msg(bpn + " BPN number found valid connector's in partner network")
				.bpnStatus(UnifiedBPNValidationStatusEnum.FULL_PARTNER).build();

		if (connectorsInfo.isEmpty()) {

			List<String> memberBPNDataList = cacheUtilityService.getAllPartners();
			if (!memberBPNDataList.isEmpty() && memberBPNDataList.contains(bpn)) {
				unifiedBpnValidationResponse
						.setMsg(bpn + " BPN number is part of partner network but there is no valid connector's found");
				unifiedBpnValidationResponse.setBpnStatus(UnifiedBPNValidationStatusEnum.PARTNER);
			} else {
				unifiedBpnValidationResponse.setMsg(bpn + " BPN number is not part of partner network");
				unifiedBpnValidationResponse.setBpnStatus(UnifiedBPNValidationStatusEnum.NOT_PARTNER);
			}
		}
		return unifiedBpnValidationResponse;
	}
}
