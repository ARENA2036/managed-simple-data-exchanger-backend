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

package org.eclipse.tractusx.sde.pcfexchange.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.tractusx.sde.common.model.PagingResponse;
import org.eclipse.tractusx.sde.pcfexchange.entity.PcfRequestEntity;
import org.eclipse.tractusx.sde.pcfexchange.enums.PCFRequestStatusEnum;
import org.eclipse.tractusx.sde.pcfexchange.enums.PCFTypeEnum;
import org.eclipse.tractusx.sde.pcfexchange.mapper.PcfExchangeMapper;
import org.eclipse.tractusx.sde.pcfexchange.repository.PcfRequestRepository;
import org.eclipse.tractusx.sde.pcfexchange.request.PcfRequestModel;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

/**
 * Tests the PCF request repository service state machine.
 *
 * <p>The service maps asynchronous PCF provider/consumer workflows to persisted request statuses.
 * These tests cover the current transition rules, status persistence, and paged query branching with
 * repository and mapper fakes, because this state machine is core business logic and should fail fast
 * during refactoring.</p>
 */
class PCFRepositoryServiceCoreLogicTest {

	@Test
	void identifyRunningStatusChangesApprovedToPushingDataAndPersists() {
		PcfRequestRepository repository = mock(PcfRequestRepository.class);
		PcfRequestEntity entity = entity("request-1", PCFRequestStatusEnum.APPROVED);
		when(repository.getReferenceById("request-1")).thenReturn(entity);
		when(repository.save(entity)).thenReturn(entity);
		PCFRepositoryService service = new PCFRepositoryService(repository, new FakePcfExchangeMapper());

		PCFRequestStatusEnum result = service.identifyRunningStatus("request-1", PCFRequestStatusEnum.APPROVED);

		assertThat(result).isEqualTo(PCFRequestStatusEnum.PUSHING_DATA);
		assertThat(entity.getStatus()).isEqualTo(PCFRequestStatusEnum.PUSHING_DATA);
		verify(repository).save(entity);
	}

	@Test
	void identifyRunningStatusChangesRejectedToSendingRejectNotificationAndPersists() {
		PcfRequestRepository repository = mock(PcfRequestRepository.class);
		PcfRequestEntity entity = entity("request-1", PCFRequestStatusEnum.REJECTED);
		when(repository.getReferenceById("request-1")).thenReturn(entity);
		when(repository.save(entity)).thenReturn(entity);
		PCFRepositoryService service = new PCFRepositoryService(repository, new FakePcfExchangeMapper());

		PCFRequestStatusEnum result = service.identifyRunningStatus("request-1", PCFRequestStatusEnum.REJECTED);

		assertThat(result).isEqualTo(PCFRequestStatusEnum.SENDING_REJECT_NOTIFICATION);
		assertThat(entity.getStatus()).isEqualTo(PCFRequestStatusEnum.SENDING_REJECT_NOTIFICATION);
	}

	@Test
	void updatePCFPushStatusMapsSuccessfulApprovalPushToPushed() {
		PcfRequestRepository repository = mock(PcfRequestRepository.class);
		PcfRequestEntity entity = entity("request-1", PCFRequestStatusEnum.PUSHING_DATA);
		when(repository.getReferenceById("request-1")).thenReturn(entity);
		when(repository.save(entity)).thenReturn(entity);
		PCFRepositoryService service = new PCFRepositoryService(repository, new FakePcfExchangeMapper());

		PCFRequestStatusEnum result = service.updatePCFPushStatus(PCFRequestStatusEnum.APPROVED, "request-1", "SUCCESS");

		assertThat(result).isEqualTo(PCFRequestStatusEnum.PUSHED);
		assertThat(entity.getStatus()).isEqualTo(PCFRequestStatusEnum.PUSHED);
		assertThat(entity.getRemark()).isEqualTo("PCF data successfuly pushed");
	}

