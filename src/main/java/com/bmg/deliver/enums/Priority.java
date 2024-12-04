package com.bmg.deliver.enums;

import lombok.Getter;

@Getter
public enum Priority {
	HIGH(1), MEDIUM(2), LOW(3);

	private final int priorityValue;

	Priority(int priorityValue) {
		this.priorityValue = priorityValue;
	}
}
