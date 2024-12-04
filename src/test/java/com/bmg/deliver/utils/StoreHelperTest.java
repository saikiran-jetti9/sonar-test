package com.bmg.deliver.utils;

import com.bmg.deliver.model.WorkflowInstance;
import com.bmg.deliver.serviceimpl.worker.StoreService;
import com.bmg.deliver.workflow.execution.ExecutionContext;

import java.io.*;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

import com.google.gson.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class StoreHelperTest {
	@Spy
	private StoreHelper storeHelper;
	@Mock
	private StoreService storeService;
	@Mock
	private ExecutionContext context;
	Gson gson = new Gson();

	@BeforeEach
	public void setUp() throws IOException {
		MockitoAnnotations.openMocks(this);
		ReflectionTestUtils.setField(storeHelper, "storeService", storeService);
		WorkflowInstance workflowInstance = new WorkflowInstance();
		workflowInstance.setId(1L);
	}

	@Test
	void testFetchStoreAssetsData_emptyAssetIds() throws IOException {
		List<String> assetIds = new ArrayList<>();
		JsonObject storeResponse = storeHelper.fetchStoreAssetsData(assetIds);
		assertNull(storeResponse);
	}

	private JsonObject getStoreResponse() throws FileNotFoundException {
		FileReader fileReader = new FileReader("src/test/resources/storehelper/storeDownloadLinks.json");
		JsonObject storeResponse = gson.fromJson(fileReader, JsonObject.class);

		if (storeResponse.has(AppConstants.ASSETS)) {
			for (JsonElement assetElement : storeResponse.get(AppConstants.ASSETS).getAsJsonArray()) {
				JsonObject asset = assetElement.getAsJsonObject();
				JsonObject assetDetails = asset.has(AppConstants.ASSET)
						? asset.get(AppConstants.ASSET).getAsJsonObject()
						: null;
				JsonArray transcodeDetails = asset.has(AppConstants.TRANSCODE_DETAILS)
						? asset.get(AppConstants.TRANSCODE_DETAILS).getAsJsonArray()
						: null;
				if (assetDetails != null && assetDetails.has(AppConstants.EXTERNAL_URL_CREATED_DATE)) {
					assetDetails.remove(AppConstants.EXTERNAL_URL_CREATED_DATE);
					ZonedDateTime utcNow = ZonedDateTime.now(ZoneOffset.UTC);
					DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
					String formattedDateTime = utcNow.format(formatter);
					assetDetails.add(AppConstants.EXTERNAL_URL_CREATED_DATE, new JsonPrimitive(formattedDateTime));
				}

				if (null != transcodeDetails && !transcodeDetails.isEmpty()) {
					for (JsonElement transcodeElement : transcodeDetails) {
						JsonObject transcode = transcodeElement.getAsJsonObject();
						if (transcode.has(AppConstants.EXTERNAL_URL_CREATED_DATE)) {
							transcode.remove(AppConstants.EXTERNAL_URL_CREATED_DATE);
							ZonedDateTime utcNow = ZonedDateTime.now(ZoneOffset.UTC);
							DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyyMMdd'T'HHmmss'Z'");
							String formattedDateTime = utcNow.format(formatter);
							transcode.add(AppConstants.EXTERNAL_URL_CREATED_DATE, new JsonPrimitive(formattedDateTime));
						}
					}
				}
			}
		}
		return storeResponse;
	}
}
