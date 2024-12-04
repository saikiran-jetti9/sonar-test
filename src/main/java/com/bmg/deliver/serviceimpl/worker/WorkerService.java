package com.bmg.deliver.serviceimpl.worker;

import com.bmg.deliver.config.WorkerCondition;
import com.bmg.deliver.dto.WorkflowInstanceMessageDTO;
import com.bmg.deliver.model.*;
import com.bmg.deliver.rabbitmq.worker.WorkerProducer;
import com.bmg.deliver.repository.*;
import com.bmg.deliver.service.WorkflowInstanceService;
import com.bmg.deliver.workflow.execution.ExecutionContext;
import com.bmg.deliver.workflow.step.Step;
import com.bmg.deliver.workflow.step.StepFactory;
import com.bmg.deliver.workflow.step.StepParams;
import com.bmg.deliver.workflow.step.StepResult;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.transaction.Transactional;
import java.util.*;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Conditional(WorkerCondition.class)
@Data
public class WorkerService {
	private final WorkerProducer workerProducer;
	private final ObjectMapper objectMapper = new ObjectMapper();
	private final WorkflowInstanceService workflowInstanceService;
	private final WorkflowInstanceRepository workflowInstanceRepository;
	private final WorkflowStepRepository workflowStepRepository;
	private final WorkflowInstanceArtifactRepository workflowInstanceArtifactRepository;
	private final WorkflowStepConfigurationRepository workflowStepConfigurationRepository;
	private final StepFactory stepFactory;
	private final SimpMessagingTemplate messagingTemplate;

	@Value("${app.dirs.attachments}")
	String attachmentsDir;

	@Value("${spring.activemq.topic}")
	private String topic;

	public WorkerService(WorkerProducer workerProducer, WorkflowInstanceService workflowInstanceService,
			WorkflowInstanceRepository workflowInstanceRepository, StepFactory stepFactory,
			WorkflowStepRepository workflowStepRepository,
			WorkflowInstanceArtifactRepository workflowInstanceArtifactRepository,
			WorkflowStepConfigurationRepository workflowStepConfigurationRepository,
			SimpMessagingTemplate messagingTemplate) {
		this.workerProducer = workerProducer;
		this.workflowInstanceService = workflowInstanceService;
		this.workflowInstanceRepository = workflowInstanceRepository;
		this.stepFactory = stepFactory;
		this.workflowStepRepository = workflowStepRepository;
		this.workflowInstanceArtifactRepository = workflowInstanceArtifactRepository;
		this.workflowStepConfigurationRepository = workflowStepConfigurationRepository;
		this.messagingTemplate = messagingTemplate;
	}

	public void processWorkflowInstanceResult(WorkflowInstance workflowInstance) {
		try {
			WorkflowInstanceMessageDTO message = new WorkflowInstanceMessageDTO();
			message.setId(workflowInstance.getId());
			message.setWorkflowId(workflowInstance.getWorkflow().getId());
			message.setPriority(workflowInstance.getPriority());

			workerProducer.sendMessage(objectMapper.writeValueAsString(message));
		} catch (Exception e) {
			log.error("Exception while sending RMQ message: {}", ExceptionUtils.getStackTrace(e));
		}
	}

	@Transactional
	public ExecutionContext buildExecutionContext(WorkflowInstanceMessageDTO workflowInstanceMessageDTO) {
		try {
			Optional<WorkflowInstance> workflowInstanceOptional = workflowInstanceService
					.getWorkflowInstanceById(workflowInstanceMessageDTO.getId());
			if (workflowInstanceOptional.isPresent()) {

				WorkflowInstance workflowInstance = workflowInstanceOptional.get();
				// workflowInstance.setStatus(WorkflowInstanceStatus.RUNNING);
				// workflowInstanceRepository.save(workflowInstance);

				ExecutionContext context = new ExecutionContext(workflowInstance, attachmentsDir);
				context.setSteps(getStepsForWorkflowInstance(context));
				return context;
			}
		} catch (Exception e) {
			log.error("Exception while building execution context: {}", ExceptionUtils.getStackTrace(e));
		}
		return null;
	}

	public List<Step> getStepsForWorkflowInstance(ExecutionContext context) {
		Optional<WorkflowInstance> optionalWorkflowInstance = workflowInstanceRepository
				.findById(context.getWorkflowInstance().getId());
		if (optionalWorkflowInstance.isPresent()) {
			WorkflowInstance workflowInstance = optionalWorkflowInstance.get();
			List<WorkflowStep> workflowSteps = workflowStepRepository
					.findByWorkflowIdOrderByExecutionOrder(workflowInstance.getWorkflow().getId()).stream().toList();
			List<Step> steps = new ArrayList<>();

			for (WorkflowStep step : workflowSteps) {
				List<WorkflowStepConfiguration> workflowStepConfigurations = workflowStepConfigurationRepository
						.findByWorkflowStepId(step.getId());
				StepParams stepParams = new StepParams(step.getId(), step.getWorkflow(), step.getExecutionOrder(),
						step.getName(), step.getType(), workflowStepConfigurations);
				Step newStep = stepFactory.createStep(stepParams);
				steps.add(newStep);
			}
			return steps;
		}
		return Collections.emptyList();
	}

	@Transactional
	public void updateWorkflowInstanceStatus(ExecutionContext context, StepResult result, long duration) {
		Map<Long, Set<WorkflowInstanceArtifact>> artifactMap = context.getArtifactMap();
		Optional<WorkflowInstance> optionalWorkflowInstance = workflowInstanceRepository
				.findById(context.getWorkflowInstance().getId());
		if (optionalWorkflowInstance.isPresent()) {
			WorkflowInstance workflowInstance = optionalWorkflowInstance.get();
			List<WorkflowStep> workflowSteps = workflowStepRepository
					.findByWorkflowIdOrderByExecutionOrder(workflowInstance.getWorkflow().getId()).stream().toList();

			List<WorkflowInstanceArtifact> workflowInstanceArtifacts = new ArrayList<>();
			for (WorkflowStep step : workflowSteps) {
				Set<WorkflowInstanceArtifact> artifacts = artifactMap.get(step.getId());

				if (artifacts != null) {
					for (WorkflowInstanceArtifact artifact : artifacts) {
						artifact.setWorkflowInstance(workflowInstance);
						artifact.setWorkflowStep(step);
						workflowInstanceArtifacts.add(artifact);
					}
				}
			}
			workflowInstance.setStatus(result.getStatus());
			workflowInstance.setCompleted(new Date());
			workflowInstance.setDuration(duration);
			workflowInstance.setLog(context.getLogger().getLogs().toString());

			if (!result.isSuccess()) {
				workflowInstance.setErrorMessage(result.getMessage());
			}

			workflowInstanceArtifactRepository.saveAll(workflowInstanceArtifacts);
			workflowInstanceRepository.save(workflowInstance);
			processWorkflowInstanceResult(workflowInstance);
		}
	}
}
