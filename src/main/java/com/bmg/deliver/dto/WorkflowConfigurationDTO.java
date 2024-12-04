package com.bmg.deliver.dto;

import lombok.Data;

@Data
public class WorkflowConfigurationDTO {
	private Long id;
	private Long workflowId;
	private String key;
	private String value;
}
