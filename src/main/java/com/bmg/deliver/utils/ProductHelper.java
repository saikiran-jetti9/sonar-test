package com.bmg.deliver.utils;

import com.bmg.deliver.enums.AssetOptions;
import com.bmg.deliver.enums.AudioTranscodeOption;
import com.bmg.deliver.enums.ProductTranscodeOption;
import com.bmg.deliver.enums.ReleaseType;
import com.bmg.deliver.model.DownloadLinksRequest;
import com.bmg.deliver.model.interfaces.StoreAsset;
import com.bmg.deliver.model.product.*;
import com.bmg.deliver.serviceimpl.worker.StoreService;
import com.bmg.deliver.workflow.execution.ExecutionContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.gson.*;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import feign.Response;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.FilenameUtils;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Getter
public class ProductHelper {
	private final StoreService storeService;
	private final StoreHelper storeHelper;
	Gson gson = new GsonBuilder().disableHtmlEscaping().create();

	public ProductHelper(StoreService storeService, StoreHelper storeHelper) {
		this.storeService = storeService;
		this.storeHelper = storeHelper;
	}

	public String baseFolderName(ReleaseType releaseType, String barcode) {
		LocalDateTime now = LocalDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(AppConstants.DATE_FORMAT_COMPACT_TIMESTAMP);
		String timestamp = now.format(formatter);
		if (ReleaseType.RELEASE_BY_RELEASE.equals(releaseType)) {
			return barcode + "_" + timestamp;
		} else if (ReleaseType.BATCHED.equals(releaseType)) {
			return timestamp + "/" + barcode;
		} else {
			return barcode;
		}
	}

	public boolean populateAssetsAndTranscodes(ReleaseProduct releaseProduct, JsonArray storeAssets,
			ProductTranscodeOption productTranscodeOption, AudioTranscodeOption audioTranscodeOption,
			ReleaseType releaseType) {
		String barcode = releaseProduct.getBarcode();
		String baseFolderName = baseFolderName(releaseType, barcode);
		if (!processProductAssets(releaseProduct, storeAssets, productTranscodeOption, baseFolderName)) {
			return false;
		}
		if (releaseProduct.getFormat().isPackageIndicator()) {
			for (ReleaseComponent component : releaseProduct.getComponents()) {
				for (ReleaseTrack track : component.getTracks()) {
					if (processTrackAssets(releaseProduct, track, storeAssets, barcode, audioTranscodeOption,
							baseFolderName, component.getComponentNumber(), track.getTrackNumber() + 1)) {
						return false;
					}
				}
			}
		}
		for (ReleaseTrack track : releaseProduct.getTracks()) {
			if (processTrackAssets(releaseProduct, track, storeAssets, barcode, audioTranscodeOption, baseFolderName, 0,
					track.getTrackNumber() + 1)) {
				return false;
			}
		}
		return true;
	}

