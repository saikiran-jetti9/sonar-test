package com.bmg.deliver.serviceimpl.worker;

import com.bmg.deliver.client.StoreClient;
import com.bmg.deliver.utils.AppConstants;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import feign.Response;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

@Service
public class StoreService {
	ObjectMapper ob = new ObjectMapper();

	private final StoreClient storeClient;

	public StoreService(StoreClient storeClient) {
		this.storeClient = storeClient;
	}

	public Response getAssetsData(List<String> assetIds) {
		return storeClient.getAssetsData(buildPostBodyForAssetsData(assetIds));
	}

	public Response requestTranscodeGeneration(ObjectNode request) {
		return storeClient.generateTranscodes(request);
	}

	public Response requestDownloadLinksGeneration(Set<String> assetIds, Set<String> transcodeIds) {
		return storeClient
				.requestDownloadLinksGeneration(buildPostBodyForDownloadLinksGeneration(assetIds, transcodeIds));
	}

	public JsonNode buildPostBodyForAssetsData(List<String> assetIds) {
		ObjectMapper objectMapper = new ObjectMapper();
		ArrayNode assetIdsJson = objectMapper.createArrayNode();
		for (String assetId : assetIds) {
			ObjectNode assetNode = objectMapper.createObjectNode();
			assetNode.put(AppConstants.ID, assetId);
			assetIdsJson.add(assetNode);
		}
		return assetIdsJson;
	}

	public ObjectNode buildPostBodyForDownloadLinksGeneration(Set<String> assetIds, Set<String> transcodeIds) {
		ObjectNode postBody = ob.createObjectNode();
		ArrayNode assetIdsArray = ob.createArrayNode();
		for (String assetId : assetIds) {
			assetIdsArray.add(Long.parseLong(assetId));
		}
		ArrayNode transcodesArray = ob.createArrayNode();
		for (String transcodeId : transcodeIds) {
			transcodesArray.add(transcodeId.replace("\"", ""));
		}
		postBody.set(AppConstants.ASSETS, assetIdsArray);
		postBody.set(AppConstants.TRANSCODES, transcodesArray);
		return postBody;
	}
}
