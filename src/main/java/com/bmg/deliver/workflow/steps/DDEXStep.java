package com.bmg.deliver.workflow.steps;

import com.bmg.deliver.enums.*;
import com.bmg.deliver.enums.ProductTranscodeOption;
import com.bmg.deliver.model.ddex.Deal;
import com.bmg.deliver.model.ddex.TrackDeal;
import com.bmg.deliver.model.interfaces.StoreAsset;
import com.bmg.deliver.model.product.*;
import com.bmg.deliver.utils.AppConstants;
import com.bmg.deliver.utils.ProductHelper;
import com.bmg.deliver.utils.StoreHelper;
import com.bmg.deliver.utils.TimeUtils;
import com.bmg.deliver.workflow.step.Step;
import com.bmg.deliver.workflow.step.StepField;
import com.bmg.deliver.workflow.step.StepParams;
import com.bmg.deliver.workflow.step.StepResult;
import com.google.gson.*;

import java.io.IOException;
import java.io.StringWriter;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

import lombok.Getter;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.apache.velocity.runtime.RuntimeConstants;
import org.apache.velocity.tools.ToolManager;
import org.apache.velocity.tools.generic.DateTool;
import org.apache.velocity.tools.generic.EscapeTool;

@Getter
public class DDEXStep extends Step {
	private final ProductHelper productHelper;
	private final StoreHelper storeHelper;

	@StepField(key = AppConstants.DDEX_RELEASE_TYPE)
	private ReleaseType releaseType;

	@StepField(key = AppConstants.PRODUCT_ASSET_OPTION)
	private ProductTranscodeOption productTranscodeOption;

	@StepField(key = AppConstants.TRACK_ASSET_OPTION)
	private AudioTranscodeOption audioTranscodeOption;

	@StepField(key = AppConstants.REPLACE_DOWNLOAD_LINK)
	private AssetOptions replaceWithDownloadLinks;

	@StepField(key = AppConstants.INCLUDE_ALBUM_STREAMING_DEALS)
	private Boolean includeAlbumStreamingDeals;

	@StepField(key = AppConstants.INCLUDE_KOSOVO)
	private Boolean includeKosovo;

	@StepField(key = AppConstants.USE_DATE_TIME)
	private Boolean useDateTime;

	private String templateCode;

	public DDEXStep(StepParams stepParams, ProductHelper productHelper, StoreHelper storeHelper, String templateCode) {
		super(stepParams.getId(), stepParams.getWorkflow(), stepParams.getExecutionOrder(), stepParams.getName(),
				stepParams.getType(), stepParams.getStepConfigurations());
		this.productHelper = productHelper;
		this.storeHelper = storeHelper;
		this.templateCode = templateCode;
	}

	@Override
	public StepResult run() throws IOException, InterruptedException {
		Gson gson = new GsonBuilder().disableHtmlEscaping().create();
		context.addArtifacts(gson.toJson(context.getWorkflowInstance().getTriggerData()), "PartnerProduct.json");
		Product product = (Product) context.getTriggerData();

		if (product.getReleaseProduct().getFormat().isPhysicalIndicator()) {
			return new StepResult(false, "Physical product handling not supported");
		}
		if (!hasValidPriceCode(product.getReleaseProduct())) {
			return new StepResult(false, "Price code is missing for isAvailableForSale true territories");
		}
		return prepareProductData(product);
	}

	public StepResult prepareProductData(Product product) throws IOException, InterruptedException {
		if (!context.getWorkflowInstance().getDeliveryType().equals(DeliveryType.DATA_ONLY)) {
			StepResult result = processNonDataOnlyDelivery(product);
			if (!result.isSuccess()) {
				return result;
			}
		}

		createDeals(product);
		addDdexFields(product);
		Gson gson = new GsonBuilder().disableHtmlEscaping().create();
		context.addArtifacts(gson.toJson(product), product.getReleaseProduct().getBarcode() + ".json");
		if (generateXml(product)) {
			context.getLogger().info("Generating Product XML...");
			return new StepResult(false, "Something went wrong while generating XML");
		}
		return new StepResult(true, "DDEX step completed successfully");
	}