	private boolean processTrackAssets(ReleaseProduct releaseProduct, ReleaseTrack track, JsonArray storeAssets,
			String barcode, AudioTranscodeOption audioTranscodeOption, String baseFolderName, long componentNumber,
			int trackNumber) {
		if (null != track.getSelectedAsset()) {
			Asset selectedAsset = track.getSelectedAsset();
			boolean hasValidTranscode = AudioTranscodeOption.NONE.equals(audioTranscodeOption)
					|| AppConstants.AUDIO_VISUAL.equalsIgnoreCase(selectedAsset.getAssetType())
					|| (AudioTranscodeOption.STANDARD_DEFINITION.equals(audioTranscodeOption)
							&& AppConstants.WAV.equalsIgnoreCase(selectedAsset.getItemType())
							&& selectedAsset.getFileType().getSampleSize() == 16);
			for (JsonElement storeAssetElement : storeAssets) {
				JsonObject storeAsset = storeAssetElement.getAsJsonObject().get(AppConstants.ASSET).getAsJsonObject();
				String storeAssetId = storeAsset.get(AppConstants.ID).getAsString();

				if (storeAssetId.equals(selectedAsset.getStoreId())) {
					if (storeAsset.has(AppConstants.FILE_SIZE)) {
						selectedAsset.setFileSize(storeAsset.get(AppConstants.FILE_SIZE).getAsString());
					}
					if (storeAsset.has(AppConstants.ORIGINAL_FILE_NAME)) {
						selectedAsset
								.setOriginalFileName(storeAsset.get(AppConstants.ORIGINAL_FILE_NAME).getAsString());
					}
					if (storeAsset.has(AppConstants.CHECKSUM)) {
						selectedAsset.setChecksum(storeAsset.get(AppConstants.CHECKSUM).getAsString());
					}
					if (storeAsset.has(AppConstants.UPLOAD_FILE_NAME)) {
						selectedAsset.setUploadFileName(storeAsset.get(AppConstants.UPLOAD_FILE_NAME).getAsString());
					}
					if (storeAsset.has(AppConstants.NAS_FILE_PATH)) {
						selectedAsset.setNasFilePath(storeAsset.get(AppConstants.NAS_FILE_PATH).getAsString());
					}
					if (storeAsset.has(AppConstants.EXTERNAL_DOWNLOAD_URL)
							&& !storeAsset.get(AppConstants.EXTERNAL_DOWNLOAD_URL).isJsonNull()) {
						selectedAsset.setExternalDownloadUrl(
								storeAsset.get(AppConstants.EXTERNAL_DOWNLOAD_URL).getAsString());
					}
					if (storeAsset.has(AppConstants.EXTERNAL_URL_CREATED_DATE)
							&& !storeAsset.get(AppConstants.EXTERNAL_URL_CREATED_DATE).isJsonNull()) {
						selectedAsset.setExternalUrlCreatedDate(
								storeAsset.get(AppConstants.EXTERNAL_URL_CREATED_DATE).getAsString());
					}
					if (storeAsset.has(AppConstants.EXTERNAL_URL_VALIDITY)
							&& !storeAsset.get(AppConstants.EXTERNAL_URL_VALIDITY).isJsonNull()) {
						selectedAsset
								.setExternalUrlValidity(storeAsset.get(AppConstants.EXTERNAL_URL_VALIDITY).getAsLong());
					}

					if (storeAsset.has(AppConstants.CHECKSUM)) {
						String base64Checksum = Base64
								.encodeBase64String(storeAsset.get(AppConstants.CHECKSUM).getAsString().getBytes());
						selectedAsset.setBase64Checksum(base64Checksum);
					}
					selectedAsset.setBaseFolder(baseFolderName + "/");
					selectedAsset.setAssetsFolder(baseFolderName + "/" + AppConstants.RESOURCES + "/");
					selectedAsset
							.setNewFilename(generateNewFileName(storeAsset, barcode, componentNumber, trackNumber));
					if (baseFolderName.equals(releaseProduct.getBarcode())) {
						selectedAsset.setDdexPath(baseFolderName);
					} else {
						selectedAsset.setDdexPath(AppConstants.RESOURCES + "/" + selectedAsset.getNewFilename());
					}

					JsonObject transcode = getValidTranscode(storeAssetElement, audioTranscodeOption.getAssetOption());
					if (null != transcode) {
						TranscodeDetails transcodeDetails = createTranscodeDetails(releaseProduct, transcode,
								baseFolderName, barcode, componentNumber, trackNumber);
						selectedAsset.setTranscodeDetails(transcodeDetails);
						hasValidTranscode = true;
					}
				}
			}
			return !hasValidTranscode;
		}
		return false;
	}

	public boolean processProductAssets(ReleaseProduct releaseProduct, JsonArray storeAssets,
			ProductTranscodeOption option, String baseFolderName) {
		boolean hasValidTranscode = ProductTranscodeOption.NONE.equals(option);
		for (Asset productAsset : releaseProduct.getProductAssets()) {
			for (JsonElement storeAssetElement : storeAssets) {
				JsonObject storeAsset = storeAssetElement.getAsJsonObject().get(AppConstants.ASSET).getAsJsonObject();
				String storeAssetId = storeAsset.get(AppConstants.ID).getAsString();
				if (storeAssetId.equals(productAsset.getStoreId())) {
					if (storeAsset.has(AppConstants.FILE_SIZE)) {
						productAsset.setFileSize(storeAsset.get(AppConstants.FILE_SIZE).getAsString());
					}
					if (storeAsset.has(AppConstants.ORIGINAL_FILE_NAME)) {
						productAsset.setOriginalFileName(storeAsset.get(AppConstants.ORIGINAL_FILE_NAME).getAsString());
					}
					if (storeAsset.has(AppConstants.CHECKSUM)) {
						productAsset.setChecksum(storeAsset.get(AppConstants.CHECKSUM).getAsString());
					}
					if (storeAsset.has(AppConstants.UPLOAD_FILE_NAME)) {
						productAsset.setUploadFileName(storeAsset.get(AppConstants.UPLOAD_FILE_NAME).getAsString());
					}
					if (storeAsset.has(AppConstants.NAS_FILE_PATH)) {
						productAsset.setNasFilePath(storeAsset.get(AppConstants.NAS_FILE_PATH).getAsString());
					}
					if (storeAsset.has(AppConstants.CHECKSUM)) {
						String base64Checksum = Base64
								.encodeBase64String(storeAsset.get(AppConstants.CHECKSUM).getAsString().getBytes());
						productAsset.setBase64Checksum(base64Checksum);
					}
					if (storeAsset.has(AppConstants.EXTERNAL_DOWNLOAD_URL)
							&& !storeAsset.get(AppConstants.EXTERNAL_DOWNLOAD_URL).isJsonNull()) {
						productAsset.setExternalDownloadUrl(
								storeAsset.get(AppConstants.EXTERNAL_DOWNLOAD_URL).getAsString());
					}
					if (storeAsset.has(AppConstants.EXTERNAL_URL_CREATED_DATE)
							&& !storeAsset.get(AppConstants.EXTERNAL_URL_CREATED_DATE).isJsonNull()) {
						productAsset.setExternalUrlCreatedDate(
								storeAsset.get(AppConstants.EXTERNAL_URL_CREATED_DATE).getAsString());
					}
					if (storeAsset.has(AppConstants.EXTERNAL_URL_VALIDITY)
							&& !storeAsset.get(AppConstants.EXTERNAL_URL_VALIDITY).isJsonNull()) {
						productAsset
								.setExternalUrlValidity(storeAsset.get(AppConstants.EXTERNAL_URL_VALIDITY).getAsLong());
					}
					if (storeAsset.has(AppConstants.CHECKSUM)) {
						String base64Checksum = Base64
								.encodeBase64String(storeAsset.get(AppConstants.CHECKSUM).getAsString().getBytes());
						productAsset.setBase64Checksum(base64Checksum);
					}
					productAsset.setBaseFolder(baseFolderName + "/");
					productAsset.setAssetsFolder(baseFolderName + "/" + AppConstants.RESOURCES + "/");
					productAsset.setNewFilename(generateNewFileName(storeAsset, releaseProduct.getBarcode(), -1, -1));
					if (baseFolderName.equals(releaseProduct.getBarcode())) {
						productAsset.setDdexPath(baseFolderName);
					} else {
						productAsset.setDdexPath(AppConstants.RESOURCES + "/" + productAsset.getNewFilename());
					}
					JsonObject transcode = getValidTranscode(storeAssetElement, option.getAssetOption());
					if (null != transcode) {
						TranscodeDetails transcodeDetails = createTranscodeDetails(releaseProduct, transcode,
								baseFolderName, releaseProduct.getBarcode(), -1, -1);
						productAsset.setTranscodeDetails(transcodeDetails);
						hasValidTranscode = true;
					}
				}
			}
		}
		return hasValidTranscode;
	}

