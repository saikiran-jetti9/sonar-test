package com.bmg.deliver.dto;

import com.bmg.deliver.enums.Priority;
import lombok.Data;

@Data
public class WorkflowInstanceMessageDTO implements Comparable<WorkflowInstanceMessageDTO> {
	private Long id;
	private Long workflowId;
	private Priority priority;

	@Override
	public int compareTo(WorkflowInstanceMessageDTO other) {
		int priorityComparison = this.priority.getPriorityValue() - other.priority.getPriorityValue();

		// Compare priorities
		if (priorityComparison != 0) {
			return priorityComparison;
		}

		// If priorities are equal, sort by id
		return this.id.compareTo(other.id);
	}
}
