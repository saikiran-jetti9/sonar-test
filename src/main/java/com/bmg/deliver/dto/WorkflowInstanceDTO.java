package com.bmg.deliver.dto;

import com.bmg.deliver.enums.Priority;
import com.bmg.deliver.enums.WorkflowInstanceStatus;
import java.util.Date;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class WorkflowInstanceDTO {
	private Long workflowId;
	private WorkflowInstanceStatus status;
	private Date completed;
	private Long duration;
	private String reason;
	private String triggerData;
	private String log;
	private String identifier;
	private String errorMessage;
	private Priority priority;
}
