package com.bmg.deliver.enums;

import lombok.Getter;

@Getter
public enum DeliveryType {
	DATA_ONLY("Data Only Trigger"), PACKSHOT("Packshot"), FULL_DELIVERY("Full Trigger"), SCREENGRAB(
			"Screengrab"), COVER_ART("Cover Art"), INSERT("Insert"), NONE("None"), TAKE_DOWN("Takedown");

	private final String value;

	DeliveryType(final String value) {
		this.value = value;
	}

	public static DeliveryType fromValue(String value) {
		for (DeliveryType type : values()) {
			if (type.getValue().equalsIgnoreCase(value)) {
				return type;
			}
		}
		return NONE;
	}
}