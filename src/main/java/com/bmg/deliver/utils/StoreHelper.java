package com.bmg.deliver.utils;

import com.bmg.deliver.serviceimpl.worker.StoreService;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import feign.Response;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@NoArgsConstructor
public class StoreHelper {
	private StoreService storeService;
	private final Gson gson = new Gson();

	@Autowired
	public StoreHelper(StoreService storeService) {
		this.storeService = storeService;
	}

	// public void generateResponseWithTranscodes(Long assetId, String itemType,
	// ExecutionContext context) {
	// try (Response response = storeService.requestTranscodeGeneration(assetId,
	// itemType)) {
	// byte[] responseBody = response.body().asInputStream().readAllBytes();
	// String generatedDataWithTranscodes = new String(responseBody,
	// StandardCharsets.UTF_8);
	// context.getLogger().info("Response: %s", generatedDataWithTranscodes);
	// } catch (Exception e) {
	// log.error("Exception {}", ExceptionUtils.getStackTrace(e));
	// }
	// }

	public JsonObject fetchStoreAssetsData(List<String> assetIds) throws IOException {
		if (assetIds.isEmpty()) {
			return null;
		}
		Response response = storeService.getAssetsData(assetIds);
		if (response != null) {
			byte[] responseBody = response.body().asInputStream().readAllBytes();
			String responseDataAsString = new String(responseBody, StandardCharsets.UTF_8);
			return gson.fromJson(responseDataAsString, JsonObject.class);
		}
		return null;
	}
}
