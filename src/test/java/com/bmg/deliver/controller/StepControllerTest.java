package com.bmg.deliver.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bmg.deliver.dto.WorkflowStepDto;
import com.bmg.deliver.dto.WorkflowStepTemplateDTO;
import com.bmg.deliver.exceptions.ApplicationRuntimeException;
import com.bmg.deliver.exceptions.ExecutionOrderExistException;
import com.bmg.deliver.exceptions.WorkflowStepNotFoundException;
import com.bmg.deliver.model.WorkflowStepTemplate;
import com.bmg.deliver.service.StepService;
import com.bmg.deliver.service.WorkflowStepTemplateService;
import com.bmg.deliver.utils.AppConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.Optional;

class StepControllerTest {

	@Mock
	private StepService stepService;

	@InjectMocks
	private StepController stepController;

	@Mock
	private WorkflowStepTemplateService workflowStepTemplateService;

	@BeforeEach
	public void setUp() {
		MockitoAnnotations.openMocks(this);
	}

	@Test
	void testCreateStep_ExecutionOrderExistException() {
		// Arrange
		WorkflowStepDto workflowStepDto = new WorkflowStepDto();
		workflowStepDto.setExecutionOrder(1);

		doThrow(new ExecutionOrderExistException(
				AppConstants.EXECUTION_ORDER_EXIST + workflowStepDto.getExecutionOrder())).when(stepService)
				.createStep(workflowStepDto);

		// Act & Assert
		ExecutionOrderExistException exception = assertThrows(ExecutionOrderExistException.class,
				() -> stepController.createStep(workflowStepDto));

		assertEquals(AppConstants.EXECUTION_ORDER_EXIST + workflowStepDto.getExecutionOrder(), exception.getMessage());
	}

	@Test
	void testUpdateStep_WorkflowStepNotFoundException() {
		Long id = 1L;
		// Arrange
		WorkflowStepDto workflowStepDto = new WorkflowStepDto();
		workflowStepDto.setId(101L);

		doThrow(new WorkflowStepNotFoundException(AppConstants.WORKFLOW_STEP_NOT_FOUND + workflowStepDto.getId()))
				.when(stepService).updateStep(id, workflowStepDto);

		// Act & Assert
		WorkflowStepNotFoundException exception = assertThrows(WorkflowStepNotFoundException.class,
				() -> stepController.updateStep(id, workflowStepDto));

		assertEquals(AppConstants.WORKFLOW_STEP_NOT_FOUND + workflowStepDto.getId(), exception.getMessage());
	}

	@Test
	void testDeleteStep_WorkflowStepNotFoundException() {
		// Arrange
		Long stepId = 101L;

		doThrow(new WorkflowStepNotFoundException(AppConstants.WORKFLOW_STEP_NOT_FOUND + stepId)).when(stepService)
				.deleteStep(stepId);

		// Act & Assert
		WorkflowStepNotFoundException exception = assertThrows(WorkflowStepNotFoundException.class,
				() -> stepController.deleteStep(stepId));

		assertEquals(AppConstants.WORKFLOW_STEP_NOT_FOUND + stepId, exception.getMessage());
	}

	@Test
	void testDeleteStep_ApplicationRuntimeException() {
		// Arrange
		Long stepId = 101L;

		doThrow(new RuntimeException("Unexpected error")).when(stepService).deleteStep(stepId);

		// Act & Assert
		ApplicationRuntimeException exception = assertThrows(ApplicationRuntimeException.class,
				() -> stepController.deleteStep(stepId));

		assertEquals(AppConstants.INTERNAL_SERVER_ERROR, exception.getMessage());
	}

	@Test
	void testCreateWorkflowStepTemplate() {
		// Arrange
		WorkflowStepTemplateDTO dto = new WorkflowStepTemplateDTO();
		dto.setWorkflowStepId(1L);

		WorkflowStepTemplate newTemplate = new WorkflowStepTemplate();
		when(workflowStepTemplateService.getWorkflowStepTemplate(dto.getWorkflowStepId())).thenReturn(Optional.empty());
		when(workflowStepTemplateService.createWorkflowStepTemplate(dto)).thenReturn(newTemplate);

		ResponseEntity<WorkflowStepTemplate> response = stepController.createOrUpdateWorkflowStepTemplate(dto);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertEquals(newTemplate, response.getBody());
	}

	@Test
	void testUpdateWorkflowStepTemplate() {
		WorkflowStepTemplateDTO dto = new WorkflowStepTemplateDTO();
		dto.setWorkflowStepId(1L);

		WorkflowStepTemplate existingTemplate = new WorkflowStepTemplate();
		when(workflowStepTemplateService.getWorkflowStepTemplate(dto.getWorkflowStepId()))
				.thenReturn(Optional.of(existingTemplate));
		when(workflowStepTemplateService.updateWorkflowStepTemplate(dto)).thenReturn(existingTemplate);

		ResponseEntity<WorkflowStepTemplate> response = stepController.createOrUpdateWorkflowStepTemplate(dto);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(existingTemplate, response.getBody());
	}

	@Test
	void testCreateOrUpdateWorkflowStepTemplateHandlesException() {
		WorkflowStepTemplateDTO dto = new WorkflowStepTemplateDTO();
		dto.setWorkflowStepId(2L);

		when(workflowStepTemplateService.getWorkflowStepTemplate(dto.getWorkflowStepId()))
				.thenThrow(new RuntimeException("Test Exception"));

		Exception exception = assertThrows(ApplicationRuntimeException.class, () -> {
			stepController.createOrUpdateWorkflowStepTemplate(dto);
		});

		assertNotNull(exception);
		assertEquals(AppConstants.INTERNAL_SERVER_ERROR, exception.getMessage());
	}
}