	private TranscodeDetails createTranscodeDetails(ReleaseProduct releaseProduct, JsonObject transcode,
			String baseFolderName, String barcode, long componentNumber, int sequenceNumber) {
		TranscodeDetails transcodeDetails = new TranscodeDetails();
		if (transcode.has(AppConstants.TRANSCODE_ID)) {
			transcodeDetails.setTranscodeId(transcode.get(AppConstants.TRANSCODE_ID).getAsString());
		}
		if (transcode.has(AppConstants.STORE_ID)) {
			transcodeDetails.setStoreId(transcode.get(AppConstants.STORE_ID).getAsLong());
		}
		if (transcode.has(AppConstants.UPLOAD_ID)) {
			transcodeDetails.setUploadId(transcode.get(AppConstants.UPLOAD_ID).getAsLong());
		}
		if (transcode.has(AppConstants.ITEM_TYPE)) {
			transcodeDetails.setItemType(transcode.get(AppConstants.ITEM_TYPE).getAsString());
		}
		if (transcode.has(AppConstants.AUDIO_CODEC)) {
			transcodeDetails.setAudioCodec(transcode.get(AppConstants.AUDIO_CODEC).getAsString());
		}
		if (transcode.has(AppConstants.CHANNELS)) {
			transcodeDetails.setChannels(transcode.get(AppConstants.CHANNELS).getAsLong());
		}
		if (transcode.has(AppConstants.DURATION)) {
			transcodeDetails.setDuration(transcode.get(AppConstants.DURATION).getAsLong());
		}
		if (transcode.has(AppConstants.SAMPLE_RATE)) {
			transcodeDetails.setSampleRate(transcode.get(AppConstants.SAMPLE_RATE).getAsLong());
		}
		if (transcode.has(AppConstants.FILE_SIZE.toLowerCase())) {
			transcodeDetails.setFileSize(transcode.get(AppConstants.FILE_SIZE.toLowerCase()).getAsLong());
		}
		if (transcode.has(AppConstants.CONTENT_TYPE)) {
			transcodeDetails.setContentType(transcode.get(AppConstants.CONTENT_TYPE).getAsString());
		}
		if (transcode.has(AppConstants.COLOR_TYPE)) {
			transcodeDetails.setContentType(transcode.get(AppConstants.COLOR_TYPE).getAsString());
		}
		if (transcode.has(AppConstants.DIMENSION)) {
			transcodeDetails.setDimension(transcode.get(AppConstants.DIMENSION).getAsString());
		}
		if (transcode.has(AppConstants.FILE_SIZE)) {
			transcodeDetails.setFileSize(transcode.get(AppConstants.FILE_SIZE).getAsLong());
		}
		if (transcode.has(AppConstants.FILE_NAME)) {
			transcodeDetails.setFileName(transcode.get(AppConstants.FILE_NAME).getAsString());
		}
		if (transcode.has(AppConstants.CHECKSUM)) {
			transcodeDetails.setChecksum(transcode.get(AppConstants.CHECKSUM).getAsString());
		}
		if (transcode.has(AppConstants.TRANSCODED_DATE)) {
			transcodeDetails.setTransCodedDate(transcode.get(AppConstants.TRANSCODED_DATE).getAsString());
		}
		if (transcode.has(AppConstants.NAS_FILE_PATH)) {
			transcodeDetails.setNasFilePath(transcode.get(AppConstants.NAS_FILE_PATH).getAsString());
		}
		if (transcode.has(AppConstants.EXTERNAL_DOWNLOAD_URL)
				&& !transcode.get(AppConstants.EXTERNAL_DOWNLOAD_URL).isJsonNull()) {
			transcodeDetails.setExternalDownloadUrl(transcode.get(AppConstants.EXTERNAL_DOWNLOAD_URL).getAsString());
		}
		if (transcode.has(AppConstants.EXTERNAL_URL_CREATED_DATE)
				&& !transcode.get(AppConstants.EXTERNAL_URL_CREATED_DATE).isJsonNull()) {
			transcodeDetails
					.setExternalUrlCreatedDate(transcode.get(AppConstants.EXTERNAL_URL_CREATED_DATE).getAsString());
		}
		if (transcode.has(AppConstants.EXTERNAL_URL_VALIDITY)
				&& !transcode.get(AppConstants.EXTERNAL_URL_VALIDITY).isJsonNull()) {
			transcodeDetails.setExternalUrlValidity(transcode.get(AppConstants.EXTERNAL_URL_VALIDITY).getAsLong());
		}
		if (transcode.has(AppConstants.CHECKSUM)) {
			String base64Checksum = Base64
					.encodeBase64String(transcode.get(AppConstants.CHECKSUM).getAsString().getBytes());
			transcodeDetails.setBase64Checksum(base64Checksum);
		}
		transcodeDetails.setBaseFolder(baseFolderName + "/");
		transcodeDetails.setAssetsFolder(baseFolderName + "/" + AppConstants.RESOURCES + "/");
		transcodeDetails.setNewFilename(generateNewFileName(transcode, barcode, componentNumber, sequenceNumber));
		if (baseFolderName.equals(releaseProduct.getBarcode())) {
			transcodeDetails.setDdexPath(baseFolderName);
		} else {
			transcodeDetails.setDdexPath(AppConstants.RESOURCES + "/" + transcodeDetails.getNewFilename());
		}

		return transcodeDetails;
	}

