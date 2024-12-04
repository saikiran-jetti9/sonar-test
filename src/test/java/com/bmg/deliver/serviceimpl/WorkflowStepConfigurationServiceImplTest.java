package com.bmg.deliver.serviceimpl;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.bmg.deliver.dto.WorkflowStepConfigurationDTO;
import com.bmg.deliver.exceptions.WorkflowNotFoundException;
import com.bmg.deliver.model.WorkflowStep;
import com.bmg.deliver.model.WorkflowStepConfiguration;
import com.bmg.deliver.repository.WorkflowStepConfigurationRepository;
import com.bmg.deliver.repository.WorkflowStepRepository;
import com.bmg.deliver.utils.AppConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class WorkflowStepConfigurationServiceImplTest {

	@InjectMocks
	private WorkflowStepConfigurationServiceImpl workflowStepConfigurationService;

	@Mock
	private WorkflowStepConfigurationRepository workflowStepConfigurationRepository;

	@Mock
	private WorkflowStepRepository workflowStepRepository;

	ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() throws IOException {
		MockitoAnnotations.initMocks(this);
		ReflectionTestUtils.setField(workflowStepConfigurationService, "objectMapper", objectMapper);
	}

	@Test
	void testCreateWorkFlowStepConfigurationSuccess() {
		WorkflowStepConfigurationDTO workflowStepConfigurationDTO = new WorkflowStepConfigurationDTO();
		workflowStepConfigurationDTO.setKey("key");
		workflowStepConfigurationDTO.setValue("value");
		workflowStepConfigurationDTO.setWorkflowStepId(1L);

		WorkflowStep workflowStep = new WorkflowStep();
		when(workflowStepRepository.findById(any())).thenReturn(Optional.of(workflowStep));

		WorkflowStepConfiguration workflowStepConfiguration = new WorkflowStepConfiguration();
		workflowStepConfiguration.setKey("key");
		workflowStepConfiguration.setValue("value");
		workflowStepConfiguration.setWorkflowStep(workflowStep);
		when(workflowStepConfigurationRepository.save(any())).thenReturn(workflowStepConfiguration);

		WorkflowStepConfiguration result = workflowStepConfigurationService.createWorkFlowStepConfiguration(
				workflowStepConfigurationDTO.getWorkflowStepId(), workflowStepConfigurationDTO);
		assertEquals(workflowStepConfiguration, result);

	}

	@Test
	void testDeleteWorkflowStepConfigurationByIdSuccess() {
		Long workflowStepConfigurationId = 1L;
		workflowStepConfigurationService.deleteWorkflowStepConfigurationById(workflowStepConfigurationId);
		assertFalse(workflowStepConfigurationRepository.findById(workflowStepConfigurationId).isPresent(),
				"Configuration should be deleted");
	}

	@Test
	void testDeleteWorkflowStepConfigurationByIdException() {
		Long workflowStepConfigurationId = 1L;
		doThrow(new RuntimeException()).when(workflowStepConfigurationRepository).deleteById(anyLong());
		WorkflowNotFoundException exception = assertThrows(WorkflowNotFoundException.class, () -> {
			workflowStepConfigurationService.deleteWorkflowStepConfigurationById(workflowStepConfigurationId);
		});
		assertEquals(AppConstants.ERROR_DELETING_WORKFLOW_STEP_CONFIGURATION, exception.getMessage());
	}

	@Test
	void testGetWorkflowStepConfigurationByIdSuccess() {
		Long id = 1L;
		List<WorkflowStepConfiguration> mockPage = new ArrayList<>();
		when(workflowStepConfigurationRepository.findByWorkflowStepId(eq(id))).thenReturn(mockPage);
		List<WorkflowStepConfiguration> result = workflowStepConfigurationService.getWorkflowStepConfigurationById(id);
		assertEquals(mockPage, result);
	}

	@Test
	void testGetWorkflowStepConfigurationByIdException() {
		Long id = 1L;
		WorkflowNotFoundException expectedException = new WorkflowNotFoundException(
				AppConstants.ERROR_GETTING_WORKFLOW_STEP_CONFIGURATION);

		doThrow(expectedException).when(workflowStepConfigurationRepository).findByWorkflowStepId(anyLong(), any());

		WorkflowNotFoundException exception = assertThrows(WorkflowNotFoundException.class, () -> {
			workflowStepConfigurationService.getWorkflowStepConfigurationById(id);
		});

		assertEquals(AppConstants.ERROR_GETTING_WORKFLOW_STEP_CONFIGURATION, exception.getMessage());
	}

	@Test
	void testGetWorkflowStepConfigurationByKeySuccess() {
		String key = "key";
		WorkflowStepConfiguration workflowStepConfiguration = new WorkflowStepConfiguration();
		when(workflowStepConfigurationRepository.findByKey(key)).thenReturn(workflowStepConfiguration);

		WorkflowStepConfiguration result = workflowStepConfigurationService.getWorkflowStepConfigurationByKey(key);
		assertEquals(workflowStepConfiguration, result);

	}

	@Test
	void testGetWorkflowStepConfigurationByKeyException() {
		String key = "key";
		WorkflowNotFoundException expectedException = new WorkflowNotFoundException(
				AppConstants.ERROR_GETTING_WORKFLOW_STEP_CONFIGURATION);
		doThrow(expectedException).when(workflowStepConfigurationRepository).findByKey(anyString());

		WorkflowNotFoundException exception = assertThrows(WorkflowNotFoundException.class, () -> {
			workflowStepConfigurationService.getWorkflowStepConfigurationByKey(key);
		});

		assertEquals(AppConstants.ERROR_GETTING_WORKFLOW_STEP_CONFIGURATION, exception.getMessage());
	}

	@Test
	void testGetWorkflowStepConfigurationDtoByWorkflowStepIdSuccess() {
		WorkflowStep workflowStep = new WorkflowStep();
		workflowStep.setId(1L);
		Long id = 1L;
		List<WorkflowStepConfiguration> workflowStepConfigurations = new ArrayList<>();
		WorkflowStepConfiguration workflowStepConfiguration = new WorkflowStepConfiguration();
		workflowStepConfiguration.setId(1L);
		workflowStepConfiguration.setKey("key");
		workflowStepConfiguration.setValue("value");
		workflowStepConfiguration.setWorkflowStep(workflowStep);
		workflowStepConfigurations.add(workflowStepConfiguration);

		when(workflowStepConfigurationRepository.findByWorkflowStepId(id)).thenReturn(workflowStepConfigurations);

		List<WorkflowStepConfigurationDTO> result = workflowStepConfigurationService
				.getWorkflowStepConfigurationDtoByWorkflowStepId(id);
		assertEquals(workflowStepConfigurations.size(), result.size());

	}

	@Test
	void testUpdateWorkflowStepConfigurationSuccess() {

		WorkflowStepConfigurationDTO workflowStepConfigurationDTO = new WorkflowStepConfigurationDTO();
		workflowStepConfigurationDTO.setKey("key");
		workflowStepConfigurationDTO.setValue("value");
		workflowStepConfigurationDTO.setWorkflowStepId(1L);

		WorkflowStepConfiguration workflowStepConfiguration = new WorkflowStepConfiguration();
		workflowStepConfiguration.setKey("key");
		workflowStepConfiguration.setValue("value");
		when(workflowStepConfigurationRepository.findByWorkflowStepIdAndKey(
				workflowStepConfigurationDTO.getWorkflowStepId(), workflowStepConfigurationDTO.getKey()))
				.thenReturn((workflowStepConfiguration));
		workflowStepConfigurationService.updateWorkflowStepConfiguration(1L, workflowStepConfigurationDTO);
		assertEquals("key", workflowStepConfiguration.getKey());
		assertEquals("value", workflowStepConfiguration.getValue());
	}

	@Test
	void testUpdateWorkflowStepConfigurationException() {
		WorkflowStepConfigurationDTO workflowStepConfigurationDTO = new WorkflowStepConfigurationDTO();
		workflowStepConfigurationDTO.setKey("key");
		workflowStepConfigurationDTO.setValue("value");

		WorkflowStepConfiguration workflowStepConfiguration = new WorkflowStepConfiguration();
		when(workflowStepConfigurationRepository.findByWorkflowStepIdAndKey(
				workflowStepConfigurationDTO.getWorkflowStepId(), workflowStepConfigurationDTO.getKey()))
				.thenReturn((workflowStepConfiguration));
		doThrow(new RuntimeException()).when(workflowStepConfigurationRepository).save(any());
		WorkflowNotFoundException exception = assertThrows(WorkflowNotFoundException.class, () -> {
			workflowStepConfigurationService.updateWorkflowStepConfiguration(1L, workflowStepConfigurationDTO);
		});
		assertEquals(AppConstants.ERROR_UPDATING_WORKFLOW_STEP_CONFIGURATION, exception.getMessage());
	}
}
