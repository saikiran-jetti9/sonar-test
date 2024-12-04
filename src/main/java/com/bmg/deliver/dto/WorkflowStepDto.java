package com.bmg.deliver.dto;

import com.bmg.deliver.enums.WorkflowStepType;
import lombok.Data;

import java.util.Date;
import java.util.List;
@Data
public class WorkflowStepDto {
	private Long id;
	private Long workflowId;
	private int executionOrder;
	private String name;
	private WorkflowStepType type;
	private Date created;
	private Date modified;
	List<WorkflowStepConfigurationDTO> workflowStepConfigurations;
}