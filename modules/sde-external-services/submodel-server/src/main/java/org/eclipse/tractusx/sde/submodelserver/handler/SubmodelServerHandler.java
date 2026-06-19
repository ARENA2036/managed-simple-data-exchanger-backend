/********************************************************************************
 * Copyright (c) 2022 BMW GmbH
 * Copyright (c) 2022,2024 T-Systems International GmbH
 * Copyright (c) 2025 ARENA2036 e.V.
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

package org.eclipse.tractusx.sde.submodelserver.handler;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.eclipse.tractusx.sde.common.constants.SubmoduleCommonColumnsConstant;
import org.eclipse.tractusx.sde.common.entities.PolicyModel;
import org.eclipse.tractusx.sde.common.submodel.executor.Step;
import org.eclipse.tractusx.sde.common.utils.JsonObjectUtility;
import org.eclipse.tractusx.sde.edc.entities.request.asset.AssetEntryRequestFactory;
import org.eclipse.tractusx.sde.submodelserver.api.SubmodelServerApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Slf4j
@RequiredArgsConstructor
@Service("submodelServerHandler")
public class SubmodelServerHandler extends Step {
    private final SubmodelServerApi submodelDatasourceClient;
    private final AssetEntryRequestFactory assetEntryRequestFactory;

    @Value(value = "${submodel.datasource.hostname}")
    private String submodelDatasourceHostname;

    @SneakyThrows
    public JsonNode run(Integer rowIndex, ObjectNode assetInfo, ObjectNode submodelData, PolicyModel policy, String processId) {
        String shellId = JsonObjectUtility.getValueFromJsonObjectAsString(assetInfo,
                SubmoduleCommonColumnsConstant.SHELL_ID);
        String subModelId = JsonObjectUtility.getValueFromJsonObjectAsString(assetInfo,
                SubmoduleCommonColumnsConstant.SUBMODULE_ID);
        String assetId = assetEntryRequestFactory.createAssetId(shellId, subModelId);

        logDebug("%%% [SubmodelServer] start upload  AssetId: " + assetId + ", Submodel-Content: " + assetInfo);
        submodelDatasourceClient.uploadAsset(assetId, submodelData);
        logDebug("%%% [SubmodelServer] uploaded: AssetId: " + assetId );
        return assetInfo;
    }

}