	private JsonObject getValidTranscode(JsonElement storeAssetElement, String itemType) {
		JsonObject asset = storeAssetElement.getAsJsonObject().get(AppConstants.ASSET).getAsJsonObject();

		if (AppConstants.NONE.equalsIgnoreCase(itemType)) {
			return null;
		}

		if (AudioTranscodeOption.STANDARD_DEFINITION.getAssetOption().equals(itemType)
				&& asset.has(AppConstants.ITEM_TYPE)
				&& AppConstants.WAV.equalsIgnoreCase(asset.get(AppConstants.ITEM_TYPE).getAsString())
				&& asset.has(AppConstants.SAMPLE_SIZE) && asset.get(AppConstants.SAMPLE_SIZE).getAsLong() == 16L) {
			return null;
		}
		if (storeAssetElement.getAsJsonObject().has(AppConstants.TRANSCODE_DETAILS)) {
			JsonArray transcodes = storeAssetElement.getAsJsonObject().get(AppConstants.TRANSCODE_DETAILS)
					.getAsJsonArray();
			for (JsonElement transcodeElement : transcodes) {
				JsonObject transcode = transcodeElement.getAsJsonObject();
				String transcodeItemType = transcode.has(AppConstants.ITEM_TYPE)
						? transcode.getAsJsonObject().get(AppConstants.ITEM_TYPE).getAsString()
						: "";
				if (!transcodeItemType.isEmpty() && transcodeItemType.toLowerCase().contains(itemType.toLowerCase())) {
					return transcode;
				}
			}
		}
		return null;
	}

	private String generateNewFileName(JsonObject storeAsset, String barcode, long componentNumber, int trackNumber) {
		String fileExtension = FilenameUtils.getExtension(storeAsset.get(AppConstants.NAS_FILE_PATH).getAsString());
		String fileName;
		if (componentNumber != -1 && trackNumber != -1) {
			fileName = String.format("%s_%s_%s.%s", barcode, formatComponentAndTrackNumber(componentNumber + 1),
					formatComponentAndTrackNumber(trackNumber), fileExtension);
		} else {
			fileName = String.format("%s.%s", barcode, fileExtension);
		}
		return fileName;
	}

