package com.bmg.deliver.model.product;

import lombok.Data;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Data
public class CaptureProduct {
	private long productId;
	private Site site;
	private String royaltyCompliance;
	private String entityStatus;
	private String lockedStatus;
	private boolean contractSupplyChainApproved;
	private boolean isMetadataConfirmed;
	private boolean isCancelled;
	private boolean isCatalogueIngestion;
	private boolean wasLiveWhenAcquired;
	private String barcode;
	private String productCode;
	private String grId;
	private List<String> altProductCodes = new ArrayList<>();
	private Boolean isDeliveredAsUPC;
	private String releaseDate;
	private String originalReleaseDate;
	private String artistName;
	private String title;
	private String versionTitle;
	private String referenceTitle;
	private String referenceSubTitle;
	private String formalSubTitle;
	private String internalVersionTitle;
	private String productTitle;
	private Genre genre;
	private String audioQuality;
	private long duration;
	private boolean isRemaster;
	private String ownershipType;
	private long copyrightYear;
	private String copyrightOwner;
	private String copyrightNotice;
	private long artworkYear;
	private String artworkOwner;
	private String artworkNotice;
	private String parentalAdvisoryArtworkIndicator;
	private String parentalAdvisoryLyricsIndicator;
	private RepertoireOwnerCompany repertoireOwnerCompany;
	private Label label;
	private SAPInternalCode sapInternalCode;
	private CaptureProductFormat format;
	private RightsCode contractualRightsCode;
	private RightsCode marketingRightsCode;
	private Language metadataLanguage;
	private boolean isSoundTrack;
	private boolean isDjmix;
	private boolean isKaraoke;
	private boolean isLive;
	private boolean isRemix;
	private boolean isPartial;
	private long numberOfTracks;
	private long numberOfTracksByBMG;
	private long numberOfComponents;
	private boolean isCompilation;
	private boolean isComponent;
	private boolean isPackageOnly;
	private boolean isLicenced;
	private boolean isMultipleArtists;
	private boolean hasBooklet;
	private boolean isPromo;
	private boolean menaCompliance;
	private String coordinator;
	private String manager;
	private String created;
	private String updated;
	private String notes;
	private String durationFormatted;
	private List<String> translationCompliances = new ArrayList<>();
	private List<Project> projects = new ArrayList<>();
	private String includesDownloadCard;
	private List<CaptureTrack> tracks = new ArrayList<>();
	private List<ProductContributor> productContributors = new ArrayList<>();
	private List<CaptureComponent> component = new ArrayList<>();
	private List<DdexContributor> displayArtists = new ArrayList<>();

	@Data
	public static class Project {
		private String code;
		private String title;
	}

	@Data
	public static class RightsCode {
		private String name;
		private String code;
		private List<String> isoCountryCodes = new ArrayList<>();
	}

	@Data
	public static class ProductContributor {
		private long productContributorId;
		private long productContributorSeqNo;
		private boolean isArtist;
		private boolean isPrimary;
		private Contributor contributor;
		private List<Contributor.ArtistRole> roles;
	}

	@Data
	public static class CaptureProductFormat {
		private String name;
		private String albumSingle;
		private String audioVisual;
		private boolean isPhysical;
		private String reMaConfigCode;
		private boolean isPackage;
		private boolean isNonMusicalAsset;
		private String DDEXReleaseType;
		private String PartnerReleaseType;
		private String DDEXCarrierType;
		private SubTypeData subTypeData;
	}

	@Data
	public static class SAPInternalCode {
		private String code;
		private String category;
		private String sapCompanyCode;
	}

	@Data
	public static class SubTypeData {
		private String CaptureFormat;
		private String SubType;
		private String PartnerReleaseType;
		private String DDEXReleaseType;
		private String DDEXCarrierType;
		private Boolean hasSubType;
		private String DDEXReleaseTypeSD;
		private String PartnerReleaseTypeSD;
		private String DDEXReleaseTypeHD;
		private String PartnerReleaseTypeHD;
		private String DDEXReleaseTypeMFiT;
		private String PartnerReleaseTypeMFiT;
	}

	@Data
	public static class DdexContributor {
		private long seqNo;
		private long partyId;
		private String firstName;
		private String lastName;
		private String fullName;
		private String artistRoleType;
		private List<String> roles = new ArrayList<>();
		private Set<String> externalCodes = new HashSet<>();
	}

	public List<CaptureTrack> getTracks() {
		List<CaptureTrack> trackList = new ArrayList<>();
		if (this.format.isPackage) {
			for (CaptureComponent component : this.component) {
				trackList.addAll(component.getTracks());
			}
			return trackList;
		}
		return this.tracks;
	}
}
