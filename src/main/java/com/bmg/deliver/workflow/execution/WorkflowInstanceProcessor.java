package com.bmg.deliver.workflow.execution;

import com.bmg.deliver.config.WorkerCondition;
import com.bmg.deliver.dto.TotalStatusCountDTO;
import com.bmg.deliver.dto.WorkflowInstanceMessageDTO;
import com.bmg.deliver.enums.WorkflowInstanceStatus;
import com.bmg.deliver.model.WorkflowInstance;
import com.bmg.deliver.repository.WorkflowInstanceArtifactRepository;
import com.bmg.deliver.repository.WorkflowInstanceRepository;
import com.bmg.deliver.service.WorkflowInstanceService;
import com.bmg.deliver.serviceimpl.worker.WorkerService;
import com.bmg.deliver.workflow.step.Step;
import com.bmg.deliver.workflow.step.StepResult;

import java.util.Date;
import java.util.List;
import java.util.Optional;

import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Conditional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Conditional(WorkerCondition.class)
public class WorkflowInstanceProcessor {
	WorkflowInstanceArtifactRepository workflowInstanceArtifactRepository;

	private final WorkflowInstanceRepository workflowInstanceRepository;
	WorkerService workerService;
	SimpMessagingTemplate messagingTemplate;
	WorkflowInstanceService workflowInstanceService;

	public WorkflowInstanceProcessor(WorkflowInstanceArtifactRepository workflowInstanceArtifactRepository,
			WorkerService workerService, WorkflowInstanceRepository workflowInstanceRepository,
			WorkflowInstanceService workflowInstanceService, SimpMessagingTemplate messagingTemplate) {
		this.workflowInstanceArtifactRepository = workflowInstanceArtifactRepository;
		this.workerService = workerService;
		this.workflowInstanceRepository = workflowInstanceRepository;
		this.messagingTemplate = messagingTemplate;
		this.workflowInstanceService = workflowInstanceService;
	}

	// TODO - Handle exception
	public void processor(WorkflowInstanceMessageDTO workflowInstanceMessageDTO) {
		Optional<WorkflowInstance> optionalWorkflowInstance = workflowInstanceRepository
				.findById(workflowInstanceMessageDTO.getId());
		WorkflowInstance workflowInstance = optionalWorkflowInstance.orElse(null);
		if (workflowInstance != null && workflowInstance.getStatus().equals(WorkflowInstanceStatus.PAUSED)) {
			log.info("The instance with id {} is paused", workflowInstanceMessageDTO.getId());
			return;
		}
		ExecutionContext context = workerService.buildExecutionContext(workflowInstanceMessageDTO);
		if (null != context) {
			List<Step> workflowSteps = context.getSteps();

			if (!workflowSteps.isEmpty()) {
				long startTime = System.currentTimeMillis();

				assert workflowInstance != null;
				workflowInstance.setStarted(new Date());
				workflowInstance.setStatus(WorkflowInstanceStatus.RUNNING);
				workflowInstanceRepository.save(workflowInstance);

				log.info("Before Web socket call before RUNNING status");
				TotalStatusCountDTO statusCounts = workflowInstanceService.retrieveTotalWorkflowsStatusCount();
				messagingTemplate.convertAndSend("/topic/workflow-status-counts", statusCounts);
				messagingTemplate.convertAndSend("/topic/workflow-updates",
						workflowInstanceService.retrieveStatusCountByWorkflow());
				log.info("After Web socket call after RUNNING status");

				WorkflowExecutor workflowExecution = new WorkflowExecutor(context);
				StepResult result = workflowExecution.execute();

				long endTime = System.currentTimeMillis();
				long durationInSeconds = endTime - startTime;
				workerService.updateWorkflowInstanceStatus(context, result, durationInSeconds);
			}
		}
	}
}