	private String formatComponentAndTrackNumber(long number) {
		int numberOfDigits = String.valueOf(number).length();
		int totalDigits = numberOfDigits + 1;

		String numberStr = String.valueOf(number);
		int numberOfZeros = totalDigits - numberStr.length();

		return "0".repeat(Math.max(0, numberOfZeros)) + numberStr;
	}

	public List<String> getAssetIdsForTracks(ReleaseProduct product) {
		List<String> assetIds = new ArrayList<>();
		for (ReleaseTrack track : product.getTracks()) {
			if (null != track.getSelectedAsset()) {
				assetIds.add(track.getSelectedAsset().getStoreId());
			}
		}
		return assetIds;
	}

	public JsonObject fetchTranscodeDetails(ExecutionContext context, ReleaseProduct releaseProduct,
			ProductTranscodeOption productTranscodeOption, AudioTranscodeOption audioTranscodeOption)
			throws IOException, InterruptedException {
		List<String> assetIds = getAssetIdsForTracks(releaseProduct);
		for (Asset productAsset : releaseProduct.getProductAssets()) {
			assetIds.add(productAsset.getStoreId());
		}

		if (ProductTranscodeOption.NONE.equals(productTranscodeOption)
				&& AudioTranscodeOption.NONE.equals(audioTranscodeOption)) {
			return storeHelper.fetchStoreAssetsData(assetIds);
		}

		context.getLogger().info("Checking for transcode details");
		if (null != productTranscodeOption) {
			context.getLogger().info("Product Transcode Option: %s", productTranscodeOption);
		}
		if (null != audioTranscodeOption) {
			context.getLogger().info("Audio Transcode Option: %s", audioTranscodeOption);
		}
		int retry = 0;
		JsonObject storeAssets;
		boolean generated = false;
		do {
			storeAssets = storeHelper.fetchStoreAssetsData(assetIds);
			if (null != storeAssets && !storeAssets.isEmpty()) {
				JsonArray assets = storeAssets.has(AppConstants.ASSETS)
						? storeAssets.get(AppConstants.ASSETS).getAsJsonArray()
						: null;
				if (null != assets) {
					generated = verifyTranscodes(context, assets, productTranscodeOption, audioTranscodeOption, retry);
					if (!generated) {
						context.getLogger()
								.info("Pausing for 30 seconds before checking again if transcodes are present...");
						Thread.sleep(30000);
					}
				}
			}
			retry++;
		} while (retry < 15 && !generated);
		return storeAssets;
	}

	private boolean verifyTranscodes(ExecutionContext context, JsonArray storeAssets,
			ProductTranscodeOption productTranscodeOption, AudioTranscodeOption audioTranscodeOption, int retry) {
		List<String> audioItemTypes = List.of("Mp3", "Mp4", "Flac", "Wav", "M4a", "Ogg", "Webm");
		List<String> imageItemTypes = List.of("Gif", "Jpeg", "Bmp", "Tiff", "Psd");
		ObjectMapper mapper = new ObjectMapper();
		ObjectNode request = mapper.createObjectNode();
		ArrayNode transcodeAssets = mapper.createArrayNode();

		for (JsonElement assetElement : storeAssets) {
			JsonObject asset = assetElement.getAsJsonObject().get(AppConstants.ASSET).getAsJsonObject();
			JsonArray transcodeDetails = assetElement.getAsJsonObject().get(AppConstants.TRANSCODE_DETAILS)
					.getAsJsonArray();
			String assetItemType = asset.has(AppConstants.ITEM_TYPE)
					? asset.get(AppConstants.ITEM_TYPE).getAsString()
					: "";
			long sampleSize = asset.has(AppConstants.SAMPLE_SIZE)
					? asset.get(AppConstants.SAMPLE_SIZE).getAsLong()
					: 0L;
			boolean hasValidTranscode = false;
			if (!assetItemType.isEmpty()) {
				if (!ProductTranscodeOption.NONE.equals(productTranscodeOption)
						&& imageItemTypes.contains(assetItemType)
						&& !productTranscodeOption.getAssetOption().contains(assetItemType.toLowerCase())) {
					for (JsonElement transcodeElement : transcodeDetails) {
						String transcodeItemType = transcodeElement.getAsJsonObject().has(AppConstants.ITEM_TYPE)
								? transcodeElement.getAsJsonObject().get(AppConstants.ITEM_TYPE).getAsString()
										.toLowerCase()
								: "";
						if (transcodeItemType.contains(productTranscodeOption.getAssetOption().toLowerCase())) {
							hasValidTranscode = true;
							break;
						}
					}
					if (!hasValidTranscode) {
						ObjectNode obj = mapper.createObjectNode();
						obj.put("assetId", asset.get(AppConstants.ID).getAsLong());
						obj.put("extension", productTranscodeOption.getAssetOption());
						transcodeAssets.add(obj);
					}
				}

				if (!AudioTranscodeOption.NONE.equals(audioTranscodeOption) && audioItemTypes.contains(assetItemType)
						&& !(AudioTranscodeOption.STANDARD_DEFINITION.equals(audioTranscodeOption)
								&& AppConstants.WAV.equalsIgnoreCase(assetItemType) && sampleSize == 16)) {
					hasValidTranscode = false;
					for (JsonElement transcodeElement : transcodeDetails) {
						String transcodeItemType = transcodeElement.getAsJsonObject().has(AppConstants.ITEM_TYPE)
								? transcodeElement.getAsJsonObject().get(AppConstants.ITEM_TYPE).getAsString()
										.toLowerCase()
								: "";
						if (transcodeItemType.contains(audioTranscodeOption.getAssetOption().toLowerCase())) {
							hasValidTranscode = true;
							break;
						}
					}
					if (!hasValidTranscode) {
						ObjectNode obj = mapper.createObjectNode();
						obj.put("assetId", asset.get(AppConstants.ID).getAsLong());
						transcodeAssets.add(obj);
					}
				}
			}
		}
		if (!transcodeAssets.isEmpty()) {
			List<Long> storeIds = new ArrayList<>();
			for (JsonNode node : transcodeAssets) {
				storeIds.add(node.get(AppConstants.ASSET_ID).asLong());
			}
			context.getLogger().info("Transcodes are missing for the following asset ids : %s", storeIds);
			request.set("transcodeAssets", transcodeAssets);
			if (retry == 0) {
				context.getLogger().info("Requesting transcode generation for assets: %s", transcodeAssets);
				Response response = storeService.requestTranscodeGeneration(request);
				context.getLogger().info("Requested transcode generation to Store, and the response is: %s %s",
						response.status(), response.protocolVersion());
			}
			return false;
		} else {
			context.getLogger().info("All transcodes are available for the assets!");
		}
		return true;
	}

