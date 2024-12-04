package com.bmg.deliver.model.product;

import lombok.Data;

@Data
public class ProductAsset {
	private String storeId;
	private String assetType;
	private String itemType;
	private boolean supplyChain;
	private String assetSubType;
	private String ddexType;
	private String ddexResourceType;
	private String fileSize;
	private String originalFileName;
	private String checksum;
	private String uploadFileName;
	private String nasFilePath;
	private String base64Checksum;
	private String baseFolder;
	private String assetsFolder;
	private String newFilename;
	private String ddexPath;
	private String assetCategory;
	private TranscodeDetails transcodeDetails;
}
