package org.eclipse.tractusx.sde.edc.services;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.net.URI;
import java.util.List;

import org.eclipse.tractusx.sde.common.configuration.properties.SDEConfigurationProperties;
import org.eclipse.tractusx.sde.digitaltwins.facilitator.DigitalTwinsUtility;
import org.eclipse.tractusx.sde.digitaltwins.gateways.external.EDCDigitalTwinProxyForLookUp;
import org.eclipse.tractusx.sde.edc.model.edr.EDRCachedByIdResponse;
import org.eclipse.tractusx.sde.edc.model.response.QueryDataOfferModel;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LookUpDTTwinTest {

	@Mock
	private EDCDigitalTwinProxyForLookUp eDCDigitalTwinProxyForLookUp;

	@Mock
	private DigitalTwinsUtility digitalTwinsUtility;

	@Mock
	private CatalogResponseBuilder catalogResponseBuilder;

	@Mock
	private SDEConfigurationProperties sdeConfigurationProperties;

	@InjectMocks
	private LookUpDTTwin lookUpDTTwin;

	@Test
	void lookUpTwinContinuesWhenOneShellDescriptorFails() {
		EDRCachedByIdResponse edrToken = EDRCachedByIdResponse.builder()
				.endpoint("https://provider.example/registry")
				.authorization("token")
				.build();
		QueryDataOfferModel dtOffer = QueryDataOfferModel.builder().connectorOfferUrl("https://provider.example/api/v1/dsp").build();

		when(sdeConfigurationProperties.getManufacturerId()).thenReturn("BPNL000000000001");
		when(digitalTwinsUtility.encodeAssetIdsObject(any())).thenReturn(List.of("asset-id"));
		when(eDCDigitalTwinProxyForLookUp.shellLookup(eq(URI.create("https://provider.example/registry")), anyList(), anyMap()))
				.thenReturn("{\"result\":[\"shell-1\",\"shell-2\"]}");
		when(digitalTwinsUtility.encodeValueAsBase64Utf8("shell-1")).thenReturn("shell-1-encoded");
		when(digitalTwinsUtility.encodeValueAsBase64Utf8("shell-2")).thenReturn("shell-2-encoded");
		when(eDCDigitalTwinProxyForLookUp.getShellDescriptorByShellId(eq(URI.create("https://provider.example/registry")),
				eq("shell-1-encoded"), anyMap())).thenThrow(new RuntimeException("temporary shell read failure"));
		when(eDCDigitalTwinProxyForLookUp.getShellDescriptorByShellId(eq(URI.create("https://provider.example/registry")),
				eq("shell-2-encoded"), anyMap())).thenReturn("""
				{
				  "idShort": "shell",
				  "specificAssetIds": [
				    {"name":"manufacturerPartId","value":"PART-1"},
				    {"name":"manufacturerId","value":"BPNL000000000001"}
				  ],
				  "submodelDescriptors": [
				    {
				      "idShort": "PCFExchangeEndpoint",
				      "semanticId": {"keys":[{"value":"pcf"}]},
				      "description": [{"language":"en","text":"PCF submodel"}],
				      "endpoints": [
				        {"protocolInformation":{"href":"https://edc.data.plane/submodel","subprotocolBody":"id=asset-1;dspEndpoint=https://provider.example/api/v1/dsp"}}
				      ]
				    }
				  ]
				}
				""");

		List<QueryDataOfferModel> offers = lookUpDTTwin.lookUpTwin(edrToken, dtOffer, "PART-1", "BPNL000000000001", "pcf", 0, 10);

		assertEquals(1, offers.size());
		assertEquals("asset-1", offers.get(0).getAssetId());
		assertEquals("https://provider.example/api/v1/dsp@/submodel", offers.get(0).getConnectorOfferUrl());
	}
}
