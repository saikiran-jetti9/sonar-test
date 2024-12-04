package com.bmg.deliver.serviceimpl.worker;

import com.bmg.deliver.client.StoreClient;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import feign.Request;
import feign.Response;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.*;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreServiceTest {

	@Mock
	private StoreClient storeClient;

	@InjectMocks
	private StoreService storeService;

	private ObjectMapper objectMapper;

	@BeforeEach
	public void setUp() {
		objectMapper = new ObjectMapper();
	}

	@Test
	void testGetAssetsData_withList() {
		List<String> assetIds = List.of("1", "2", "3");
		JsonNode expectedBody = storeService.buildPostBodyForAssetsData(assetIds);
		Response mockResponse = Response.builder().status(200)
				.request(Request.create(Request.HttpMethod.GET, "/assets", Map.of(), null, null, null)).build();

		when(storeClient.getAssetsData(any(JsonNode.class))).thenReturn(mockResponse);

		Response response = storeService.getAssetsData(assetIds);

		verify(storeClient, times(1)).getAssetsData(expectedBody);
		assertEquals(200, response.status());
	}

	@Test
	void testBuildPostBodyForAssetsData() {
		List<String> assetIds = List.of("1", "2", "3");
		JsonNode assetIdsJson = storeService.buildPostBodyForAssetsData(assetIds);

		assertEquals(3, assetIdsJson.size());
	}

	@Test
	void testRequestDownloadLinksGeneration_Success() {
		Set<String> assetIds = new HashSet<>();
		assetIds.add("1");
		assetIds.add("2");
		assetIds.add("3");
		Set<String> transcodeIds = new HashSet<>();
		transcodeIds.add("transcode1");
		transcodeIds.add("transcode2");

		storeService.requestDownloadLinksGeneration(assetIds, transcodeIds);

		verify(storeClient).requestDownloadLinksGeneration(any());
	}
}
