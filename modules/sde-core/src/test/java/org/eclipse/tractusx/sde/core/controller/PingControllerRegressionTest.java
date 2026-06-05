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

import java.time.LocalDateTime;

import org.eclipse.tractusx.sde.edc.util.EDCAssetUrlCacheService;
import org.eclipse.tractusx.sde.portal.utils.MemberCompanyBPNCacheUtilityService;
import org.junit.jupiter.api.Test;

/**
 * Regression tests for simple operational endpoints exposed by {@link PingController}.
 *
 * <p>The controller provides a lightweight liveness response and cache-clear commands used during
 * local operation and troubleshooting. These tests verify the returned values and, more importantly,
 * that each cache endpoint delegates to the correct cache service method. The tests use direct
 * controller calls with stubs because no HTTP infrastructure is needed to protect this behavior.</p>
 */
class PingControllerRegressionTest {

	private final EdcCacheServiceStub edcCacheService = new EdcCacheServiceStub();
	private final MemberCompanyBpnCacheServiceStub bpnCacheService = new MemberCompanyBpnCacheServiceStub();
	private final PingController controller = new PingController(edcCacheService, bpnCacheService);

	@Test
	void pingReturnsParseableLocalDateTime() {
		String body = controller.getProcessReportById().getBody();

		assertThat(body).isNotBlank();
		assertThat(LocalDateTime.parse(body)).isNotNull();
	}

	@Test
	void clearMemberCompanyCacheDelegatesToBpnCacheService() {
		assertThat(controller.clearBpnnumberCache().getBody()).isEqualTo("Cleared");

		assertThat(bpnCacheService.clearCalls).isEqualTo(1);
	}

	@Test
	void clearDdtrCacheDelegatesToEdcCacheService() {
		assertThat(controller.clearDdtrurlCache().getBody()).isEqualTo("Cleared");

		assertThat(edcCacheService.clearDdtrCalls).isEqualTo(1);
	}

	@Test
	void clearPcfCacheDelegatesToEdcCacheService() {
		assertThat(controller.clearPCFExchangeUrlCache().getBody()).isEqualTo("Cleared");

		assertThat(edcCacheService.clearPcfCalls).isEqualTo(1);
	}

	private static final class EdcCacheServiceStub extends EDCAssetUrlCacheService {

		private int clearDdtrCalls;
		private int clearPcfCalls;

		private EdcCacheServiceStub() {
			super(null, null, null, null, null, null);
		}

		@Override
		public void clearDDTRUrlCache() {
			clearDdtrCalls++;
		}

		@Override
		public void clearPCFExchangeUrlCache() {
			clearPcfCalls++;
		}
	}

	private static final class MemberCompanyBpnCacheServiceStub extends MemberCompanyBPNCacheUtilityService {

		private int clearCalls;

		private MemberCompanyBpnCacheServiceStub() {
			super(null);
		}

		@Override
		public void removeAllBPNNumberCache() {
			clearCalls++;
		}
	}
}