	public boolean verifyDownloadLinks(ExecutionContext context, ReleaseProduct releaseProduct,
			AssetOptions assetOptions) throws InterruptedException, IOException {
		context.getLogger().info("Verifying external download links...");
		DownloadLinksRequest request = new DownloadLinksRequest();
		int maxRetries = 15;
		int retryCount = 0;

		do {
			request.getAllAssetIds().clear();
			request.getAssetIds().clear();
			request.getTranscodeIds().clear();
			checkDownloadLinks(releaseProduct, assetOptions, request);

			if ((!request.getAssetIds().isEmpty() || !request.getTranscodeIds().isEmpty()) && retryCount == 0) {
				context.getLogger().info("Download links are missing for the request: %s", request);
				context.getLogger().info(
						"Request Store to generate download links and check for their availability up to 15 times, with a 30-second interval between each attempt!");
				Response response = storeService.requestDownloadLinksGeneration(request.getAssetIds(),
						request.getTranscodeIds());
				context.getLogger().info("Requested Store to generate download links, and the response is: %s",
						response.status());
			}

			if (!request.getAssetIds().isEmpty() || !request.getTranscodeIds().isEmpty()) {
				if (retryCount != 0) {
					context.getLogger().info("Download links are still unavailable for: %s", request);
				}
				Thread.sleep(30000);
				JsonObject storeAssets = storeHelper.fetchStoreAssetsData(request.getAllAssetIds().stream().toList());
				if (storeAssets != null) {
					validateDownloadLinks(releaseProduct, storeAssets);
				}
			}
			if (request.getTranscodeIds().isEmpty() && request.getAssetIds().isEmpty()) {
				context.getLogger().info("Download links for the required assets are available!");
			}
			retryCount++;
		} while ((!request.getAssetIds().isEmpty() || !request.getTranscodeIds().isEmpty()) && retryCount < maxRetries);

		return request.getAssetIds().isEmpty() && request.getTranscodeIds().isEmpty();
	}

	private void validateDownloadLinks(ReleaseProduct releaseProduct, JsonObject storeAssets) {
		JsonArray assets = storeAssets.get(AppConstants.ASSETS).getAsJsonArray();

		for (JsonElement assetElement : assets) {
			JsonObject storeAsset = assetElement.getAsJsonObject();
			for (Asset productAsset : releaseProduct.getProductAssets()) {
				updateDownloadLinks(storeAsset, productAsset);
			}

			for (ReleaseTrack track : releaseProduct.getTracks()) {
				if (null != track.getSelectedAsset()) {
					updateDownloadLinks(storeAsset, track.getSelectedAsset());
				}
			}
		}
	}

