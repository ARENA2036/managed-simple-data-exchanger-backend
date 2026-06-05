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
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

/**
 * Static regression test connecting controller security annotations with seeded permissions.
 *
 * <p>The application authorizes endpoints through permission names referenced from controller
 * annotations and seeded through Flyway role-permission data. These tests scan the current source and
 * seed files to ensure controller permissions are represented in the database seed set. This catches
 * documentation and security drift early without starting the application or depending on Keycloak.</p>
 */
class ControllerPermissionSeedCoverageTest {

	private static final Path CONTROLLER_ROOT = Path.of("src/main/java/org/eclipse/tractusx/sde/core/controller");
	private static final Path FLYWAY_ROOT = Path.of("src/main/resources/flyway");
	private static final Pattern PERMISSION_PATTERN = Pattern.compile("hasPermission\\([^,]+,'([^']+)'\\)");
	private static final Pattern SEEDED_PERMISSION_PATTERN = Pattern.compile("VALUES \\('([^']+)','");

	@Test
	void everyPermissionReferencedByControllersIsSeededByFlyway() throws IOException {
		Set<String> controllerPermissions = controllerPermissions();
		Set<String> seededPermissions = seededPermissions();

		assertThat(controllerPermissions)
				.isNotEmpty()
				.isSubsetOf(seededPermissions);
	}

	private Set<String> controllerPermissions() throws IOException {
		Set<String> permissions = new TreeSet<>();
		for (Path path : javaFiles(CONTROLLER_ROOT)) {
			String source = Files.readString(path);
			PERMISSION_PATTERN.matcher(source).results()
					.map(result -> result.group(1))
					.flatMap(value -> java.util.Arrays.stream(value.split("@")))
					.forEach(permissions::add);
		}
		return permissions;
	}

	private Set<String> seededPermissions() throws IOException {
		Set<String> permissions = new TreeSet<>();
		for (Path path : sqlFiles(FLYWAY_ROOT)) {
			SEEDED_PERMISSION_PATTERN.matcher(Files.readString(path)).results()
					.map(result -> result.group(1))
					.forEach(permissions::add);
		}
		return permissions;
	}

	private Set<Path> javaFiles(Path root) throws IOException {
		try (var paths = Files.walk(root)) {
			return paths.filter(path -> path.toString().endsWith(".java")).collect(Collectors.toCollection(TreeSet::new));
		}
	}

	private Set<Path> sqlFiles(Path root) throws IOException {
		try (var paths = Files.walk(root)) {
			return paths.filter(path -> path.toString().endsWith(".sql")).collect(Collectors.toCollection(TreeSet::new));
		}
	}
}
