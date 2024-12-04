package com.bmg.deliver.dto;

import lombok.Data;

@Data
public class WorkflowEmailDTO {
	private Long id;
	private Long workflowId;
	private String status;
	private String email;
	private String name;
}
