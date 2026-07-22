/********************************************************************************
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
