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

package org.eclipse.tractusx.sde.core.csv.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.nio.file.Files;
import java.nio.file.Path;

import org.eclipse.tractusx.sde.common.entities.csv.CsvContent;
import org.eclipse.tractusx.sde.common.exception.CsvException;
import org.eclipse.tractusx.sde.common.exception.ValidationException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

/**
 * Exercises the CSV upload lifecycle at the service boundary.
 *
 * <p>The service validates incoming multipart files, stores accepted CSV files under generated UUID
 * names, reads them back into the internal row model, and deletes temporary files after processing.
 * These tests cover the happy path plus negative paths for blank names, unsupported extensions, path
 * traversal, and missing files. The goal is to protect file-system safety and provider-upload
 * behavior without depending on controllers or external services.</p>
 */
class CsvHandlerServiceTest {

	@TempDir
	private Path uploadDir;

	@Test
	void storeFilePersistsCsvWithGeneratedUuidName() {
		CsvHandlerService service = service();

		String uuid = service.storeFile(csvFile("parts.csv", "manufacturerId;partInstanceId\nBPNL;urn:uuid:1"));

		assertThat(uuid).isNotBlank();
		assertThat(Path.of(service.getFilePath(uuid))).exists();
		assertThat(Path.of(service.getFilePath(uuid)).getFileName().toString()).isEqualTo(uuid + ".csv");
	}

	@Test
	void storeFileRejectsBlankFileName() {
		assertThatThrownBy(() -> service().storeFile(csvFile("", "a;b")))
				.isInstanceOf(ValidationException.class)
				.hasMessageContaining("valid CSV file");
	}

	@Test
	void storeFileRejectsUnsupportedFileExtension() {
		assertThatThrownBy(() -> service().storeFile(csvFile("parts.txt", "a;b")))
				.isInstanceOf(ValidationException.class)
				.hasMessageContaining("only supports .csv");
	}

	@Test
	void storeFileRejectsPathTraversalFileName() {
		assertThatThrownBy(() -> service().storeFile(csvFile("../parts.csv", "a;b")))
				.isInstanceOf(IllegalArgumentException.class)
				.hasMessageContaining("Invalid csv filename");
	}

	@Test
	void processFileReadsRowsAndDeletesFile() throws Exception {
		CsvHandlerService service = service();
		String uuid = service.storeFile(csvFile("parts.csv",
				"manufacturerId;partInstanceId\nBPNL00000003CML1;urn:uuid:123\nBPNL00000003CML2;urn:uuid:456"));

		CsvContent csvContent = service.processFile(uuid);

		assertThat(csvContent.getColumns()).containsExactly("manufacturerId", "partInstanceId");
		assertThat(csvContent.getRows()).hasSize(2);
		assertThat(csvContent.getRows().get(0).position()).isEqualTo(2);
		assertThat(csvContent.getRows().get(0).content()).isEqualTo("BPNL00000003CML1;urn:uuid:123");
		assertThat(Path.of(service.getFilePath(uuid))).doesNotExist();
	}

	@Test
	void processFileThrowsCsvExceptionForMissingFile() {
		assertThatThrownBy(() -> service().processFile("missing-file"))
				.isInstanceOf(CsvException.class)
				.hasMessageContaining("no such file");
	}

	@Test
	void deleteFileDeletesExistingStoredCsv() throws Exception {
		CsvHandlerService service = service();
		String uuid = service.storeFile(csvFile("parts.csv", "a;b"));

		assertThat(service.deleteFile(uuid)).isTrue();

		assertThat(Files.exists(Path.of(service.getFilePath(uuid)))).isFalse();
	}

	private CsvHandlerService service() {
		CsvConfigurationProperties properties = new CsvConfigurationProperties();
		properties.setUploadDir(uploadDir.toString());
		return new CsvHandlerService(properties);
	}

	private MockMultipartFile csvFile(String fileName, String content) {
		return new MockMultipartFile("file", fileName, "text/csv", content.getBytes());
	}
}
