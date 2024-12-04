package com.bmg.deliver.model.product;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class Recording {
	private long recordingId;
	private Site site;
	private String royaltyCompliance;
	private String entityStatus;
	private String lockedStatus;
	private String businessUnit;
	private String category;
	private String recordingType;
	private String isrc;
	private String artistName;
	private String title;
	private String versionTitle;
	private String internalVersionTitle;
	private String recordingTitle;
	private String recordingDate;
	private long duration;
	private String durationFormatted;
	private String ownershipType;
	private Genre genre;
	private String explicitIndicator;
	private long copyrightYear;
	private boolean isRemaster;
	private boolean isCover;
	private boolean isRerecording;
	private boolean isOriginalRecording;
	private boolean isDjmix;
	private boolean isTribute;
	private String copyrightOwner;
	private String copyrightNotice;
	private boolean isLive;
	private boolean isRemix;
	private boolean isCatalogueAcquisition;
	private boolean isMasteredITunes;
	private boolean containsSample;
	private boolean isMedley;
	private String recordingLocation;
	private RepertoireOwnerCompany repertoireOwnerCompany;
	private Label label;
	private Language metadataLanguage;
	private Language recordingLanguage;
	private Country recordingCountry;
	private String created;
	private String modified;
	private List<Contributor.ExternalCode> externalCodes;
	private String rightsOwnerBeginDate;
	private List<RecordingContributor> recordingContributors;
	private List<RecordingSong> songs;

	private String ddexType;
	private String ddexResourceType;
	private boolean isInstrumental;
	private String referenceTitle;
	private String referenceSubTitle;
	private String formalSubTitle;

	private String trackDuration;
	private List<CaptureProduct.DdexContributor> displayArtists = new ArrayList<>();
	private List<CaptureProduct.DdexContributor> resourceContributors = new ArrayList<>();

	@Data
	public static class RecordingContributor {
		private long recordingContributorId;
		private long recordingContributorSeqNo;
		private String overrideFullName;
		private boolean isArtist;
		private boolean isPrimary;
		private boolean excludeFromSupplyChain;
		private Contributor contributor;
		private List<Contributor.ArtistRole> roles = new ArrayList<>();
		private String ppiCategory;
	}

}
