package com.bmg.deliver.dto;

import lombok.Data;

@Data
public class WorkflowInstanceArtifactDTO {
	private Long id;
	private Long workflowStepId;
	private Long workflowInstanceId;
	private String description;
	private String filename;
	private String uniqueFilename;
}