	private void updateDownloadLinks(JsonObject storeAsset, Asset releaseAsset) {
		JsonObject asset = storeAsset.get(AppConstants.ASSET).getAsJsonObject();
		if (asset.has(AppConstants.ID) && null != releaseAsset
				&& releaseAsset.getStoreId().equals(asset.get(AppConstants.ID).getAsString())) {
			if (null != releaseAsset.getTranscodeDetails()) {

				TranscodeDetails transcodeDetails = releaseAsset.getTranscodeDetails();
				for (JsonElement transcodeEle : storeAsset.get(AppConstants.TRANSCODE_DETAILS).getAsJsonArray()) {
					JsonObject transcode = transcodeEle.getAsJsonObject();
					if (transcodeDetails.getTranscodeId()
							.equals(transcode.get(AppConstants.TRANSCODE_ID).getAsString())) {
						addDownloadLinkToTranscode(transcodeDetails, transcode);
					}
				}
			} else {
				addDownloadLinkToAsset(releaseAsset, asset);
			}
		}
	}

	private void addDownloadLinkToTranscode(TranscodeDetails transcodeDetails, JsonObject storeAsset) {
		if (storeAsset.has(AppConstants.EXTERNAL_DOWNLOAD_URL)) {
			transcodeDetails.setExternalDownloadUrl(storeAsset.get(AppConstants.EXTERNAL_DOWNLOAD_URL).getAsString());
		}
		if (storeAsset.has(AppConstants.EXTERNAL_URL_CREATED_DATE)) {
			transcodeDetails
					.setExternalUrlCreatedDate(storeAsset.get(AppConstants.EXTERNAL_URL_CREATED_DATE).getAsString());
		}
		if (storeAsset.has(AppConstants.EXTERNAL_URL_VALIDITY)) {
			transcodeDetails.setExternalUrlValidity(storeAsset.get(AppConstants.EXTERNAL_URL_VALIDITY).getAsLong());
		}
	}

	private void addDownloadLinkToAsset(Asset asset, JsonObject storeAsset) {
		if (storeAsset.has(AppConstants.EXTERNAL_DOWNLOAD_URL)
				&& !storeAsset.get(AppConstants.EXTERNAL_DOWNLOAD_URL).isJsonNull()) {
			asset.setExternalDownloadUrl(storeAsset.get(AppConstants.EXTERNAL_DOWNLOAD_URL).getAsString());
		}
		if (storeAsset.has(AppConstants.EXTERNAL_URL_CREATED_DATE)
				&& !storeAsset.get(AppConstants.EXTERNAL_URL_CREATED_DATE).isJsonNull()) {
			asset.setExternalUrlCreatedDate(storeAsset.get(AppConstants.EXTERNAL_URL_CREATED_DATE).getAsString());
		}
		if (storeAsset.has(AppConstants.EXTERNAL_URL_VALIDITY)
				&& !storeAsset.get(AppConstants.EXTERNAL_URL_VALIDITY).isJsonNull()) {
			asset.setExternalUrlValidity(storeAsset.get(AppConstants.EXTERNAL_URL_VALIDITY).getAsLong());
		}
	}

	private void checkDownloadLinks(ReleaseProduct releaseProduct, AssetOptions assetOptions,
			DownloadLinksRequest request) {
		for (Asset productAsset : releaseProduct.getProductAssets()) {
			if (AssetOptions.ALL_ASSETS.equals(assetOptions) || AssetOptions.PRODUCT_ASSETS.equals(assetOptions)) {
				if (null != productAsset.getTranscodeDetails()) {
					TranscodeDetails transcode = productAsset.getTranscodeDetails();

					// If Validity or Created date is null then generate or if the link is invalid
					// then generate
					if (null == transcode.getExternalUrlValidity() || null == transcode.getExternalUrlCreatedDate()
							|| downloadLinkNotValid(transcode.getExternalUrlCreatedDate(),
									transcode.getExternalUrlValidity())) {
						transcode.setExternalDownloadUrl(null);
						transcode.setExternalUrlValidity(null);
						transcode.setExternalUrlCreatedDate(null);
						request.getTranscodeIds().add(transcode.getTranscodeId());
					}
				} else if (null == productAsset.getExternalUrlValidity()
						|| null == productAsset.getExternalUrlCreatedDate() || downloadLinkNotValid(
								productAsset.getExternalUrlCreatedDate(), productAsset.getExternalUrlValidity())) {
					productAsset.setExternalDownloadUrl(null);
					productAsset.setExternalUrlValidity(null);
					productAsset.setExternalUrlCreatedDate(null);
					request.getAssetIds().add(productAsset.getStoreId());
				}
			} else {
				productAsset.setExternalDownloadUrl(null);
				productAsset.setExternalUrlValidity(null);
				productAsset.setExternalUrlCreatedDate(null);

				if (null != productAsset.getTranscodeDetails()) {
					productAsset.getTranscodeDetails().setExternalDownloadUrl(null);
					productAsset.getTranscodeDetails().setExternalUrlValidity(null);
					productAsset.getTranscodeDetails().setExternalUrlCreatedDate(null);
				}
			}
			request.getAllAssetIds().add(productAsset.getStoreId());
		}
		checkDownloadLinksForTracks(releaseProduct.getTracks(), assetOptions, request);
	}

