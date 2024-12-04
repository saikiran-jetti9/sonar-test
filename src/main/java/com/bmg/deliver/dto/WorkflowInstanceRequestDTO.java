package com.bmg.deliver.dto;

import com.bmg.deliver.enums.Priority;
import com.fasterxml.jackson.databind.node.ObjectNode;
import lombok.Data;

@Data
public class WorkflowInstanceRequestDTO {
	private Long workflowId;
	private ObjectNode triggerData;
	private String identifier;
	private Priority priority;
	private String reason;
}
