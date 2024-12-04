package com.bmg.deliver.controller;

import com.bmg.deliver.dto.WorkflowStepConfigurationDTO;
import com.bmg.deliver.dto.WorkflowStepDto;
import com.bmg.deliver.dto.WorkflowStepTemplateDTO;
import com.bmg.deliver.exceptions.*;
import com.bmg.deliver.model.WorkflowStepTemplate;
import com.bmg.deliver.service.StepService;
import com.bmg.deliver.service.WorkflowStepTemplateService;
import com.bmg.deliver.utils.AppConstants;
import io.swagger.v3.oas.annotations.Operation;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("api/step")
@Slf4j
public class StepController {

	@Autowired
	private StepService stepService;

	@Autowired
	WorkflowStepTemplateService workflowStepTemplateService;

	@PostMapping
	@Operation(summary = "Create a new step", description = "Create a new step", tags = {"Step-Controller"})
	public ResponseEntity<WorkflowStepDto> createStep(@RequestBody WorkflowStepDto workflowStepDto) {
		try {
			return stepService.createStep(workflowStepDto);
		} catch (ExecutionOrderExistException ex) {
			throw new ExecutionOrderExistException(
					AppConstants.EXECUTION_ORDER_EXIST + workflowStepDto.getExecutionOrder());
		}
	}

	@PutMapping("/{id}")
	@Operation(summary = "Update a step", description = "Update a step", tags = {"Step-Controller"})
	public ResponseEntity<WorkflowStepDto> updateStep(@PathVariable Long id,
			@RequestBody WorkflowStepDto workflowStepDto) {
		try {
			return stepService.updateStep(id, workflowStepDto);
		} catch (WorkflowStepNotFoundException ex) {
			throw new WorkflowStepNotFoundException(AppConstants.WORKFLOW_STEP_NOT_FOUND + workflowStepDto.getId());
		}
	}

	@DeleteMapping("/{id}")
	@Operation(summary = "Delete a step", description = "Delete a step", tags = {"Step-Controller"})
	public void deleteStep(@PathVariable Long id) {
		try {
			stepService.deleteStep(id);
		} catch (WorkflowStepNotFoundException ex) {
			throw new WorkflowStepNotFoundException(AppConstants.WORKFLOW_STEP_NOT_FOUND + id);
		} catch (Exception e) {
			throw new ApplicationRuntimeException(AppConstants.INTERNAL_SERVER_ERROR);
		}
	}

	/**
	 * Create or Update step configurations based on step id
	 *
	 * @param id
	 * @param workflowStepConfigurationDTOs
	 * @return
	 */
	@PostMapping("{id}/configs")
	@Operation(summary = "Create or Update step configurations", description = "Create or Update step configurations", tags = {
			"Step-Controller"})
	public WorkflowStepDto createOrUpdateStepConfigs(@PathVariable Long id,
			@RequestBody List<WorkflowStepConfigurationDTO> workflowStepConfigurationDTOs) {
		try {
			return stepService.createOrUpdateStepConfigs(id, workflowStepConfigurationDTOs);
		} catch (Exception e) {
			log.error("Error while creating or updating step configurations for step id {}", id);
			throw new ApplicationRuntimeException(AppConstants.INTERNAL_SERVER_ERROR);
		}

	}

	/**
	 *
	 * @param workflowStepTemplateDTO
	 * @return
	 */
	@PostMapping("/template")
	@Operation(tags = {
			"Workflow-Step-Template"}, description = " Create or update WorkflowStepTemplate based on workflowStepId", summary = " Create WorkflowStepTemplate based on workflowStepId")
	public ResponseEntity<WorkflowStepTemplate> createOrUpdateWorkflowStepTemplate(
			@RequestBody WorkflowStepTemplateDTO workflowStepTemplateDTO) {
		try {
			Optional<WorkflowStepTemplate> exists = workflowStepTemplateService
					.getWorkflowStepTemplate(workflowStepTemplateDTO.getWorkflowStepId());
			if (exists.isEmpty()) {
				WorkflowStepTemplate workflowStepTemplate = workflowStepTemplateService
						.createWorkflowStepTemplate(workflowStepTemplateDTO);
				return new ResponseEntity<>(workflowStepTemplate, HttpStatus.CREATED);
			} else {
				WorkflowStepTemplate workflowStepTemplate = workflowStepTemplateService
						.updateWorkflowStepTemplate(workflowStepTemplateDTO);
				return new ResponseEntity<>(workflowStepTemplate, HttpStatus.OK);
			}
		} catch (Exception e) {
			log.error("Error while creating or updating workflow step template {}", ExceptionUtils.getStackTrace(e));
			throw new ApplicationRuntimeException(AppConstants.INTERNAL_SERVER_ERROR, e);
		}
	}

}