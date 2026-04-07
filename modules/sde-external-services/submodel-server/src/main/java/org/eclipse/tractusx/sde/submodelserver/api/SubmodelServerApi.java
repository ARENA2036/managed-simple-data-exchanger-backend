package org.eclipse.tractusx.sde.submodelserver.api;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;
import java.util.Map;

@FeignClient(value = "SubmodelServerAPI", url = "${submodel.datasource.hostname}")
public interface SubmodelServerApi {

    @PostMapping(path = "/{assetId}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> uploadAsset(
            @PathVariable String assetId,
            @RequestBody JsonNode requestBody
    );

    @PostMapping(path = "/{submodel}/{submodelUriPath}/{uuid}", consumes = MediaType.APPLICATION_JSON_VALUE)
    ResponseEntity<String> uploadAsset(
            @PathVariable String submodel,
            @PathVariable String submodelUriPath,
            @PathVariable String uuid,
            @RequestBody JsonNode requestBody
    );


    @GetMapping(path = "/{assetId}")
    ResponseEntity<String> downloadAsset(
            @PathVariable String assetId,
            @RequestHeader Map<String, String> header
    );

}