	public boolean generateXml(Product product) {
		if (this.templateCode == null) {
			context.getLogger().info("Template is not selected for XML generation, terminating the process");
			return true;
		}

		String barcode = product.getReleaseProduct().getBarcode();
		String fileName = product.getReleaseProduct().getBarcode() + ".xml";

		VelocityEngine velocityEngine = new VelocityEngine();
		Properties props = new Properties();
		props.setProperty(RuntimeConstants.RESOURCE_LOADER, "string");
		props.setProperty("string.resource.loader.class",
				"org.apache.velocity.runtime.resource.loader.StringResourceLoader");
		velocityEngine.init(props);

		// Create the Velocity context and add data
		VelocityContext templateContext = new VelocityContext();
		templateContext.put("releaseProduct", product.getReleaseProduct());
		templateContext.put("captureProduct", product.getCaptureProduct());
		templateContext.put("productDeals", product.getProductDeals());
		templateContext.put("trackDeals", product.getTrackDeals());

		ToolManager toolManager = new ToolManager(true, true);
		toolManager.configure("velocity-tools.xml");
		templateContext.put("date", new DateTool());
		templateContext.put("esc", new EscapeTool());
		templateContext.put("tools", toolManager.getToolboxFactory().createToolbox("application"));
		templateContext.put("artistRoles", AppConstants.getDdexArtistRoles());
		templateContext.put("ddexRecordingTypes", AppConstants.getDdexRecordingTypes());
		templateContext.put("directContributorRoles", AppConstants.getDdexDirectContributorRoles());
		templateContext.put("indirectContributorRoles", AppConstants.getDdexIndirectContributorRoles());

		// Merge the template and data into a string
		StringWriter writer = new StringWriter();
		try {
			velocityEngine.evaluate(templateContext, writer, "XMLTemplate", templateCode);
			context.getLogger().info("Product XML is generated successfully...");
		} catch (Exception e) {
			logger.error("Exception while generating xml %s", e);
			return false;
		}
		String remotePath = productHelper.baseFolderName(releaseType, barcode);
		getContext().addArtifacts(writer, fileName, remotePath + "/" + fileName);
		return false;
	}

	/**
	 * Process non data only delivery 1.Fetch transcode details from store
	 * 2.Populate assets and transcodes details from store response 3.Verify if
	 * required assets have valid download links
	 */
	private StepResult processNonDataOnlyDelivery(Product product) throws IOException, InterruptedException {
		JsonObject storeAssetsData = productHelper.fetchTranscodeDetails(context, product.getReleaseProduct(),
				productTranscodeOption, audioTranscodeOption);
		if (null != storeAssetsData) {
			JsonArray storeAssets = storeAssetsData.has(AppConstants.ASSETS)
					? storeAssetsData.get(AppConstants.ASSETS).getAsJsonArray()
					: null;
			boolean hasValidTranscode = productHelper.populateAssetsAndTranscodes(product.getReleaseProduct(),
					storeAssets, productTranscodeOption, audioTranscodeOption, releaseType);
			if (!hasValidTranscode) {
				return new StepResult(false, "Transcodes are missing");
			}

			boolean hasValidDownloadLinks = productHelper.verifyDownloadLinks(context, product.getReleaseProduct(),
					replaceWithDownloadLinks);
			List<StoreAsset> preparedAssets = productHelper.getPreparedAssets(product.getReleaseProduct());
			product.getPreparedAssets().addAll(preparedAssets);
			addAssetsToUpload(product);

			if (!hasValidDownloadLinks) {
				return new StepResult(false, "Download links are missing");
			}
		}
		return new StepResult(true, "Assets processed successfully");
	}

	public void addAssetsToUpload(Product product) {
		List<StoreAsset> preparedAssets = product.getPreparedAssets();

		for (StoreAsset storeAsset : preparedAssets) {
			if (storeAsset instanceof Asset asset) {
				String remotePath = asset.getAssetsFolder() + asset.getNewFilename();
				context.getFilesToUpload().put(remotePath, asset.getNasFilePath());
			} else if (storeAsset instanceof TranscodeDetails transcodeDetails) {
				String remotePath = transcodeDetails.getAssetsFolder() + transcodeDetails.getNewFilename();
				context.getFilesToUpload().put(remotePath, transcodeDetails.getNasFilePath());
			}
		}
	}

	/**
	 * Check if all territories have valid price code when isAvailableForSale is
	 * true
	 *
	 * @param releaseProduct
	 *            Release product
	 * @return boolean indicating if all territories have valid price code
	 */
	public boolean hasValidPriceCode(ReleaseProduct releaseProduct) {
		if (releaseProduct.getDigitalAttributes().isPermanentDownload()) {
			for (Distributor distributor : releaseProduct.getDistributors()) {
				for (DistributionTerritory distributionTerritory : distributor.getDistributorTerritories()) {
					if (null != distributionTerritory.getIsAvailableForSale()
							&& distributionTerritory.getIsAvailableForSale()
							&& (distributionTerritory.getPartnerPriceCode() == null
									|| distributionTerritory.getPartnerPriceCode().isEmpty())) {
						return false;
					}
				}
			}
		}
		return true;
	}

