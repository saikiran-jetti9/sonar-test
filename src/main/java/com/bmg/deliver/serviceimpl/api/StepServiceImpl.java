package com.bmg.deliver.serviceimpl.api;

import com.bmg.deliver.dto.WorkflowStepConfigurationDTO;
import com.bmg.deliver.dto.WorkflowStepDto;
import com.bmg.deliver.exceptions.ApplicationRuntimeException;
import com.bmg.deliver.exceptions.ExecutionOrderExistException;
import com.bmg.deliver.exceptions.WorkflowStepNotFoundException;
import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.model.WorkflowStep;
import com.bmg.deliver.model.WorkflowStepConfiguration;
import com.bmg.deliver.repository.*;
import com.bmg.deliver.service.StepService;
import com.bmg.deliver.service.WorkflowStepConfigurationService;
import com.bmg.deliver.utils.AppConstants;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
public class StepServiceImpl implements StepService {

	@Autowired
	private WorkflowStepRepository stepRepository;

	@Autowired
	private WorkflowRepository workflowRepository;

	@Autowired
	private WorkflowStepConfigurationRepository workflowStepConfigurationRepository;

	@Autowired
	private WorkflowInstanceArtifactRepository workflowInstanceArtifactRepository;

	@Autowired
	private WorkflowStepConfigurationService workflowStepConfigurationService;

	@Autowired
	private WorkflowStepTemplateRepository workflowStepTemplateRepository;

	/**
	 * Create a new step Creating a new step involves creating a new WorkflowStep
	 * object and saving it to the database along with the default configurations
	 * for the step
	 *
	 * @param workflowStepDto
	 */
	@Override
	@Transactional
	public ResponseEntity<WorkflowStepDto> createStep(WorkflowStepDto workflowStepDto) {

		// Check if the execution order already exists
		isExecutionOrderExists(workflowStepDto.getWorkflowId(), workflowStepDto.getExecutionOrder());

		WorkflowStep workflowStep = new WorkflowStep();
		workflowStep.setWorkflow(getWorkflow(workflowStepDto.getWorkflowId()));
		workflowStep.setName(workflowStepDto.getName());
		workflowStep.setType(workflowStepDto.getType());
		workflowStep.setExecutionOrder(workflowStepDto.getExecutionOrder());
		WorkflowStep workflowStep1 = stepRepository.save(workflowStep);

		// create step configurations
		if (workflowStepDto.getWorkflowStepConfigurations() != null
				&& !workflowStepDto.getWorkflowStepConfigurations().isEmpty()) {
			WorkflowStepDto workflowStepDto1 = createOrUpdateStepConfigs(workflowStep1.getId(),
					workflowStepDto.getWorkflowStepConfigurations());
			return ResponseEntity.ok(workflowStepDto1);
		}
		workflowStepDto.setId(workflowStep1.getId());
		workflowStepDto.setCreated(workflowStep1.getCreated());
		workflowStepDto.setModified(workflowStep1.getModified());
		workflowStepDto.setName(workflowStep1.getName());
		workflowStepDto.setType(workflowStep1.getType());
		workflowStepDto.setExecutionOrder(workflowStep1.getExecutionOrder());
		workflowStepDto.setWorkflowId(workflowStep1.getWorkflow().getId());
		return ResponseEntity.ok(workflowStepDto);
	}

	@Override
	public ResponseEntity<WorkflowStepDto> updateStep(Long id, WorkflowStepDto workflowStepDto) {
		Optional<WorkflowStep> workflowStep = stepRepository.findById(id);
		if (workflowStep.isEmpty()) {
			throw new WorkflowStepNotFoundException(AppConstants.WORKFLOW_STEP_NOT_FOUND + workflowStepDto.getId());
		}

		// Check if the execution order already exists
		if (workflowStepDto.getExecutionOrder() != workflowStep.get().getExecutionOrder()) {
			isExecutionOrderExists(workflowStepDto.getWorkflowId(), workflowStepDto.getExecutionOrder());
		}

		workflowStep.get().setName(workflowStepDto.getName());
		workflowStep.get().setType(workflowStepDto.getType());
		workflowStep.get().setExecutionOrder(workflowStepDto.getExecutionOrder());
		WorkflowStep workflowStep1 = stepRepository.save(workflowStep.get());

		// update step configurations
		if (workflowStepDto.getWorkflowStepConfigurations() != null
				&& !workflowStepDto.getWorkflowStepConfigurations().isEmpty()) {
			WorkflowStepDto workflowStepDto1 = createOrUpdateStepConfigs(workflowStep1.getId(),
					workflowStepDto.getWorkflowStepConfigurations());
			return ResponseEntity.ok(workflowStepDto1);
		}
		workflowStepDto.setId(workflowStep1.getId());
		workflowStepDto.setCreated(workflowStep1.getCreated());
		workflowStepDto.setModified(workflowStep1.getModified());
		workflowStepDto.setName(workflowStep1.getName());
		workflowStepDto.setType(workflowStep1.getType());
		workflowStepDto.setExecutionOrder(workflowStep1.getExecutionOrder());
		workflowStepDto.setWorkflowId(workflowStep1.getWorkflow().getId());
		return ResponseEntity.ok(workflowStepDto);
	}

