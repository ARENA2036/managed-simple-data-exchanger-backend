/********************************************************************************
 * Copyright (c) 2022 BMW GmbH
 * Copyright (c) 2022,2024 T-Systems International GmbH
 * Copyright (c) 2022,2024 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.sde.edc.entities.request.asset;

import java.util.HashMap;
import java.util.Map;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.tractusx.sde.common.configuration.properties.DigitalTwinConfigurationProperties;
import org.eclipse.tractusx.sde.common.configuration.properties.EDCVersionConfigurationProperties;
import org.eclipse.tractusx.sde.common.utils.UUIdGenerator;
import org.eclipse.tractusx.sde.edc.constants.EDCAssetConfigurableConstant;
import org.eclipse.tractusx.sde.edc.constants.EDCAssetConstant;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

@Slf4j
@Service
@RequiredArgsConstructor
public class AssetEntryRequestFactory {

	@Getter
    @Value(value = "${submodel.datasource.hostname}")
	private String submodelDatasourceHostname;

	@Value(value = "${manufacturerId}")
	private String manufacturerId;

	@Value(value = "${edc.hostname}")
	private String edcEndpoint;

	@Value(value = "${spring.security.oauth2.resourceserver.jwt.issuer-uri}")
	private String idpIssuerTokenURL;

	@Value(value = "${digital-twins.authentication.clientId}")
	private String clientId;

	@Value(value = "${digital-twins.authentication.enable}")
	private boolean digitalTwinRegistryAuthEnable;

	@Value(value = "${submodel.datasource.enable:false}")
	private boolean useSeparatedSubmodelServer;

	private final EDCAssetConfigurableConstant edcAssetConfigurableConstant;

	private final EDCVersionConfigurationProperties edcVersionProperties;

    private final DigitalTwinConfigurationProperties digitalTwinConfigurationProperties;

    @SneakyThrows
	public  String createAssetId(String shellId, String subModelId) {
		return shellId + "-" + subModelId;
	}

	public AssetEntryRequest createDigitalTwinRegistryAssetRequest(String baseUrl, String registryType){
		String assetId = "sde:asset:dtr:" + UUIdGenerator.getUuid();

        HashMap<String, String> dataAddressProperties = createDigitalTwinRegistryDataAddressProperties(baseUrl);
        DataAddressRequest dataAddressRequest = DataAddressRequest.builder().properties(dataAddressProperties).build();

        HashMap<String, Object> assetProperties = createDtrAssetProperties(assetId, registryType, baseUrl);

        return AssetEntryRequest.builder()
				.id(assetId)
				.properties(assetProperties)
				.dataAddress(dataAddressRequest)
				.build();
	}

	public AssetEntryRequest createAssetRequest(String submodel, String assetName, String shellId, String subModelId,
                                                String submoduleUriPath, String uuid, String sematicId, String dctType) {

		String assetId = createAssetId(shellId, subModelId);

		HashMap<String, Object> assetProperties = createAssetProperties(assetId, assetName, sematicId, dctType);

        String uriString;
        UriComponentsBuilder uriBuilder = UriComponentsBuilder.fromUriString(submodelDatasourceHostname);
        if(useSeparatedSubmodelServer){
            uriString =  uriBuilder
                    .pathSegment(createAssetId(shellId, submodel))
                    .toUriString();
        } else {
            uriString = uriBuilder
                    .pathSegment(submodel)
                    .pathSegment(submoduleUriPath)
                    .pathSegment(uuid)
                    .toUriString();
        }

		HashMap<String, String> dataAddressProperties = getDataAddressProperties(shellId, subModelId, uriString);
		DataAddressRequest dataAddressRequest = DataAddressRequest.builder().properties(dataAddressProperties).build();

		return AssetEntryRequest.builder()
				.id(assetId)
				.properties(assetProperties)
				.dataAddress(dataAddressRequest)
				.build();
	}


	private HashMap<String, Object> createDtrAssetProperties(String assetId, String registryType, String baseUrl) {
		HashMap<String, Object> assetProperties = new HashMap<>();

		assetProperties.put(EDCAssetConstant.ASSET_PROP_ID, assetId);
		assetProperties.put(registryType, baseUrl);
		if(edcVersionProperties.getMinor() < 11){
			assetProperties.put(EDCAssetConstant.ASSET_PROP_CONTENTTYPE, EDCAssetConstant.ASSET_PROP_CONTENT_TYPE);
		}
		assetProperties.put(EDCAssetConstant.CX_COMMON_VERSION, edcAssetConfigurableConstant.getAssetPropCommonVersion());
		if (StringUtils.isNotBlank(edcAssetConfigurableConstant.getAssetPropTypeDigitalTwin())) {
			Object propValue= Map.of("@id",
					EDCAssetConstant.CX_TAXO_PREFIX + edcAssetConfigurableConstant.getAssetPropTypeDigitalTwin());
			assetProperties.put(EDCAssetConstant.DCT_TYPE, propValue);
		} else {
			log.error("EDC DTR Asset creation error: dct:type is missing!" );
		}


		return assetProperties;
	}

	private HashMap<String, Object> createAssetProperties(String assetId, String assetName, String sematicId, String edcAssetType) {
		HashMap<String, Object> assetProperties = new HashMap<>();
		assetProperties.put(EDCAssetConstant.ASSET_PROP_ID, assetId);
        if(edcVersionProperties.getMinor() < 11){
            assetProperties.put(EDCAssetConstant.ASSET_PROP_CONTENTTYPE, EDCAssetConstant.ASSET_PROP_CONTENT_TYPE);
        }
		assetProperties.put(EDCAssetConstant.ASSET_PROP_VERSION, EDCAssetConstant.ASSET_PROP_VERSION_VALUE);
		assetProperties.put(EDCAssetConstant.ASSET_PROP_NAME, assetName);
		assetProperties.put(EDCAssetConstant.RDFS_LABEL, assetName);
		assetProperties.put(EDCAssetConstant.RDFS_COMMENT, assetName);
		assetProperties.put(EDCAssetConstant.DCAT_VERSION, edcAssetConfigurableConstant.getAssetPropDcatVersion());
        assetProperties.put(EDCAssetConstant.CX_COMMON_VERSION,
                edcAssetConfigurableConstant.getAssetPropCommonVersion());
        if(edcAssetConfigurableConstant.getAssetPropTypeDigitalTwin().equalsIgnoreCase(edcAssetType)){
            assetProperties.put(EDCAssetConstant.CX_COMMON_VERSION,
                    edcAssetConfigurableConstant.getAssetPropCommonVersion());
        }

		if (StringUtils.isNotBlank(sematicId))
			assetProperties.put(EDCAssetConstant.AAS_SEMANTICS_SEMANTIC_ID, Map.of("@id", sematicId));

		if (StringUtils.isNotBlank(edcAssetType)) {
			Object propValue= Map.of("@id", EDCAssetConstant.CX_TAXO_PREFIX + edcAssetType);
			if(edcVersionProperties.getMinor() >= 11){
				propValue =  EDCAssetConstant.CX_TAXO_PREFIX + edcAssetType;
			}
			assetProperties.put(EDCAssetConstant.DCT_TYPE, propValue);
			assetProperties.put(EDCAssetConstant.ASSET_PROP_TYPE, edcAssetType);
		} else {
			assetProperties.put(EDCAssetConstant.ASSET_PROP_TYPE,
					edcAssetConfigurableConstant.getAssetPropTypeDefaultValue());
		}
		return assetProperties;
	}

	private HashMap<String, String> getDataAddressProperties(String shellId, String subModelId, String endpoint) {
		HashMap<String, String> dataAddressProperties = new HashMap<>();
		dataAddressProperties.put("type", EDCAssetConstant.TYPE);
		if (StringUtils.isBlank(shellId) && StringUtils.isBlank(subModelId) ){
			dataAddressProperties.put("baseUrl", String.format(endpoint, shellId, subModelId));
		} else {
			dataAddressProperties.put("baseUrl", endpoint);
		}
		dataAddressProperties.put("oauth2:tokenUrl", idpIssuerTokenURL + "/protocol/openid-connect/token");
		dataAddressProperties.put("oauth2:clientId", clientId);
		dataAddressProperties.put("oauth2:clientSecretKey", "client-secret");
		dataAddressProperties.put("proxyMethod", "true");
		dataAddressProperties.put("proxyBody", "true");
		dataAddressProperties.put("proxyPath", "true");
		dataAddressProperties.put("proxyQueryParams", "true");
		if(edcVersionProperties.getMinor() < 11){
			dataAddressProperties.put("contentType", EDCAssetConstant.ASSET_PROP_CONTENT_TYPE);
		}
		return dataAddressProperties;
	}


    private HashMap<String, String> createDigitalTwinRegistryDataAddressProperties(String baseUrl) {
        HashMap<String, String> dataAddressProperties = new HashMap<>();
        dataAddressProperties.put("type", EDCAssetConstant.TYPE);
        dataAddressProperties.put("baseUrl", baseUrl);
        dataAddressProperties.put("proxyMethod", "true");
        dataAddressProperties.put("proxyBody", "true");
        dataAddressProperties.put("proxyPath", "true");
        dataAddressProperties.put("proxyQueryParams", "true");
        if(edcVersionProperties.getMinor() < 11){
            dataAddressProperties.put("contentType", EDCAssetConstant.ASSET_PROP_CONTENT_TYPE);
        }
		if(digitalTwinRegistryAuthEnable){
			dataAddressProperties.put("oauth2:tokenUrl", digitalTwinConfigurationProperties.getDigitalTwinsAuthenticationUrl());
			dataAddressProperties.put("oauth2:clientId", digitalTwinConfigurationProperties.getDigitalTwinsAuthenticationClientId());
			dataAddressProperties.put("oauth2:clientSecret", digitalTwinConfigurationProperties.getDigitalTwinsAuthenticationClientSecret());
			if (StringUtils.isNotBlank(digitalTwinConfigurationProperties.getDigitalTwinsAuthenticationScope())) {
				dataAddressProperties.put("oauth2:scope", digitalTwinConfigurationProperties.getDigitalTwinsAuthenticationScope());
			}
		}
        return dataAddressProperties;
    }

}