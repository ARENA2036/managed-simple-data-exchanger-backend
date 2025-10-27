package org.eclipse.tractusx.sde.digitaltwins.gateways.external;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(value = "SubmodelServerClient", url = "${dft.hostname}")
public interface SubmodelServerClient {

    @PostMapping(path = "/{assetId}", consumes = "application/json")
    ResponseEntity<String> createSubModel(@PathVariable("assetId") String assetId, @RequestBody JsonNode requestBody);

}
