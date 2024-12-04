package com.bmg.deliver.model.product;

import lombok.Data;

import java.util.List;

@Data
public class CaptureTrack {
	private long trackId;
	private long sideNumber;
	private long trackNumber;
	private long displayTrackNumber;
	private long trackSequenceNumber;
	private boolean isHidden;
	private boolean isGapless;
	private boolean isBonus;
	private String grId;
	private String overrideArtistName;
	private String overrideTitle;
	private String overrideVersionTitle;
	private long overrideDuration;
	private String overrideRecordingTitle;
	private Recording recording;

	public ReleaseTrack getReleaseTrack(ReleaseProduct releaseProduct) {
		List<ReleaseTrack> tracks = releaseProduct.getTracks();
		for (ReleaseTrack track : tracks) {
			if (track.getCaptureTrackId() == this.trackId) {
				return track;
			}
		}
		return null;
	}
}