	private void addDdexFields(Product product) {
		ReleaseProduct releaseProduct = product.getReleaseProduct();
		CaptureProduct captureProduct = product.getCaptureProduct();

		if (null != captureProduct.getVersionTitle() && !captureProduct.getVersionTitle().isEmpty()) {
			String[] referenceTitles = generateReferenceTitleAndSubTitle(releaseProduct.getTitle(),
					releaseProduct.getVersionTitle());
			captureProduct.setReferenceTitle(referenceTitles[0]);
			captureProduct.setReferenceSubTitle(referenceTitles[1]);
			captureProduct.setFormalSubTitle(captureProduct.getVersionTitle().replace(";", " - "));
		} else {
			captureProduct.setReferenceTitle(releaseProduct.getTitle());
		}
		captureProduct.setParentalAdvisoryLyricsIndicator(
				getParentalWarningType(captureProduct.getParentalAdvisoryLyricsIndicator()));

		int index = 0;
		for (CaptureProduct.ProductContributor contributor : captureProduct.getProductContributors()) {
			for (Contributor.ArtistRole role : contributor.getRoles()) {
				if (role.isDisplay()) {
					String artistRoleType = switch (contributor.getContributor().getGroupType().toUpperCase()) {
						case AppConstants.SOLO -> AppConstants.ARTIST_NAME;
						case AppConstants.VARIOUS_ARTISTS -> AppConstants.MULTIPLE_ARTIST;
						default -> AppConstants.BAND_NAME;
					};
					Set<String> isni = contributor.getContributor().getExternalCodes().stream()
							.filter(code -> AppConstants.ISNI.equalsIgnoreCase(code.getExternalCodeType()))
							.map(Contributor.ExternalCode::getExternalCodeText).collect(Collectors.toSet());
					CaptureProduct.DdexContributor artist = new CaptureProduct.DdexContributor();
					artist.setFullName(contributor.getContributor().getFullName());
					artist.setPartyId(contributor.getContributor().getContributorId());
					artist.setExternalCodes(isni);
					artist.setArtistRoleType(artistRoleType);
					artist.getRoles().add(role.getName());
					artist.setSeqNo(index++);
					captureProduct.getDisplayArtists().add(artist);
				}
			}
		}

		for (CaptureTrack track : captureProduct.getTracks()) {
			Recording recording = track.getRecording();
			populateRecordingDetails(recording, releaseProduct, captureProduct);
		}
	}

	private ReleaseTrack getReleaseTrackByRecordingId(ReleaseProduct releaseProduct, Long recordingId) {
		for (ReleaseTrack track : releaseProduct.getTracks()) {
			if (track.getCaptureRecordingId() == recordingId) {
				return track;
			}
		}
		return null;
	}

	public void populateRecordingDetails(Recording recording, ReleaseProduct releaseProduct,
			CaptureProduct captureProduct) {
		ReleaseTrack releaseTrack = getReleaseTrackByRecordingId(releaseProduct, recording.getRecordingId());
		setRecordingTypeAndResourceType(recording);
		generateFormattedDuration(recording, releaseTrack);
		long totalSeconds = captureProduct.getDuration() / 1000;
		captureProduct.setDurationFormatted(String.format("PT%dM%dS", totalSeconds / 60, totalSeconds % 60));

		if (null != recording.getVersionTitle() && !recording.getVersionTitle().isEmpty()) {
			String[] referenceTitles = generateReferenceTitleAndSubTitle(recording.getTitle(),
					recording.getVersionTitle());
			recording.setReferenceTitle(referenceTitles[0]);
			recording.setReferenceSubTitle(referenceTitles[1]);
			recording.setFormalSubTitle(recording.getVersionTitle().replace(";", " - "));
		} else {
			recording.setReferenceTitle(recording.getTitle());
		}

		if (!AppConstants.VOCAL.equalsIgnoreCase(recording.getCategory())) {
			recording.setInstrumental(true);
		}
		if (null != recording.getExplicitIndicator()) {
			recording.setExplicitIndicator(getParentalWarningType(recording.getExplicitIndicator()));
		}
		processContributors(recording, captureProduct);
	}

