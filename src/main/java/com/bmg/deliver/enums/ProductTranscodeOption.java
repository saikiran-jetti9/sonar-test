package com.bmg.deliver.enums;

import lombok.Getter;

@Getter
public enum ProductTranscodeOption {
	NONE("none"), JPG("jpg");

	private final String assetOption;

	ProductTranscodeOption(String assetOption) {
		this.assetOption = assetOption;
	}
}