	@Test
	void updatePCFPushStatusMapsFailedApprovalPushToFailedToPushData() {
		PcfRequestRepository repository = mock(PcfRequestRepository.class);
		PcfRequestEntity entity = entity("request-1", PCFRequestStatusEnum.APPROVED);
		when(repository.getReferenceById("request-1")).thenReturn(entity);
		when(repository.save(entity)).thenReturn(entity);
		PCFRepositoryService service = new PCFRepositoryService(repository, new FakePcfExchangeMapper());

		PCFRequestStatusEnum result = service.updatePCFPushStatus(PCFRequestStatusEnum.APPROVED, "request-1", "FAILED");

		assertThat(result).isEqualTo(PCFRequestStatusEnum.FAILED_TO_PUSH_DATA);
		assertThat(entity.getStatus()).isEqualTo(PCFRequestStatusEnum.FAILED_TO_PUSH_DATA);
		assertThat(entity.getRemark()).isEqualTo("FAILED");
	}

	@Test
	void getPcfDataUsesTypeOnlyQueryWhenStatusIsNull() {
		PcfRequestRepository repository = mock(PcfRequestRepository.class);
		PcfRequestEntity entity = entity("request-1", PCFRequestStatusEnum.REQUESTED);
		when(repository.findByType(any(PageRequest.class), eq(PCFTypeEnum.PROVIDER)))
				.thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(0, 10), 1));
		PCFRepositoryService service = new PCFRepositoryService(repository, new FakePcfExchangeMapper());

		PagingResponse response = service.getPcfData(null, PCFTypeEnum.PROVIDER, 0, 10);

		assertThat((List<?>) response.getItems()).hasSize(1);
		assertThat(response.getTotalItems()).isEqualTo(1);
		verify(repository).findByType(any(PageRequest.class), eq(PCFTypeEnum.PROVIDER));
	}

	@Test
	void getPcfDataUsesStatusFilteredQueryWhenStatusesAreProvided() {
		PcfRequestRepository repository = mock(PcfRequestRepository.class);
		PcfRequestEntity entity = entity("request-1", PCFRequestStatusEnum.REQUESTED);
		List<PCFRequestStatusEnum> statuses = List.of(PCFRequestStatusEnum.REQUESTED);
		when(repository.findByTypeAndStatusIn(any(PageRequest.class), eq(PCFTypeEnum.CONSUMER), eq(statuses)))
				.thenReturn(new PageImpl<>(List.of(entity), PageRequest.of(1, 5), 6));
		PCFRepositoryService service = new PCFRepositoryService(repository, new FakePcfExchangeMapper());

		PagingResponse response = service.getPcfData(statuses, PCFTypeEnum.CONSUMER, 1, 5);

		assertThat(response.getPage()).isEqualTo(1);
		assertThat(response.getPageSize()).isEqualTo(5);
		assertThat(response.getTotalItems()).isEqualTo(6);
		verify(repository).findByTypeAndStatusIn(any(PageRequest.class), eq(PCFTypeEnum.CONSUMER), eq(statuses));
	}

	private static PcfRequestEntity entity(String requestId, PCFRequestStatusEnum status) {
		PcfRequestEntity entity = new PcfRequestEntity();
		entity.setRequestId(requestId);
		entity.setProductId("product-1");
		entity.setBpnNumber("BPNL00000003CML1");
		entity.setType(PCFTypeEnum.PROVIDER);
		entity.setStatus(status);
		return entity;
	}

	private static final class FakePcfExchangeMapper implements PcfExchangeMapper {
		@Override
		public PcfRequestModel mapFrom(PcfRequestEntity entity) {
			return PcfRequestModel.builder()
					.requestId(entity.getRequestId())
					.productId(entity.getProductId())
					.bpnNumber(entity.getBpnNumber())
					.type(entity.getType())
					.status(entity.getStatus())
					.remark(entity.getRemark())
					.build();
		}

		@Override
		public PcfRequestEntity mapFrom(PcfRequestModel pojo) {
			PcfRequestEntity entity = new PcfRequestEntity();
			entity.setRequestId(pojo.getRequestId());
			entity.setProductId(pojo.getProductId());
			entity.setBpnNumber(pojo.getBpnNumber());
			entity.setType(pojo.getType());
			entity.setStatus(pojo.getStatus());
			entity.setRemark(pojo.getRemark());
			return entity;
		}

		@Override
		public List<PcfRequestModel> mapFrom(List<PcfRequestEntity> entity) {
			return entity.stream().map(this::mapFrom).toList();
		}
	}
}