	public String getParentalWarningType(String explicitIndicator) {
		return switch (explicitIndicator.toUpperCase()) {
			case AppConstants.UNKNOWN -> AppConstants.UNKNOWN_TITLE_CASE;
			case AppConstants.CLEAN -> AppConstants.EXPLICIT_CONTENT_EDITED;
			case AppConstants.EXPLICIT_UPPER_CASE -> AppConstants.EXPLICIT;
			default -> AppConstants.NOT_EXPLICIT;
		};
	}

	private void processContributors(Recording recording, CaptureProduct captureProduct) {
		List<CaptureProduct.DdexContributor> ddexContributors = new ArrayList<>();
		int directArtistSeq = 1;
		int resourceContributorSeq = 1;

		for (Recording.RecordingContributor recordingContributor : recording.getRecordingContributors()) {
			Contributor contributor = recordingContributor.getContributor();
			if (recordingContributor.isExcludeFromSupplyChain()) {
				continue;
			}

			Set<String> isni = contributor.getExternalCodes().stream()
					.filter(code -> AppConstants.ISNI.equalsIgnoreCase(code.getExternalCodeType()))
					.map(Contributor.ExternalCode::getExternalCodeText).collect(Collectors.toSet());

			String artistRoleType = switch (contributor.getGroupType().toUpperCase()) {
				case AppConstants.SOLO -> AppConstants.ARTIST_NAME;
				case AppConstants.VARIOUS_ARTISTS -> AppConstants.MULTIPLE_ARTIST;
				default -> AppConstants.BAND_NAME;
			};

			CaptureProduct.DdexContributor artist = new CaptureProduct.DdexContributor();
			artist.setFullName(contributor.getFullName());
			if (null != recordingContributor.getOverrideFullName()
					&& !recordingContributor.getOverrideFullName().isEmpty()) {
				artist.setFullName(recordingContributor.getOverrideFullName());
			}
			artist.setFirstName(contributor.getFirstName());
			artist.setLastName(contributor.getLastName());
			artist.setPartyId(contributor.getContributorId());
			artist.setExternalCodes(isni);
			artist.setArtistRoleType(artistRoleType);

			boolean hasDisplayRole = false;
			boolean hasNonDisplayRole = false;
			boolean hasVocalsRole = false;
			boolean hasNonDisplayVocals = false;

			for (Contributor.ArtistRole role : recordingContributor.getRoles()) {
				if (role.isDisplay()) {
					hasDisplayRole = true;
				} else {
					hasNonDisplayRole = true;
				}
				if (AppConstants.VOCAL.equalsIgnoreCase(role.getName())
						|| AppConstants.VOCALS.equalsIgnoreCase(role.getName())) {
					hasVocalsRole = true;
				} else if (!role.isDisplay()) {
					hasNonDisplayVocals = true;
				}
			}
			if ((hasVocalsRole && (!hasDisplayRole || hasNonDisplayVocals)) || (!hasVocalsRole && hasNonDisplayRole)) {
				for (Contributor.ArtistRole role : recordingContributor.getRoles()) {
					if (AppConstants.VOCAL.equalsIgnoreCase(role.getName())
							|| AppConstants.VOCALS.equalsIgnoreCase(role.getName())) {
						if (!hasDisplayRole) {
							artist.getRoles().add(role.getName());
						}
					} else if (!role.isDisplay()) {
						artist.getRoles().add(role.getName());
					}
				}
				artist.setSeqNo(resourceContributorSeq++);
				recording.getResourceContributors().add(artist);
			}

			for (Contributor.ArtistRole role : recordingContributor.getRoles()) {
				if (!role.isDisplay()) {
					continue;
				}
				CaptureProduct.DdexContributor displayArtist = new CaptureProduct.DdexContributor();
				displayArtist.setFullName(contributor.getFullName());
				displayArtist.setPartyId(contributor.getContributorId());
				displayArtist.setExternalCodes(isni);
				displayArtist.setArtistRoleType(artistRoleType);
				displayArtist.getRoles().add(role.getName());
				displayArtist.setSeqNo(directArtistSeq++);
				ddexContributors.add(displayArtist);
			}
		}
		processClassicalGenreContributors(captureProduct, recording, ddexContributors, directArtistSeq);
		recording.setDisplayArtists(ddexContributors);
	}

