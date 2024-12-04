package com.bmg.deliver.model.product;

import com.bmg.deliver.model.interfaces.StoreAsset;
import lombok.Data;

@Data
public class Asset implements StoreAsset {
	private String storeId;
	private String assetType;
	private String itemType;
	private boolean supplyChain;
	private FileType fileType;
	private String ddexType;
	private String ddexResourceType;
	private String fileSize;
	private String originalFileName;
	private String checksum;
	private String uploadFileName;
	private String nasFilePath;
	private String externalDownloadUrl;
	private String externalUrlCreatedDate;
	private Long externalUrlValidity;
	private String base64Checksum;
	private String baseFolder;
	private String assetsFolder;
	private String ddexPath;
	private String newFilename;
	private Long duration;
	private String aRef;
	private String rRef;
	private TranscodeDetails transcodeDetails;

	@Data
	public static class FileType {
		private Long bitRate;
		private String audioCodec;
		private Long duration;
		private Long sampleRate;
		private Long sampleSize;
		private Long channels;
		private String videoCodec;
		private Long height;
		private Long width;
		private String dimension;
		private Long frameRate;
	}
}