	/**
	 * Delete a step Deleting a step involves deleting the step from the database
	 * along with its configurations and artifacts
	 *
	 * @param id
	 */
	@Override
	@Transactional
	public void deleteStep(Long id) {
		try {
			Optional<WorkflowStep> workflowStep = stepRepository.findById(id);
			if (workflowStep.isEmpty()) {
				throw new WorkflowStepNotFoundException(AppConstants.WORKFLOW_STEP_NOT_FOUND + id);
			}
			workflowStepConfigurationRepository.deleteAllByWorkflowStepId(id);
			workflowInstanceArtifactRepository.deleteAllByWorkflowStepId(id);
			workflowStepTemplateRepository.deleteAllByWorkflowStepId(workflowStep.get());
			stepRepository.delete(workflowStep.get());
		} catch (Exception e) {
			log.error("Failed to delete WorkflowStep with ID: {}", id, e);
			throw new ApplicationRuntimeException("Failed to delete WorkflowStep", e);
		}

	}

	/**
	 * This method is used to create or update step configurations based on the step
	 * id. If the configurations already exist, they are updated. If they do not
	 * exist, they are created.
	 *
	 * @param stepId
	 * @param workflowStepConfigurationDTOs
	 */
	@Override
	@Transactional
	public WorkflowStepDto createOrUpdateStepConfigs(Long stepId,
			List<WorkflowStepConfigurationDTO> workflowStepConfigurationDTOs) {
		List<WorkflowStepConfiguration> existingConfigs = workflowStepConfigurationService
				.getWorkflowStepConfigurationById(stepId);
		if (existingConfigs.isEmpty()) {
			log.info("Creating new configurations for step id {}", stepId);
			for (WorkflowStepConfigurationDTO workflowStepConfigurationDTO : workflowStepConfigurationDTOs) {
				workflowStepConfigurationDTO.setWorkflowStepId(stepId);
				workflowStepConfigurationService.createWorkFlowStepConfiguration(stepId, workflowStepConfigurationDTO);
			}
		} else {
			log.info("Updating existing configurations for step id {}", stepId);
			for (WorkflowStepConfigurationDTO workflowStepConfigurationDTO : workflowStepConfigurationDTOs) {
				workflowStepConfigurationService.updateWorkflowStepConfiguration(stepId, workflowStepConfigurationDTO);
			}
		}

		return getStep(stepId);
	}

	private WorkflowStepDto getStep(Long id) {
		Optional<WorkflowStep> workflowStep = stepRepository.findById(id);
		if (workflowStep.isEmpty()) {
			throw new WorkflowStepNotFoundException(AppConstants.WORKFLOW_STEP_NOT_FOUND + id);
		}
		WorkflowStepDto workflowStepDto = new WorkflowStepDto();
		workflowStepDto.setId(workflowStep.get().getId());
		workflowStepDto.setName(workflowStep.get().getName());
		workflowStepDto.setType(workflowStep.get().getType());
		workflowStepDto.setExecutionOrder(workflowStep.get().getExecutionOrder());
		workflowStepDto.setWorkflowId(workflowStep.get().getWorkflow().getId());
		workflowStepDto.setCreated(workflowStep.get().getCreated());
		workflowStepDto.setModified(workflowStep.get().getModified());
		workflowStepDto.setWorkflowStepConfigurations(
				workflowStepConfigurationService.getWorkflowStepConfigurationDtoByWorkflowStepId(id));
		return workflowStepDto;
	}

	protected void isExecutionOrderExists(Long workflowId, int executionOrder) {
		if (stepRepository.findByWorkflowIdAndExecutionOrder(workflowId, executionOrder).isPresent()) {
			log.info("Execution order {} already exists for workflow {}", executionOrder, workflowId);
			throw new ExecutionOrderExistException(AppConstants.EXECUTION_ORDER_EXIST + workflowId);
		}
	}

	private Workflow getWorkflow(Long workflowId) {
		Optional<Workflow> workflow = workflowRepository.findById(workflowId);
		return workflow.orElse(null);
	}
}