	private void processClassicalGenreContributors(CaptureProduct captureProduct, Recording recording,
			List<CaptureProduct.DdexContributor> ddexContributors, int directArtistSeq) {
		if (captureProduct.getGenre().isClassical()) {
			for (RecordingSong recordingSong : recording.getSongs()) {
				for (RecordingSong.SongContributor songContributor : recordingSong.getSongContributors()) {
					for (Contributor.ArtistRole role : songContributor.getRoles()) {
						if (!AppConstants.COMPOSER.equalsIgnoreCase(role.getName())) {
							continue;
						}
						CaptureProduct.DdexContributor artist = new CaptureProduct.DdexContributor();
						artist.setFullName(songContributor.getContributor().getFullName());
						artist.setPartyId(songContributor.getContributor().getContributorId());
						artist.getRoles().add(AppConstants.COMPOSER);
						artist.setSeqNo(directArtistSeq++);
						artist.setExternalCodes(new HashSet<>());
						ddexContributors.add(artist);
					}
				}
			}
		}
	}

	public void generateFormattedDuration(Recording recording, ReleaseTrack releaseTrack) {
		long totalSeconds = 0;
		if (null != releaseTrack && null != releaseTrack.getSelectedAsset()) {
			if (null != releaseTrack.getSelectedAsset().getFileType()
					&& null != releaseTrack.getSelectedAsset().getFileType().getDuration()) {
				totalSeconds = releaseTrack.getSelectedAsset().getFileType().getDuration() / 1000;
			} else if (null != releaseTrack.getSelectedAsset().getDuration()) {
				totalSeconds = releaseTrack.getSelectedAsset().getDuration() / 1000;
			}
		} else {
			totalSeconds = recording.getDuration();
		}
		recording.setTrackDuration(String.format("PT%dM%dS", totalSeconds / 60, totalSeconds % 60));
	}

	public void generateProductDuration(CaptureProduct captureProduct) {
		long totalSeconds = captureProduct.getDuration() / 1000;
		captureProduct.setDurationFormatted(String.format("PT%dM%dS", totalSeconds / 60, totalSeconds % 60));
	}

	private void setRecordingTypeAndResourceType(Recording recording) {
		if (recording.getRecordingType().toLowerCase().contains(AppConstants.AUDIO_VISUAL.toLowerCase())
				|| recording.getRecordingType().toLowerCase().contains(AppConstants.VIDEO_CLIP.toLowerCase())) {
			recording.setDdexType(AppConstants.VIDEO);
			recording.setDdexResourceType(AppConstants.SHORT_FORM_MUSICAL_WORK_VIDEO);
		} else {
			recording.setDdexType(AppConstants.SOUND_RECORDING);
			recording.setDdexResourceType(AppConstants.MUSICAL_WORK_SOUND_RECORDING);
		}
	}

	/**
	 * Generate ReferenceTitle, FormalTitle
	 */
	public String[] generateReferenceTitleAndSubTitle(String title, String versionTitle) {
		StringBuilder finalTitle = new StringBuilder(title);
		StringBuilder finalVersionTitle = new StringBuilder();
		if (null != versionTitle && !versionTitle.isEmpty()) {
			String[] versionTitles = versionTitle.split(";");

			for (int i = 0; i < versionTitles.length; i++) {
				if (i == versionTitles.length - 1) {
					if (versionTitles[i].startsWith(AppConstants.FEAT.toLowerCase())
							|| versionTitles[i].startsWith(AppConstants.WITH.toLowerCase())) {
						finalTitle.append(" (").append(versionTitles[i]).append(")");
					} else {
						finalVersionTitle.append(versionTitles[i]);
					}
				} else {
					finalTitle.append(" (").append(versionTitles[i]).append(")");
				}
			}
		}

		String[] titles = new String[2];
		titles[0] = finalTitle.toString();
		titles[1] = finalVersionTitle.toString();
		return titles;
	}

