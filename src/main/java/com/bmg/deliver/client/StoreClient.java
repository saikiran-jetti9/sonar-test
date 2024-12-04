package com.bmg.deliver.client;

import com.bmg.deliver.config.FeignClientConfig;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import feign.Response;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

@FeignClient(name = "store", url = "${feign.client.config.storeClient.url}", configuration = FeignClientConfig.class)
public interface StoreClient {

	@PostMapping("/api/assets/transcodes")
	Response getAssetsData(JsonNode assetIdsJson);

	@PostMapping("/api/assets/gcsUpload")
	Response requestDownloadLinksGeneration(ObjectNode assetIdsTranscodeIdsJson);

	@PostMapping("/api/assets/generate")
	Response generateTranscodes(ObjectNode assetIdsTranscodeIdsJson);
}
