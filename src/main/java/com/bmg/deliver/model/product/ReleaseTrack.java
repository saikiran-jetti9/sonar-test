package com.bmg.deliver.model.product;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class ReleaseTrack {
	private long trackId;
	private int idx;
	private long captureTrackId;
	private long captureRecordingId;
	private int sideNumber;
	private int trackNumber;
	private boolean instantGrat;
	private String instantGratOverride;
	private boolean instantGratTimed;
	private String instantGratOverrideLocalTime;
	private String instantGratOverrideZone;
	private String instantGratOverrideTimeAndZone;
	private String instantGratOverrideDateTime;
	private boolean preStream;
	private String preStreamOverride;
	private boolean preStreamTimed;
	private String preStreamOverrideLocalTime;
	private String preStreamOverrideZone;
	private String preStreamOverrideTimeAndZone;
	private String preStreamOverrideDateTime;
	private boolean individualAvailable;
	private String availability;
	private String priceTier;
	private String priceDescription;
	private Asset selectedAsset;
	private String isrc;
	private String recordingType;
	private String recordingGroup;
	private Long previewStartTime;
	private YoutubeShort shorts;
	private String earliestReleaseDate;
	private List<Distributor> distributors = new ArrayList<>();
	private String selectedAssetDuration;
	private String aRef;
	private String rRef;

	@Data
	public static class YoutubeShort {
		private String startPoint;
		private String endPoint;
		private String duration;
		private String startDateTimeUTC;
		private String startDate;
		private String startTime;
	}
}