	public void createDeals(Product product) {
		ReleaseProduct releaseProduct = product.getReleaseProduct();
		List<ReleaseTrack> releaseTracks = getReleaseTracks(releaseProduct);
		Map<String, List<ReleaseTrack>> instantGratOverrideTracksByDate = getInstantGratOverrideTracksByDate(
				releaseTracks);

		List<Deal> productDeals = new ArrayList<>();
		Map<String, List<Deal>> trackDealMap = new HashMap<>();
		createAllDeals(releaseProduct, productDeals, trackDealMap, instantGratOverrideTracksByDate, releaseTracks);

		List<Deal> processedDeals = new ArrayList<>();
		productDeals.forEach(deal -> {
			if (null == useDateTime || !useDateTime) {
				deal.setReleaseDateTime(null);
				deal.setPreOrderDateTime(null);
				deal.setShortsStartDateTime(null);
				deal.setInstantGratPeriodStartDateTime(null);
				deal.setInstantGratPeriodEndDateTime(null);
				deal.setInstantGratOverrideDateTime(null);
				deal.setTakeDownDateTime(null);
			}
			processedDeals.add(deal);
		});

		trackDealMap.forEach((key, deals) -> {
			if (null == useDateTime || !useDateTime) {
				deals.forEach(deal -> {
					deal.setReleaseDateTime(null);
					deal.setPreOrderDateTime(null);
					deal.setShortsStartDateTime(null);
					deal.setInstantGratPeriodStartDateTime(null);
					deal.setInstantGratPeriodEndDateTime(null);
					deal.setInstantGratOverrideDateTime(null);
					deal.setTakeDownDateTime(null);
				});
			}
		});
		List<TrackDeal> trackDealGroups = mergeTrackDeals(trackDealMap);
		product.setProductDeals(processedDeals);
		product.setTrackDeals(trackDealGroups);
	}

	/**
	 * The first track's deals will be added directly to the deals list, while the
	 * rest will be merged with existing deals if possible or added as a new group
	 *
	 * @param trackDealMap
	 *            Map of track deals
	 * @return List of track deal groups
	 */
	private List<TrackDeal> mergeTrackDeals(Map<String, List<Deal>> trackDealMap) {
		List<TrackDeal> deals = new ArrayList<>();

		for (Map.Entry<String, List<Deal>> entry : trackDealMap.entrySet()) {
			List<Deal> dealsForResource = entry.getValue();
			Set<String> trackResourceSet = new HashSet<>(Collections.singleton(entry.getKey()));
			TrackDeal deal = new TrackDeal(dealsForResource, trackResourceSet);

			boolean isMerged = false;
			for (TrackDeal groupDeal : deals) {
				if (groupDeal.merge(deal)) {
					isMerged = true;
					break;
				}
			}

			if (!isMerged) {
				deals.add(deal);
			}
		}
		return deals;
	}

	private void createAllDeals(ReleaseProduct releaseProduct, List<Deal> productDeals,
			Map<String, List<Deal>> trackDealMap, Map<String, List<ReleaseTrack>> instantGratOverrideTracksByDate,
			List<ReleaseTrack> releaseTracks) {
		for (Distributor distributor : releaseProduct.getDistributors()) {
			List<String> exclusives = new ArrayList<>();
			List<String> exclusions = new ArrayList<>();
			if (null != distributor.getRetailers()) {
				distributor.getRetailers().getExclusives()
						.forEach(retailerWrapper -> exclusives.add(retailerWrapper.getRetailer().getRetailerName()));
				distributor.getRetailers().getExclusions()
						.forEach(retailerWrapper -> exclusions.add(retailerWrapper.getRetailer().getRetailerName()));
			}
			String distributorName = distributor.getDistributorName();

			for (DistributionTerritory territory : distributor.getDistributorTerritories()) {
				if (!AppConstants.XK.equals(territory.getIsoCode()) || Boolean.TRUE.equals(includeKosovo)) {
					String preOrderDate = (territory.getPreOrderDate() != null
							&& !territory.getPreOrderDate().isEmpty()) ? territory.getPreOrderDate() : "";

					Deal deal = Deal.createDefaultProductDeal(territory, distributorName, exclusives, exclusions);
					createDealsForTerritory(releaseProduct, deal, productDeals, instantGratOverrideTracksByDate,
							preOrderDate, true);
					createTrackDeals(releaseProduct, territory, deal, trackDealMap, releaseTracks,
							instantGratOverrideTracksByDate, preOrderDate);
				}
			}
		}
	}

	private void createTrackDeals(ReleaseProduct product, DistributionTerritory territory, Deal deal,
			Map<String, List<Deal>> trackDealsMap, List<ReleaseTrack> releaseTracks,
			Map<String, List<ReleaseTrack>> instantGratOverrideTracksByDate, String preOrderDate) {
		for (ReleaseTrack track : releaseTracks) {
			if (track.isIndividualAvailable()) {
				for (Distributor distributor : track.getDistributors()) {
					for (DistributionTerritory trackTerritory : distributor.getDistributorTerritories()) {
						if (trackTerritory.getIsoCode().equals(territory.getIsoCode())) {
							Deal countryDealGroup = Deal.createDefaultTrackDeal(deal, track, trackTerritory);
							List<Deal> deals = trackDealsMap.computeIfAbsent(track.getRRef(), k -> new ArrayList<>());
							createDealsForTerritory(product, countryDealGroup, deals, instantGratOverrideTracksByDate,
									preOrderDate, false);
							break;
						}
					}
				}
			}
		}
	}

