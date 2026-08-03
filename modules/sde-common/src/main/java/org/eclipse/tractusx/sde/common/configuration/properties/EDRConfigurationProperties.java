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

package org.eclipse.tractusx.sde.common.configuration.properties;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

@Configuration
@ConfigurationProperties(prefix = "edr")
@Data
public class EDRConfigurationProperties {

    private Retry retry = new Retry();
    private Refresh refresh = new Refresh();

    @Data
    public static class Retry {
        // amount of max retries
        private Integer count = 5;
        //Time in ms
        private Integer time = 250;
    }

    @Data
    public static class Refresh {
        //enable/disable auto refresh of edr token
        private Boolean auto = false;
    }
}
