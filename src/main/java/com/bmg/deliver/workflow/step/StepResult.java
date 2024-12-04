package com.bmg.deliver.workflow.step;

import com.bmg.deliver.enums.WorkflowInstanceStatus;
import lombok.Getter;

@Getter
public class StepResult {
	WorkflowInstanceStatus status;
	boolean success;
	private final String message;

	public StepResult(boolean success, String message) {
		if (success) {
			this.status = WorkflowInstanceStatus.COMPLETED;
		} else {
			this.status = WorkflowInstanceStatus.FAILED;
		}
		this.success = success;
		this.message = message;
	}
}