	private void checkDownloadLinksForTracks(List<ReleaseTrack> tracks, AssetOptions assetOptions,
			DownloadLinksRequest request) {
		for (ReleaseTrack track : tracks) {
			if (null != track.getSelectedAsset()) {
				Asset selectedAsset = track.getSelectedAsset();
				request.getAllAssetIds().add(selectedAsset.getStoreId());
				if (useDownloadLink(assetOptions, selectedAsset)) {
					if (null != selectedAsset.getTranscodeDetails()) {
						TranscodeDetails transcode = selectedAsset.getTranscodeDetails();
						if (null == transcode.getExternalUrlCreatedDate()
								|| null == transcode.getExternalUrlValidity() | downloadLinkNotValid(
										transcode.getExternalUrlCreatedDate(), transcode.getExternalUrlValidity())) {
							transcode.setExternalDownloadUrl(null);
							transcode.setExternalUrlValidity(null);
							transcode.setExternalUrlCreatedDate(null);
							request.getTranscodeIds().add(transcode.getTranscodeId());
						}
					} else if (null == selectedAsset.getExternalUrlValidity()
							|| null == selectedAsset.getExternalUrlCreatedDate()
							|| downloadLinkNotValid(selectedAsset.getExternalUrlCreatedDate(),
									selectedAsset.getExternalUrlValidity())) {
						selectedAsset.setExternalDownloadUrl(null);
						selectedAsset.setExternalUrlValidity(null);
						selectedAsset.setExternalUrlCreatedDate(null);
						request.getAssetIds().add(selectedAsset.getStoreId());
					}
				} else {
					selectedAsset.setExternalDownloadUrl(null);
					selectedAsset.setExternalUrlValidity(null);
					selectedAsset.setExternalUrlCreatedDate(null);

					if (null != selectedAsset.getTranscodeDetails()) {
						selectedAsset.getTranscodeDetails().setExternalDownloadUrl(null);
						selectedAsset.getTranscodeDetails().setExternalUrlValidity(null);
						selectedAsset.getTranscodeDetails().setExternalUrlCreatedDate(null);
					}
				}
			}
		}
	}

	private static boolean useDownloadLink(AssetOptions assetOptions, Asset selectedAsset) {
		String assetType = selectedAsset.getAssetType();
		// Download links are needed if Asset Option is All Assets, Track Assets, Audio
		// Assets or Video Assets
		return AssetOptions.ALL_ASSETS.equals(assetOptions) || AssetOptions.TRACK_ASSETS.equals(assetOptions)
				|| (AssetOptions.AUDIO_ASSETS.equals(assetOptions) && AppConstants.AUDIO.equalsIgnoreCase(assetType))
				|| (AssetOptions.VIDEO_ASSETS.equals(assetOptions)
						&& AppConstants.AUDIO_VISUAL.equalsIgnoreCase(assetType));
	}

	public boolean downloadLinkNotValid(String createdDate, Long validity) {
		ZonedDateTime dateTime = parseDate(createdDate);
		if (null != dateTime) {
			ZonedDateTime expirationDate = dateTime.plus(Duration.ofSeconds(validity));
			return !ZonedDateTime.now(ZoneOffset.UTC).isBefore(expirationDate);
		}
		return true;
	}

	private ZonedDateTime parseDate(String createdDate) {
		final List<DateTimeFormatter> formatters = Arrays.asList(
				DateTimeFormatter.ofPattern(AppConstants.DATE_FORMAT_ISO_LOCAL_DATE_TIME_WITH_MILLIS),
				DateTimeFormatter.ofPattern(AppConstants.DATE_FORMAT_ISO_LOCAL_DATE_TIME),
				DateTimeFormatter.ofPattern(AppConstants.DATE_FORMAT_COMPACT_DATE_TIME_WITH_ZONE));
		for (DateTimeFormatter formatter : formatters) {
			try {
				return ZonedDateTime.parse(createdDate, formatter.withZone(ZoneOffset.UTC));
			} catch (DateTimeParseException e) {
				// Ignore and try the next formatter
			}
		}
		return null;
	}

	public List<StoreAsset> getPreparedAssets(ReleaseProduct product) {
		List<StoreAsset> preparedAssets = new ArrayList<>();
		for (Asset productAsset : product.getProductAssets()) {
			if (null != productAsset.getTranscodeDetails()) {
				preparedAssets.add(productAsset.getTranscodeDetails());
			} else {
				preparedAssets.add(productAsset);
			}
		}

		for (ReleaseTrack track : product.getTracks()) {
			if (null != track.getSelectedAsset()) {
				Asset asset = track.getSelectedAsset();
				if (null != asset.getTranscodeDetails()) {
					preparedAssets.add(asset.getTranscodeDetails());
				} else {
					preparedAssets.add(asset);
				}
			}
		}
		return preparedAssets;
	}
}