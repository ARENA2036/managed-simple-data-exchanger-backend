/********************************************************************************
 * Copyright (c) 2026 ARENA2036 e.V.
 * Copyright (c) 2026 Contributors to the Eclipse Foundation
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

package org.eclipse.tractusx.sde.core.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.eclipse.tractusx.sde.common.enums.ProgressStatusEnum;
import org.eclipse.tractusx.sde.core.processreport.ProcessReportUseCase;
import org.eclipse.tractusx.sde.core.processreport.model.ProcessFailureDetails;
import org.eclipse.tractusx.sde.core.processreport.model.ProcessReport;
import org.eclipse.tractusx.sde.core.processreport.model.ProcessReportPageResponse;
import org.eclipse.tractusx.sde.core.service.SubmodelCsvService;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;

/**
 * Documents the controller contract for provider process reports and report detail lookups.
 *
 * <p>The tests check pagination defaults, successful report lookup, not-found handling, failure
 * detail retrieval, and submodel-specific CSV history delegation. This controller is a key read side
 * for provider processing, so the tests focus on preserving request-to-service parameter mapping and
 * response status decisions during later service or API refactorings.</p>
 */
class ProcessReportControllerRegressionTest {

	private final ProcessReportUseCaseStub processReportUseCase = new ProcessReportUseCaseStub();
	private final SubmodelCsvServiceStub submodelCsvService = new SubmodelCsvServiceStub();
	private final ProcessReportController controller = new ProcessReportController(processReportUseCase,
			submodelCsvService);

	@Test
	void listProcessReportsDefaultsPagination() {
		ResponseEntity<ProcessReportPageResponse> response = controller.getProcessingReportsByDateDesc(null, null);

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(processReportUseCase.lastPage).isZero();
		assertThat(processReportUseCase.lastPageSize).isEqualTo(10);
		assertThat(response.getBody().getItems()).hasSize(1);
	}

	@Test
	void getProcessReportByIdReturnsReportWhenPresent() {
		ResponseEntity<ProcessReport> response = controller.getProcessReportById("process-1");

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(response.getBody().getProcessId()).isEqualTo("process-1");
	}

	@Test
	void getProcessReportByIdReturnsNotFoundWhenMissing() {
		assertThat(controller.getProcessReportById("missing").getStatusCode().value()).isEqualTo(404);
	}

	@Test
	void failureDetailsDelegatesProcessIdToUseCase() {
		ResponseEntity<List<ProcessFailureDetails>> response = controller
				.getProcessFailureDetailsReportById("process-1");

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(processReportUseCase.lastFailureProcessId).isEqualTo("process-1");
		assertThat(response.getBody()).extracting(ProcessFailureDetails::getLog).containsExactly("failed row");
	}

	@Test
	void successDetailsDelegatesSubmodelAndProcessIdToCsvService() {
		ResponseEntity<List<List<String>>> response = controller
				.getProcessSuccessDetailsReportById("process-1", "serialpart");

		assertThat(response.getStatusCode().value()).isEqualTo(200);
		assertThat(submodelCsvService.lastSubmodel).isEqualTo("serialpart");
		assertThat(submodelCsvService.lastProcessId).isEqualTo("process-1");
		assertThat(response.getBody()).containsExactly(List.of("manufacturerId"), List.of("BPNL00000003CML1"));
	}

	private static final class ProcessReportUseCaseStub extends ProcessReportUseCase {

		private int lastPage = -1;
		private int lastPageSize = -1;
		private String lastFailureProcessId;

		private ProcessReportUseCaseStub() {
			super(null, null, null, null);
		}

		@Override
		public ProcessReportPageResponse listAllProcessReports(int page, int size) {
			lastPage = page;
			lastPageSize = size;
			return ProcessReportPageResponse.builder()
					.page(page)
					.pageSize(size)
					.totalItems(1)
					.items(List.of(report("process-1")))
					.build();
		}

		@Override
		public ProcessReport getProcessReportById(String id) {
			if ("missing".equals(id)) {
				return null;
			}
			return report(id);
		}

		@Override
		public List<ProcessFailureDetails> getProcessFailureDetailsReportById(String id) {
			lastFailureProcessId = id;
			return List.of(ProcessFailureDetails.builder().processId(id).log("failed row").build());
		}

		private ProcessReport report(String id) {
			return ProcessReport.builder()
					.processId(id)
					.status(ProgressStatusEnum.COMPLETED)
					.csvType("SERIALPART")
					.build();
		}
	}

	private static final class SubmodelCsvServiceStub extends SubmodelCsvService {

		private String lastSubmodel;
		private String lastProcessId;

		private SubmodelCsvServiceStub() {
			super(null, null, null);
		}

		@Override
		public List<List<String>> findAllSubmodelCsvHistory(String submodel, String processId) {
			lastSubmodel = submodel;
			lastProcessId = processId;
			return List.of(List.of("manufacturerId"), List.of("BPNL00000003CML1"));
		}
	}
}
