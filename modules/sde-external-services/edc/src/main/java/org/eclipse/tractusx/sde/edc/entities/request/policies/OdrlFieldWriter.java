/********************************************************************************
 * Copyright (c) 2022 BMW GmbH
 * Copyright (c) 2022,2024 T-Systems International GmbH
 * Copyright (c) 2026 ARENA2036 e.V.
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

package org.eclipse.tractusx.sde.edc.entities.request.policies;

import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.databind.SerializerProvider;
import lombok.experimental.UtilityClass;

import java.io.IOException;

@UtilityClass
public class OdrlFieldWriter {
    public static final String ODRL_PREFIX = "odrl:";
    public static final String EDC_PREFIX = "odrl:";

    public static void writeOdrlField(JsonGenerator gen, SerializerProvider provider, boolean withPrefix, String baseName, Object fieldValue)
            throws IOException {
        if (fieldValue == null) return;
        String prefix = withPrefix ? ODRL_PREFIX: "";
        write(gen,provider,prefix,baseName,fieldValue);
    }

    public static void writeEdcField(JsonGenerator gen, SerializerProvider provider, boolean withPrefix, String baseName, Object fieldValue)
            throws IOException {
        if (fieldValue == null) return;
        String prefix = withPrefix ? EDC_PREFIX: "";
        write(gen,provider,prefix,baseName,fieldValue);
    }

    public static void write(
            JsonGenerator gen,
            SerializerProvider provider,
            String prefix,
            String baseName,
            Object fieldValue)
            throws IOException {
        if (fieldValue == null) return;
        gen.writeFieldName(prefix + baseName);
        provider.defaultSerializeValue(fieldValue, gen);
    }
}
