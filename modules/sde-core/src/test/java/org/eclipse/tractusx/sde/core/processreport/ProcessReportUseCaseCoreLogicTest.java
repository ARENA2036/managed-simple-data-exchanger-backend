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

package org.eclipse.tractusx.sde.core.processreport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.eclipse.tractusx.sde.common.enums.ProgressStatusEnum;
import org.eclipse.tractusx.sde.core.failurelog.mapper.FailureLogMapper;
import org.eclipse.tractusx.sde.core.failurelog.repository.FailureLogRepository;
import org.eclipse.tractusx.sde.core.processreport.entity.ProcessReportEntity;
import org.eclipse.tractusx.sde.core.processreport.mapper.ProcessReportMapper;
import org.eclipse.tractusx.sde.core.processreport.model.ProcessReport;
import org.eclipse.tractusx.sde.core.processreport.model.ProcessReportPageResponse;
import org.eclipse.tractusx.sde.core.processreport.repository.ProcessReportRepository;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * Tests the process-report use case that records provider workflow progress.
 *
 * <p>Process reports are the operational audit trail for uploads and deletes. These tests verify that
 * reports are initialized with the expected status and counters, completion updates delegate to the
 * repository with the correct values, and list responses preserve paging metadata. Repository and
 * mapper collaborators are mocked at interface boundaries.</p>
 */
class ProcessReportUseCaseCoreLogicTest {

	@Test
	void startBuildProcessReportPersistsInProgressReportWithUppercaseCsvType() {
		ProcessReportRepository repository = mock(ProcessReportRepository.class);
		CapturingProcessReportMapper mapper = new CapturingProcessReportMapper();
		ProcessReportUseCase useCase = new ProcessReportUseCase(repository, mock(FailureLogRepository.class), mapper,
				mock(FailureLogMapper.class));

		useCase.startBuildProcessReport("process-1", "csv", 12, List.of(), List.of(), "policy-1");

		ProcessReport saved = mapper.lastMappedReport;
		assertThat(saved.getProcessId()).isEqualTo("process-1");
		assertThat(saved.getCsvType()).isEqualTo("CSV");
		assertThat(saved.getStatus()).isEqualTo(ProgressStatusEnum.IN_PROGRESS);
		assertThat(saved.getNumberOfItems()).isEqualTo(12);
		assertThat(saved.getPolicyUuid()).isEqualTo("policy-1");
		verify(repository).save(any(ProcessReportEntity.class));
	}

	@Test
	void startDeleteProcessResetsCountersAndSetsReferenceProcessId() {
		ProcessReportRepository repository = mock(ProcessReportRepository.class);
		CapturingProcessReportMapper mapper = new CapturingProcessReportMapper();
		ProcessReportUseCase useCase = new ProcessReportUseCase(repository, mock(FailureLogRepository.class), mapper,
				mock(FailureLogMapper.class));
		ProcessReport oldReport = ProcessReport.builder()
				.processId("old-process")
				.numberOfDeletedItems(4)
				.numberOfFailedItems(3)
				.numberOfSucceededItems(2)
				.numberOfUpdatedItems(1)
				.build();

		useCase.startDeleteProcess(oldReport, "reference-process", "json", 5, "delete-process");

		assertThat(mapper.lastMappedReport.getProcessId()).isEqualTo("delete-process");
		assertThat(mapper.lastMappedReport.getReferenceProcessId()).isEqualTo("reference-process");
		assertThat(mapper.lastMappedReport.getCsvType()).isEqualTo("JSON");
		assertThat(mapper.lastMappedReport.getStatus()).isEqualTo(ProgressStatusEnum.IN_PROGRESS);
		assertThat(mapper.lastMappedReport.getNumberOfItems()).isEqualTo(5);
		assertThat(mapper.lastMappedReport.getNumberOfDeletedItems()).isZero();
		assertThat(mapper.lastMappedReport.getNumberOfFailedItems()).isZero();
		assertThat(mapper.lastMappedReport.getNumberOfSucceededItems()).isZero();
		assertThat(mapper.lastMappedReport.getNumberOfUpdatedItems()).isZero();
	}

	@Test
	void finishBuildProgressReportDelegatesFinalizeUpdateWithCompletedStatus() {
		ProcessReportRepository repository = mock(ProcessReportRepository.class);
		ProcessReportUseCase useCase = new ProcessReportUseCase(repository, mock(FailureLogRepository.class),
				new CapturingProcessReportMapper(), mock(FailureLogMapper.class));

		useCase.finishBuildProgressReport("process-1", 8, 2, 3);

		verify(repository).finalizeProgressReport(eq("process-1"), any(LocalDateTime.class),
				eq(ProgressStatusEnum.COMPLETED.toString()), eq(8), eq(2), eq(3L));
	}

	@Test
	void listAllProcessReportsMapsPageMetadata() {
		ProcessReportRepository repository = mock(ProcessReportRepository.class);
		ProcessReportEntity entity = new ProcessReportEntity();
		entity.setProcessId("process-1");
		when(repository.findAll(any(PageRequest.class)))
				.thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(1, 5), 6));
		ProcessReportUseCase useCase = new ProcessReportUseCase(repository, mock(FailureLogRepository.class),
				new CapturingProcessReportMapper(), mock(FailureLogMapper.class));

		ProcessReportPageResponse response = useCase.listAllProcessReports(1, 5);

		assertThat(response.getPage()).isEqualTo(1);
		assertThat(response.getPageSize()).isEqualTo(5);
		assertThat(response.getTotalItems()).isEqualTo(6);
		assertThat(response.getItems()).extracting(ProcessReport::getProcessId).containsExactly("process-1");
	}

	private static final class CapturingProcessReportMapper implements ProcessReportMapper {
		private ProcessReport lastMappedReport;

		@Override
		public ProcessReportEntity mapFrom(ProcessReport processReport) {
			lastMappedReport = processReport;
			ProcessReportEntity entity = new ProcessReportEntity();
			entity.setProcessId(processReport.getProcessId());
			return entity;
		}

		@Override
		public ProcessReport mapFrom(ProcessReportEntity processReportEntity) {
			return ProcessReport.builder().processId(processReportEntity.getProcessId()).build();
		}
	}
}
