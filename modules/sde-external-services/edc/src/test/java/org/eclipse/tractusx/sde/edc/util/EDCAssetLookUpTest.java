package org.eclipse.tractusx.sde.edc.util;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.eclipse.tractusx.sde.edc.entities.request.contractdefinition.Criterion;
import org.eclipse.tractusx.sde.edc.model.response.QueryDataOfferModel;
import org.eclipse.tractusx.sde.edc.services.CatalogResponseBuilder;
import org.eclipse.tractusx.sde.portal.handler.PortalProxyService;
import org.eclipse.tractusx.sde.portal.model.ConnectorInfo;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class EDCAssetLookUpTest {

	@Mock
	private PortalProxyService portalProxyService;

	@Mock
	private CatalogResponseBuilder catalogResponseBuilder;

	@InjectMocks
	private EDCAssetLookUp edcAssetLookUp;

	@Test
	void getEDCAssetsByTypeContinuesWhenOneConnectorFails() {
		ReflectionTestUtils.setField(edcAssetLookUp, "consumerHost", "https://consumer.example");

		List<Criterion> filter = List.of(Criterion.builder().operandLeft("a").operator("=").operandRight("b").build());
		ConnectorInfo connectorInfo = ConnectorInfo.builder()
				.bpn("BPNL000000000001")
				.connectorEndpoint(List.of("https://provider-1.example/api/v1/dsp", "https://provider-2.example/api/v1/dsp"))
				.build();

		when(catalogResponseBuilder.queryOnDataOffers(eq("https://provider-1.example/api/v1/dsp"), eq("BPNL000000000001"), eq(0), eq(100),
				anyString()))
				.thenThrow(new RuntimeException("upstream error"));
		when(catalogResponseBuilder.queryOnDataOffers(eq("https://provider-2.example/api/v1/dsp"), eq("BPNL000000000001"), eq(0), eq(100),
				anyString()))
				.thenReturn(List.of(QueryDataOfferModel.builder().assetId("asset-1").build()));

		List<QueryDataOfferModel> offers = edcAssetLookUp.getEDCAssetsByType(List.of(connectorInfo), filter);

		assertEquals(1, offers.size());
		assertEquals("asset-1", offers.get(0).getAssetId());
		assertEquals("https://provider-2.example/api/v1/dsp", offers.get(0).getConnectorOfferUrl());
	}

	@Test
	void getEDCAssetsByTypeSkipsConsumerConnector() {
		ReflectionTestUtils.setField(edcAssetLookUp, "consumerHost", "https://consumer.example");

		List<Criterion> filter = List.of(Criterion.builder().operandLeft("x").operator("=").operandRight("y").build());
		ConnectorInfo connectorInfo = ConnectorInfo.builder()
				.bpn("BPNL000000000001")
				.connectorEndpoint(List.of("https://consumer.example/api/v1/dsp"))
				.build();

		List<QueryDataOfferModel> offers = edcAssetLookUp.getEDCAssetsByType(List.of(connectorInfo), filter);

		assertEquals(0, offers.size());
		verify(catalogResponseBuilder, org.mockito.Mockito.never())
				.queryOnDataOffers(anyString(), anyString(), anyInt(), anyInt(), anyString());
	}
}
