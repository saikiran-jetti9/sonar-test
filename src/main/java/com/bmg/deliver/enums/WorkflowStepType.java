package com.bmg.deliver.enums;

import com.bmg.deliver.workflow.step.Step;
import com.bmg.deliver.workflow.steps.*;
import lombok.Getter;

@Getter
public enum WorkflowStepType {
	DDEX(DDEXStep.class), SFTP(SFTPStep.class), GCS_UPLOADER(GCSUploaderStep.class), XML_RUNNER(XMLStep.class);

	private final Class<? extends Step> stepClass;

	WorkflowStepType(Class<? extends Step> stepClass) {
		this.stepClass = stepClass;
	}
}
