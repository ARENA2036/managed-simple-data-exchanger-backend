/********************************************************************************
 * Copyright (c) 2026 ARENA2036 e.V.
 *
 * See the NOTICE file(s) distributed with this work for additional
 * information regarding copyright ownership.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Apache License, Version 2.0 which is available at
 * https://www.apache.org/licenses/LICENSE-2.0.
 *
 * SPDX-License-Identifier: Apache-2.0
 ********************************************************************************/

package org.eclipse.tractusx.sde.openapi;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

import org.junit.jupiter.api.Test;

/**
 * Captures known drift between the static OpenAPI file and implemented controllers.
 *
 * <p>The project ships a checked-in OpenAPI description as well as annotated Spring controllers. These
 * tests document current mismatches, such as endpoints that are present only in one representation or
 * controller requirements that are not obvious from the static specification. The tests are not meant
 * to bless drift forever; they make it visible so API documentation refactoring can change the
 * expected assertions deliberately.</p>
 */
class OpenApiControllerDriftCurrentStateTest {

	private static final Path OPEN_API = Path.of("src/main/resources/sde-open-api.yml");
	private static final Path CONTROLLERS = Path.of("src/main/java/org/eclipse/tractusx/sde/core/controller");

	@Test
	void staticOpenApiDocumentsUploadEndpointThatHasNoControllerMapping() throws IOException {
		String openApi = Files.readString(OPEN_API);
		String controllerSources = readAllControllerSources();

		assertThat(openApi).contains("\n  /upload:");
		assertThat(controllerSources).doesNotContain("@PostMapping(value = \"/upload\")");
		assertThat(controllerSources).doesNotContain("@PostMapping(\"/upload\")");
	}

	@Test
	void staticOpenApiMissesControllerEndpointsDocumentedAsCurrentGaps() throws IOException {
		String openApi = Files.readString(OPEN_API);
		String controllerSources = readAllControllerSources();

		assertThat(controllerSources).contains("/submodels/csvfile/{submodelName}");
		assertThat(openApi).doesNotContain("/submodels/csvfile/{submodelName}:");

		assertThat(controllerSources).contains("/cache/clear-pcfurl");
		assertThat(openApi).doesNotContain("/cache/clear-pcfurl:");
	}

	@Test
	void pcfProductEndpointExpectsEdcBpnHeaderInController() throws IOException {
		String controllerSources = readAllControllerSources();

		assertThat(controllerSources).contains("@RequestHeader(value = \"Edc-Bpn\")");
	}

	private String readAllControllerSources() throws IOException {
		StringBuilder builder = new StringBuilder();
		try (var paths = Files.walk(CONTROLLERS)) {
			for (Path path : paths.filter(path -> path.toString().endsWith(".java")).toList()) {
				builder.append(Files.readString(path)).append('\n');
			}
		}
		return builder.toString();
	}
}
