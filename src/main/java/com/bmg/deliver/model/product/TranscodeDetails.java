package com.bmg.deliver.model.product;

import com.bmg.deliver.model.interfaces.StoreAsset;
import lombok.Data;

@Data
public class TranscodeDetails implements StoreAsset {
	private long storeId;
	private String transcodeId;
	private long uploadId;
	private String itemType;
	private String audioCodec;
	private String contentType;
	private String colorType;
	private String dimension;
	private long fileSize;
	private Long channels;
	private Long duration;
	private Long sampleRate;
	private String fileName;
	private String checksum;
	private String transCodedDate;
	private String nasFilePath;
	private String externalDownloadUrl;
	private String externalUrlCreatedDate;
	private Long externalUrlValidity;
	private String base64Checksum;
	private String baseFolder;
	private String assetsFolder;
	private String newFilename;
	private String ddexPath;
}
