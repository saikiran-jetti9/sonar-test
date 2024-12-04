package com.bmg.deliver.dto;

import lombok.Data;

@Data
public class WorkflowStepConfigurationDTO {
	private Long id;
	private Long workflowStepId;
	private String key;
	private String value;
}
