package com.bmg.deliver.dto.responsedto;

import com.bmg.deliver.enums.DeliveryType;
import com.bmg.deliver.enums.Priority;
import com.bmg.deliver.enums.WorkflowInstanceStatus;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
public class ResponseWorkflowInstanceDTO {
	private Long id;
	private Long workflowId;
	private String workflowName;
	private WorkflowInstanceStatus status;
	private Date completed;
	private Long duration;
	private String identifier;
	private Priority priority;
	private DeliveryType deliveryType;
	private Date started;
	private Date created;
	private Date modified;
}
