package com.bmg.deliver.model;

import lombok.Data;

import java.util.HashSet;
import java.util.Set;

@Data
public class DownloadLinksRequest {
	private Set<String> allAssetIds = new HashSet<>();
	private Set<String> assetIds = new HashSet<>();
	private Set<String> transcodeIds = new HashSet<>();

	@Override
	public String toString() {
		if (!assetIds.isEmpty() && !transcodeIds.isEmpty()) {
			return "assetIds: " + assetIds + ", transcodeIds: " + transcodeIds;
		} else if (!assetIds.isEmpty()) {
			return "assetIds: " + assetIds;
		} else if (!transcodeIds.isEmpty()) {
			return "transcodeIds: " + transcodeIds;
		}
		return "allAssetIds: " + allAssetIds;
	}
}