	private void createDealsForTerritory(ReleaseProduct releaseProduct, Deal deal, List<Deal> dealsList,
			Map<String, List<ReleaseTrack>> instantGratOverrideTracksByDate, String preOrderDate,
			boolean isProductDeal) {
		List<Deal> countryDealGroups = new ArrayList<>();
		String groupName;

		boolean streamDealMerged = false;
		boolean downloadDealMerged = false;
		for (Deal ddexDeal : dealsList) {
			if (ddexDeal.merge(deal)) {
				if (ddexDeal.isDownload()) {
					downloadDealMerged = true;
				} else {
					streamDealMerged = true;
				}
			}
		}
		ReleaseProduct.DigitalAttributes digitalAttributes = releaseProduct.getDigitalAttributes();
		if (!downloadDealMerged) {
			countryDealGroups.addAll(createDownloadDeals(releaseProduct, digitalAttributes, deal,
					instantGratOverrideTracksByDate, preOrderDate, isProductDeal));
		}

		if (!streamDealMerged
				&& (digitalAttributes.isAdSupportStreaming() || digitalAttributes.isSubscriptionStreaming())) {
			groupName = isProductDeal ? AppConstants.STREAM : AppConstants.TRACK_STREAM;
			countryDealGroups.add(Deal.createStreamDeal(deal, groupName, digitalAttributes.isSubscriptionStreaming(),
					digitalAttributes.isAdSupportStreaming()));
		}
		if (!countryDealGroups.isEmpty()) {
			dealsList.addAll(countryDealGroups);
		}
	}

	private List<Deal> createDownloadDeals(ReleaseProduct releaseProduct,
			ReleaseProduct.DigitalAttributes digitalAttributes, Deal deal,
			Map<String, List<ReleaseTrack>> instantGratOverrideTracksByDate, String preOrderDate,
			boolean isProductDeal) {
		List<Deal> downloadDeals = new ArrayList<>();
		if (digitalAttributes.isPermanentDownload() && null != includeAlbumStreamingDeals
				&& includeAlbumStreamingDeals) {
			String groupName = isProductDeal ? AppConstants.DOWNLOAD : AppConstants.TRACK_DOWNLOAD;
			downloadDeals.add(Deal.createDownloadDeal(deal, groupName));

			boolean hasPreOrder = null != releaseProduct.getPreOrderDate()
					&& !releaseProduct.getPreOrderDate().isEmpty();
			if (isProductDeal && !deal.isTakeDownDeal() && hasPreOrder) {
				downloadDeals.addAll(createPreOrderAndInstantGratDeals(instantGratOverrideTracksByDate, deal,
						preOrderDate, releaseProduct.getReleaseDate()));
			}
		}
		return downloadDeals;
	}

	private List<Deal> createPreOrderAndInstantGratDeals(
			Map<String, List<ReleaseTrack>> instantGratOverrideTracksByDate, Deal deal, String preOrderDate,
			String releaseDate) {
		List<Deal> countryDealGroups = new ArrayList<>();
		List<String> instantGratDates = new ArrayList<>(instantGratOverrideTracksByDate.keySet());
		String instantGratPeriodEndDate = releaseDate;
		if (!instantGratDates.isEmpty()) {
			instantGratPeriodEndDate = instantGratOverrideTracksByDate.get(instantGratDates.get(0)).get(0)
					.getInstantGratOverride();
		}

		if (null == instantGratPeriodEndDate || !instantGratPeriodEndDate.equals(preOrderDate)) {
			String previousDay = null != instantGratPeriodEndDate
					? TimeUtils.subtractDaysFromDate(instantGratPeriodEndDate, 1)
					: null;

			Deal preOrderDeal = Deal.createDownloadPreorderDeal(deal, AppConstants.DOWNLOAD_PREORDER, preOrderDate,
					previousDay);
			countryDealGroups.add(preOrderDeal);
		}

		int index = 0;
		for (String instantGratDate : instantGratDates) {
			Deal instantGratDeal = Deal.createDownloadInstantGratDeal(deal, AppConstants.DOWNLOAD_INSTANT_GRAT,
					instantGratDate);
			List<Long> captureTrackIds = instantGratDates.subList(0, index + 1).stream()
					.flatMap(date -> instantGratOverrideTracksByDate.get(date).stream())
					.map(ReleaseTrack::getCaptureTrackId).toList();

			List<String> trackRefs = instantGratDates.subList(0, index + 1).stream()
					.flatMap(date -> instantGratOverrideTracksByDate.get(date).stream()).map(ReleaseTrack::getARef)
					.toList();

			instantGratDeal.setCaptureTrackIdsForInstantGratTracks(captureTrackIds);
			instantGratDeal.setInstantGratTrackRefs(trackRefs);

			String igEndDate = index == instantGratDates.size() - 1
					? TimeUtils.subtractDaysFromDate(deal.getReleaseDate(), 1)
					: TimeUtils.subtractDaysFromDate(instantGratDates.get(index + 1), 1);
			instantGratDeal.setInstantGratPeriodEndDate(igEndDate);
			instantGratDeal.setInstantGratPeriodEndDateTime(deal.convertDateTime(igEndDate));
			countryDealGroups.add(instantGratDeal);
			index++;
		}
		return countryDealGroups;
	}

