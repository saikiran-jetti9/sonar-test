package com.bmg.deliver.workflow.step;

import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.model.WorkflowStepConfiguration;
import com.bmg.deliver.enums.WorkflowStepType;
import lombok.Data;

import java.util.List;

@Data
public class StepParams {
	private final Long id;
	private final Workflow workflow;
	private final Integer executionOrder;
	private final String name;
	private final WorkflowStepType type;
	private final List<WorkflowStepConfiguration> stepConfigurations;

	public StepParams(Long id, Workflow workflow, Integer executionOrder, String name, WorkflowStepType type,
			List<WorkflowStepConfiguration> stepConfigurations) {
		this.id = id;
		this.workflow = workflow;
		this.executionOrder = executionOrder;
		this.name = name;
		this.type = type;
		this.stepConfigurations = stepConfigurations;
	}

}
