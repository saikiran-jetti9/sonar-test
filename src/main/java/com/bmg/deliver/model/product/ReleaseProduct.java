package com.bmg.deliver.model.product;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReleaseProduct {
	private String messageId;
	private String messageDate;
	private Long approvalId;
	private String releaseId;
	private String captureProductId;
	private String barcode;
	private String productCode;
	private String artistName;
	private String title;
	private String versionTitle;
	private String internalVersionTitle;
	private boolean isMultipleArtists;
	private String releaseDate;
	private boolean releaseTimed;
	private String releaseTimeAndZone;
	private String releaseDateTime;
	private String approveStatus;
	private String approveDate;
	private String marketingRightsCodeName;
	private String contractualRightsCodeName;
	private String financeCategory;
	private RepertoireOwnerCompany repertoireOwnerCompany;
	private Label label;
	private ReleaseProductFormat format;
	private Genre genre;
	private String parentalAdvisoryArtworkIndicator;
	private String parentalAdvisoryLyricsIndicator;
	private List<Asset> productAssets = new ArrayList<>();
	private List<ReleaseComponent> components = new ArrayList<>();
	private List<Distributor> distributors = new ArrayList<>();
	private String consumerSynopsis;
	private ChangeManagement changeManagement;
	private String preOrderReady;
	private String sentForPreOrder;
	private String changeReason;
	private ProductSummary productSummary;
	private boolean standAlone;
	private String audioQuality;
	private DigitalAttributes digitalAttributes;
	private List<ReleaseTrack> tracks = new ArrayList<>();
	private String preOrderDate;
	private Boolean preOrderTimed;
	private String preOrderTimeAndZone;
	private boolean isDataOnlyTrigger;
	private String assetOptions;
	private String ddexPartyId;
	private String ddexPartyName;
	private String deliveryType;

	@Data
	public static class ReleaseProductFormat {
		private String id;
		private String name;
		private boolean packageIndicator;
		private boolean physicalIndicator;
		private String albumSingle;
	}

	@Data
	public static class RepertoireOwnerCompany {
		private String id;
		private String name;
	}

	@Data
	public static class Label {
		private String id;
		private String name;
	}

	@Data
	public static class DigitalAttributes {
		private List<Distributor.RetailerWrapper> retailers;
		private String previewClipsAvailability;
		private String previewClipsAvailabilityDate;
		private boolean permanentDownload;
		private String permanentDownloadDate;
		private boolean subscriptionStreaming;
		private String subscriptionStreamingDate;
		private boolean adSupportStreaming;
		private String adSupportedStreamingDate;
		private boolean appleDigitalMaster;
		private String masteringEngineerStudio;
		private String masteringStudioEmailAddress;
		private boolean containsAudioVisual;
		private boolean videoQcReady;
	}

	@Data
	public static class Genre {
		private String id;
		private String name;
	}

	@Data
	public static class ProductSummary {
		private String identifier;
		private String artistDisplayName;
		private String productTitle;
		private String label;
		private String genre;
		private String preOrderDate;
		private String releaseDate;
		private String productApprover;
		private String emailIdProductApprover;
		private String approvalDate;
		private String approvalId;
		private String issueType;
		private String priority;
		private String configuration;
		private String format;
		private String timedRelease;
		private String pricing;
		private String euroPpdBmgCode;
		private String gbpPpdBmgCode;
		private String usdPpdBmgCode;
		private String exclusivity;
		private String projectCode;
		private String projectName;
		private String summary;
		private String preOrderTimeAndZone;
		private String releaseTimeAndZone;
		private String audioQuality;
		private Confirmations confirmations;
	}

	@Data
	public static class Confirmations {
		private String confirmedDate;
		private String confirmedUser;
		private String emailIdConfirmedUser;
		private String changeReason;
		private List<Change> changes;
	}

	@Data
	public static class Change {
		private String changeType;
		private String entityType;
		private String changeTypeId;
		private String captureEntityId;
		private String changeSummary;
	}

	public List<ReleaseTrack> getTracks() {
		List<ReleaseTrack> trackList = new ArrayList<>();
		if (format.isPackageIndicator()) {
			for (ReleaseComponent component : components) {
				trackList.addAll(component.getTracks());
			}
			return trackList;
		}
		return tracks;
	}
}