	/**
	 * Group instant grat override true tracks by date and then sort them by
	 * instantGratOverride date in ascending order Get instant grat override tracks
	 * by date in ascending order of instantGratOverride date
	 *
	 * @param releaseTracks
	 *            List of release tracks
	 * @return Map of instant grat override tracks by instantGratOverride date
	 */
	private Map<String, List<ReleaseTrack>> getInstantGratOverrideTracksByDate(List<ReleaseTrack> releaseTracks) {
		Map<String, List<ReleaseTrack>> instantGratOverrideMap = getInstantGratOverrideTracksMap(releaseTracks);
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern(AppConstants.DATE_FORMAT_YYYY_MM_DD);

		Map<String, List<ReleaseTrack>> sortedInstantGratOverrideMap = new TreeMap<>((date1, date2) -> {
			LocalDate localDate1 = LocalDate.parse(date1, formatter);
			LocalDate localDate2 = LocalDate.parse(date2, formatter);
			return localDate1.compareTo(localDate2);
		});

		sortedInstantGratOverrideMap.putAll(instantGratOverrideMap);
		return sortedInstantGratOverrideMap;
	}

	private Map<String, List<ReleaseTrack>> getInstantGratOverrideTracksMap(List<ReleaseTrack> tracks) {
		Map<String, List<ReleaseTrack>> instantGratOverrideMap = new HashMap<>();

		for (ReleaseTrack track : tracks) {
			if (track.isInstantGrat() && null != track.getInstantGratOverride()
					&& !track.getInstantGratOverride().isEmpty()) {
				String instantGratOverride = track.getInstantGratOverride();
				List<ReleaseTrack> instantGratTracks;
				if (instantGratOverrideMap.containsKey(instantGratOverride)) {
					instantGratTracks = instantGratOverrideMap.get(instantGratOverride);
				} else {
					instantGratTracks = new ArrayList<>();
				}
				instantGratTracks.add(track);
				instantGratOverrideMap.put(instantGratOverride, instantGratTracks);
			}
		}
		return instantGratOverrideMap;
	}

	private List<ReleaseTrack> getReleaseTracks(ReleaseProduct releaseProduct) {
		List<ReleaseTrack> releaseTracks = new ArrayList<>();
		int index = 0;
		if (releaseProduct.getFormat().isPackageIndicator()) {
			Map<Long, ReleaseComponent> componentMap = new TreeMap<>();

			for (ReleaseComponent component : releaseProduct.getComponents()) {
				long componentNumber = component.getComponentNumber();
				componentMap.put(componentNumber, component);
			}

			int size = 0;
			for (Map.Entry<Long, ReleaseComponent> entry : componentMap.entrySet()) {
				long componentNumber = entry.getKey();
				ReleaseComponent component = componentMap.get(componentNumber);
				for (ReleaseTrack track : component.getTracks()) {
					index = track.getTrackNumber() + size + 1;
					track.setARef("A" + index);
					track.setRRef("R" + index);
					releaseTracks.add(track);
				}
				size = component.getTracks().size();
			}
		} else {
			for (ReleaseTrack track : releaseProduct.getTracks()) {
				index = track.getTrackNumber() + 1;
				track.setARef("A" + index);
				track.setRRef("R" + index);
				releaseTracks.add(track);
			}
		}

		for (Asset asset : releaseProduct.getProductAssets()) {
			asset.setARef("A" + (++index));
			asset.setRRef("R" + index);
		}
		return releaseTracks;
	}
}
