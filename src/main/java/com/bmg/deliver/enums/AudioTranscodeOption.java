package com.bmg.deliver.enums;

import lombok.Getter;

@Getter
public enum AudioTranscodeOption {
	NONE("none"), MP3("mp3_320"), FLAC("flac"), STANDARD_DEFINITION("wav_16");

	private final String assetOption;

	AudioTranscodeOption(String assetOption) {
		this.assetOption = assetOption;
	}
}
