package com.bmg.deliver.workflow.execution;

import com.bmg.deliver.workflow.step.Step;
import com.bmg.deliver.workflow.step.StepResult;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class WorkflowExecutor {
	private final ExecutionContext context;
	private final List<Step> workflowStep;

	public WorkflowExecutor(ExecutionContext context) {
		this.context = context;
		this.workflowStep = context.getSteps();
	}

	public StepResult execute() {
		Logger logger = context.getLogger();
		logger.info("WorkflowInstance was started");
		for (Step step : workflowStep) {
			context.setCurrentStepId(step.getId());

			logger.info("[START] Executing %s", step.getType());
			StepResult result = step.execute(context);
			if (!result.isSuccess()) {
				logger.info("[ERROR] %s failed with an error: %s", step.getType(), result.getMessage());
				logger.info("WorkflowInstance is done with ERROR");
				return result;
			}
			logger.info("[DONE] %s Completed successfully!", step.getType());
		}
		logger.info("WorkflowInstance is done with SUCCESS");
		return new StepResult(true, "Workflow execution completed successfully");
	}
}
