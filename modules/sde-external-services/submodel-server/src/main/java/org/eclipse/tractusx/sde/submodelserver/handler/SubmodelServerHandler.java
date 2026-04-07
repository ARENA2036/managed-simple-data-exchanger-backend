package org.eclipse.tractusx.sde.submodelserver.handler;


import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.eclipse.tractusx.sde.common.constants.SubmoduleCommonColumnsConstant;
import org.eclipse.tractusx.sde.common.entities.PolicyModel;
import org.eclipse.tractusx.sde.common.submodel.executor.Step;
import org.eclipse.tractusx.sde.common.utils.JsonObjectUtility;
import org.eclipse.tractusx.sde.edc.entities.request.asset.AssetEntryRequestFactory;
import org.eclipse.tractusx.sde.submodelserver.api.SubmodelServerApi;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.net.URI;

@Slf4j
@RequiredArgsConstructor
@Service("submodelServerHandler")
public class SubmodelServerHandler extends Step {
    private final SubmodelServerApi submodelDatasourceClient;
    private final AssetEntryRequestFactory assetEntryRequestFactory;

    @Value(value = "${submodel.datasource.hostname}")
    private String submodelDatasourceHostname;

    @SneakyThrows
    public JsonNode run(Integer rowIndex, ObjectNode jsonObject, String processId, PolicyModel policy) {
        String shellId = JsonObjectUtility.getValueFromJsonObjectAsString(jsonObject,
                SubmoduleCommonColumnsConstant.SHELL_ID);
        String subModelId = JsonObjectUtility.getValueFromJsonObjectAsString(jsonObject,
                SubmoduleCommonColumnsConstant.SUBMODULE_ID);
        String assetId = assetEntryRequestFactory.createAssetId(shellId, subModelId);

        logDebug("%%% [SubmodelServer] start upload  AssetId: " + assetId + ", Submodel-Content: " + jsonObject);
        submodelDatasourceClient.uploadAsset(assetId, jsonObject);
        logDebug("%%% [SubmodelServer] uploaded: AssetId: " + assetId );
        return jsonObject;
    }

}
