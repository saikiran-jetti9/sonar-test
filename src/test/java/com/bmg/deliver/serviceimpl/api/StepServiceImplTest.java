package com.bmg.deliver.serviceimpl.api;

import com.bmg.deliver.dto.WorkflowStepConfigurationDTO;
import com.bmg.deliver.dto.WorkflowStepDto;
import com.bmg.deliver.enums.WorkflowStepType;
import com.bmg.deliver.exceptions.ApplicationRuntimeException;
import com.bmg.deliver.exceptions.ExecutionOrderExistException;
import com.bmg.deliver.exceptions.WorkflowStepNotFoundException;
import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.model.WorkflowStep;
import com.bmg.deliver.model.WorkflowStepConfiguration;
import com.bmg.deliver.repository.WorkflowInstanceArtifactRepository;
import com.bmg.deliver.repository.WorkflowRepository;
import com.bmg.deliver.repository.WorkflowStepConfigurationRepository;
import com.bmg.deliver.repository.WorkflowStepRepository;
import com.bmg.deliver.service.WorkflowStepConfigurationService;
import com.bmg.deliver.utils.AppConstants;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.http.ResponseEntity;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class StepServiceImplTest {

	@InjectMocks
	private StepServiceImpl stepService;

	@Mock
	private WorkflowStepRepository stepRepository;

	@Mock
	private WorkflowRepository workflowRepository;

	@Mock
	private WorkflowStepConfigurationRepository workflowStepConfigurationRepository;

	@Mock
	private WorkflowInstanceArtifactRepository workflowInstanceArtifactRepository;

	@Mock
	private WorkflowStepConfigurationService workflowStepConfigurationService;

	private WorkflowStepDto workflowStepDto;

	private WorkflowStep workflowStep;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		workflowStepDto = new WorkflowStepDto();
		workflowStepDto.setWorkflowId(1L);
		workflowStepDto.setName("Test Step");
		workflowStepDto.setType(WorkflowStepType.DDEX);
		workflowStepDto.setExecutionOrder(1);

		workflowStep = new WorkflowStep();
		workflowStep.setId(1L);
		workflowStep.setWorkflow(new Workflow());
		workflowStep.setName("Test Step");
		workflowStep.setType(WorkflowStepType.DDEX);
		workflowStep.setExecutionOrder(1);

	}

	@Test
	void testCreateStep() {
		when(stepRepository.save(any(WorkflowStep.class))).thenReturn(workflowStep);

		ResponseEntity<WorkflowStepDto> response = stepService.createStep(workflowStepDto);

		assertEquals(200, response.getStatusCodeValue());
		assertNotNull(response.getBody());
	}

	@Test
	void testUpdateStep_Success() {

		when(stepRepository.findById(1L)).thenReturn(Optional.of(workflowStep));
		when(stepRepository.save(any(WorkflowStep.class))).thenReturn(workflowStep);

		ResponseEntity<WorkflowStepDto> response = stepService.updateStep(1L, workflowStepDto);

		assertEquals(200, response.getStatusCodeValue());
		assertNotNull(response.getBody());
	}

	@Test
	void testUpdateStep_WhenNotFound() {
		when(stepRepository.findById(1L)).thenReturn(Optional.empty());

		Exception exception = assertThrows(WorkflowStepNotFoundException.class, () -> {
			stepService.updateStep(1L, workflowStepDto);
		});

		assertEquals(AppConstants.WORKFLOW_STEP_NOT_FOUND + null, exception.getMessage());
	}

	@Test
	void testcreateStep_ExecutionOrderExistException() {
		WorkflowStepDto workflowStepDto = new WorkflowStepDto();
		workflowStepDto.setWorkflowId(1L);
		workflowStepDto.setName("name");
		workflowStepDto.setType(WorkflowStepType.DDEX);
		workflowStepDto.setExecutionOrder(1);

		Workflow workflow = new Workflow();
		workflow.setId(1L);

		when(workflowRepository.findById(workflowStepDto.getWorkflowId())).thenReturn(Optional.of(workflow));
		when(stepRepository.findByWorkflowIdAndExecutionOrder(workflowStepDto.getWorkflowId(),
				workflowStepDto.getExecutionOrder())).thenReturn(Optional.of(new WorkflowStep()));

		assertThatThrownBy(() -> stepService.createStep(workflowStepDto))
				.isInstanceOf(ExecutionOrderExistException.class)
				.hasMessage(AppConstants.EXECUTION_ORDER_EXIST + workflowStepDto.getExecutionOrder());
	}

	// @Test
	// void testdeleteStep() {
	// WorkflowStep workflowStep = new WorkflowStep();
	// workflowStep.setId(1L);
	// workflowStep.setName("name");
	// workflowStep.setType(WorkflowStepType.DDEX);
	// workflowStep.setExecutionOrder(1);
	//
	// when(stepRepository.findById(workflowStep.getId())).thenReturn(Optional.of(workflowStep));
	//
	// stepService.deleteStep(workflowStep.getId());
	//
	// verify(stepRepository, times(1)).delete(workflowStep);
	// }

	@Test
	void testdeleteStep_ApplicationRuntimeException() {
		WorkflowStep workflowStep = new WorkflowStep();
		workflowStep.setId(1L);
		workflowStep.setName("name");
		workflowStep.setType(WorkflowStepType.DDEX);
		workflowStep.setExecutionOrder(1);

		when(stepRepository.findById(workflowStep.getId())).thenReturn(Optional.of(workflowStep));
		doThrow(new RuntimeException()).when(stepRepository).delete(workflowStep);

		assertThatThrownBy(() -> stepService.deleteStep(workflowStep.getId()))
				.isInstanceOf(ApplicationRuntimeException.class).hasMessage("Failed to delete WorkflowStep");
	}

	@Test
	void createOrUpdateStepConfigs_WhenNoExistingConfigs_CreatesNewConfigs() {
		Long stepId = 1L;
		WorkflowStepConfigurationDTO config1 = new WorkflowStepConfigurationDTO();
		config1.setKey("key1");
		config1.setValue("value1");

		WorkflowStepConfigurationDTO config2 = new WorkflowStepConfigurationDTO();
		config2.setKey("key2");
		config2.setValue("value2");

		List<WorkflowStepConfigurationDTO> configs = Arrays.asList(config1, config2);

		when(stepRepository.findById(stepId)).thenReturn(Optional.of(workflowStep));
		when(workflowStepConfigurationService.getWorkflowStepConfigurationById(stepId))
				.thenReturn(Collections.emptyList());

		stepService.createOrUpdateStepConfigs(stepId, configs);

		for (WorkflowStepConfigurationDTO config : configs) {
			verify(workflowStepConfigurationService).createWorkFlowStepConfiguration(eq(stepId), eq(config));
		}
	}

	@Test
	void createOrUpdateStepConfigs_WhenExistingConfigs_UpdatesExistingConfigs() {
		Long stepId = 1L;
		WorkflowStepConfigurationDTO config1 = new WorkflowStepConfigurationDTO();
		config1.setKey("key1");
		config1.setValue("value1");

		WorkflowStepConfigurationDTO config2 = new WorkflowStepConfigurationDTO();
		config2.setKey("key2");
		config2.setValue("value2");

		List<WorkflowStepConfigurationDTO> configs = Arrays.asList(config1, config2);
		when(stepRepository.findById(stepId)).thenReturn(Optional.of(workflowStep));
		when(workflowStepConfigurationService.getWorkflowStepConfigurationById(stepId))
				.thenReturn(Arrays.asList(new WorkflowStepConfiguration()));

		stepService.createOrUpdateStepConfigs(stepId, configs);

		for (WorkflowStepConfigurationDTO config : configs) {
			verify(workflowStepConfigurationService).updateWorkflowStepConfiguration(eq(stepId), eq(config));
		}
	}

}
