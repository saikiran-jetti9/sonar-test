package com.bmg.deliver.controller;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bmg.deliver.dto.*;
import com.bmg.deliver.dto.responsedto.ResponseWorkflowInstanceDTO;
import com.bmg.deliver.dto.responsedto.WorkflowInstanceFilterDTO;
import com.bmg.deliver.enums.Priority;
import com.bmg.deliver.exceptions.*;
import com.bmg.deliver.model.*;
import com.bmg.deliver.service.WorkflowInstanceService;
import com.bmg.deliver.service.WorkflowService;
import com.bmg.deliver.service.WorkflowStepTemplateService;
import com.bmg.deliver.serviceimpl.api.WorkflowServiceImpl;
import com.bmg.deliver.utils.AppConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class WorkflowControllerTest {

	@Mock
	private WorkflowService workflowService;

	@Mock
	private WorkflowInstanceService workflowInstanceService;

	@Mock
	private WorkflowServiceImpl workflowMasterService;

	@Mock
	private WorkflowStepTemplateService workflowStepTemplateService;

	@InjectMocks
	private WorkflowController workflowController;

	private Workflow mockWorkflow;
	private WorkflowStep mockWorkflowStep;
	private WorkflowInstance mockWorkflowInstance;
	JsonNode triggerData;
	private final ObjectMapper objectMapper = new ObjectMapper();

	private WorkflowInstanceDTO mockWorkflowInstanceDTO;
	private Page<WorkflowInstance> workflowInstances;

	private WorkflowInstanceFilterDTO instanceFilterDTO;

	@BeforeEach
	void setup() {
		workflowInstances = new PageImpl<>(Collections.singletonList(new WorkflowInstance()));

		mockWorkflow = new Workflow();
		mockWorkflow.setId(1L);
		mockWorkflow.setName("Sportify");

		mockWorkflowStep = new WorkflowStep();
		mockWorkflowStep.setId(1L);
		mockWorkflowStep.setName("DDEX");

		mockWorkflowInstance = new WorkflowInstance();
		mockWorkflowInstance.setId(1L);
		mockWorkflowInstance.setWorkflow(mockWorkflow);

		mockWorkflowInstanceDTO = new WorkflowInstanceDTO();
		mockWorkflowInstanceDTO.setWorkflowId(1L);
		mockWorkflowInstanceDTO.setReason("Task from ppa");
		mockWorkflowInstanceDTO.setPriority(Priority.HIGH);
		triggerData = objectMapper.createObjectNode();

		instanceFilterDTO = new WorkflowInstanceFilterDTO();
		instanceFilterDTO.setEndDate(null);
		instanceFilterDTO.setStartDate(null);
		instanceFilterDTO.setStatus(null);
		instanceFilterDTO.setPriority(null);
		instanceFilterDTO.setDeliveryType(null);
		instanceFilterDTO.setDuration(null);
		instanceFilterDTO.setCompletedStart(null);
		instanceFilterDTO.setCompletedEnd(null);
	}

	@Test
	void testListWorkflows() {
		List<WorkflowDTO> workflows = new ArrayList<>();
		WorkflowDTO mockWorkflowdTO = new WorkflowDTO();
		workflows.add(mockWorkflowdTO);
		Page<WorkflowDTO> page = new PageImpl<>(workflows);

		when(workflowService.getAllWorkflows(any(), any(Pageable.class), any(), any(), any())).thenReturn(page);

		assertEquals(page, workflowController.listWorkflows(0, 1, "null", "asc", null, null, null, null));
	}

	@Test
	void testListWorkflows_sortByName() {
		List<WorkflowDTO> workflows = new ArrayList<>();
		WorkflowDTO mockWorkflowdTO = new WorkflowDTO();
		workflows.add(mockWorkflowdTO);
		Page<WorkflowDTO> page = new PageImpl<>(workflows);
		PageRequest expectedPageRequest = PageRequest.of(0, 10, Sort.by("name"));

		when(workflowService.getAllWorkflows(any(), any(Pageable.class), any(), any(), any())).thenReturn(page);

		assertEquals(page, workflowController.listWorkflows(0, 1, "name", "asc", null, null, null, null));
	}

	@Test
	void testListWorkflows_sortByNameDesc() {
		List<WorkflowDTO> workflows = new ArrayList<>();
		WorkflowDTO mockWorkflowdTO = new WorkflowDTO();
		workflows.add(mockWorkflowdTO);
		Page<WorkflowDTO> page = new PageImpl<>(workflows);
		PageRequest expectedPageRequest = PageRequest.of(0, 10, Sort.by("name").descending());

		when(workflowService.getAllWorkflows(any(), any(Pageable.class), any(), any(), any())).thenReturn(page);

		assertEquals(page, workflowController.listWorkflows(0, 1, "name", "desc", null, null, null, null));
	}

	@Test
	void testListWorkflows_sortByEnable() {
		List<WorkflowDTO> workflows = new ArrayList<>();
		WorkflowDTO mockWorkflowdTO = new WorkflowDTO();
		workflows.add(mockWorkflowdTO);
		Page<WorkflowDTO> page = new PageImpl<>(workflows);
		PageRequest expectedPageRequest = PageRequest.of(0, 10, Sort.by("enabled"));

		when(workflowService.getAllWorkflows(any(), any(Pageable.class), any(), any(), any())).thenReturn(page);

		assertEquals(page, workflowController.listWorkflows(0, 1, "enabled", "asc", null, null, null, null));
	}

	@Test
	void testListWorkflows_sortByEnabledDesc() {
		List<WorkflowDTO> workflows = new ArrayList<>();
		WorkflowDTO mockWorkflowdTO = new WorkflowDTO();
		workflows.add(mockWorkflowdTO);
		Page<WorkflowDTO> page = new PageImpl<>(workflows);
		PageRequest expectedPageRequest = PageRequest.of(0, 10, Sort.by("enabled").descending());

		when(workflowService.getAllWorkflows(any(), any(Pageable.class), any(), any(), any())).thenReturn(page);

		assertEquals(page, workflowController.listWorkflows(0, 1, "enabled", "desc", null, null, null, null));
	}

	@Test
  void testGetWorkflow() {
    when(workflowService.getWorkflowById(1L)).thenReturn(mockWorkflow);

    assertEquals(mockWorkflow, workflowController.getWorkflow(1L));
  }

	@Test
  void testGetWorkflowThrowsNotFoundException() {
    when(workflowService.getWorkflowById(1L))
        .thenThrow(new WorkflowNotFoundException(AppConstants.WORKFLOW_ID_NOT_FOUND));

    assertThrows(WorkflowNotFoundException.class, () -> workflowController.getWorkflow(1L));
  }

	@Test
	void testGetWorkflowSteps() {
		WorkflowStepDto workflowStepDto = new WorkflowStepDto();
		Long workflowId = 1L;
		List<WorkflowStepDto> steps = Collections.singletonList(workflowStepDto);

		when(workflowService.getWorkflowSteps(eq(workflowId))).thenReturn(steps);

		List<WorkflowStepDto> result = workflowController.getWorkflowSteps(workflowId);

		assertNotNull(result);
	}

	@Test
  void testGetWorkflowStepsThrowsNotFoundException() {
    when(workflowService.getWorkflowSteps(eq(1L)))
        .thenThrow(new WorkflowStepNotFoundException(AppConstants.WORKFLOW_STEPS_NOT_FOUND));

    assertThrows(
        WorkflowStepNotFoundException.class, () -> workflowController.getWorkflowSteps(1L));
  }

	// @Test
	// void testCreateWorkFlowInstance() {
	// ResponseEntity<Void> response = new ResponseEntity<>(HttpStatus.CREATED);
	//
	// assertEquals(response, workflowController.createWorkFlowInstance(1L, "alias",
	// triggerData));
	// }

	@Test
	void testCreateWorkFlowInstance_WorkflowNotFoundException() throws JsonProcessingException {
		Long workflowId = 1L;

		if (triggerData == null) {
			triggerData = objectMapper.createObjectNode();
		}

		String jsonString = objectMapper.writeValueAsString(triggerData);
		JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

		when(workflowMasterService.createWorkFlowInstance(workflowId, jsonObject))
				.thenThrow(new WorkflowNotFoundException(AppConstants.WORKFLOW_ID_NOT_FOUND));

		assertThrows(WorkflowNotFoundException.class,
				() -> workflowController.createWorkFlowInstance(1L, "alias", triggerData));
	}

	@Test
	void testCreateWorkFlowInstance_WorkflowStepNotFoundException() throws Exception {
		Long workflowId = 1L;
		if (triggerData == null) {
			triggerData = objectMapper.createObjectNode();
		}

		String jsonString = objectMapper.writeValueAsString(triggerData);
		JsonObject jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();

		when(workflowMasterService.createWorkFlowInstance(workflowId, jsonObject))
				.thenThrow(new WorkflowStepNotFoundException(AppConstants.WORKFLOW_STEPS_NOT_FOUND));

		assertThrows(WorkflowStepNotFoundException.class,
				() -> workflowController.createWorkFlowInstance(1L, "alias", triggerData));
	}

	@Test
	void testCreateWorkflowInstance_genericException() {
		Long id = 1L;
		JsonNode triggerData = objectMapper.createObjectNode().put("key", "value");

		doThrow(new RuntimeException("Generic error")).when(workflowMasterService)
				.createWorkFlowInstance(any(Long.class), any(JsonObject.class));

		Exception exception = assertThrows(Exception.class,
				() -> workflowController.createWorkFlowInstance(id, "alias", triggerData));

		assertEquals(AppConstants.INTERNAL_SERVER_ERROR, exception.getMessage());
	}

	@Test
	void testListWorkflowInstances() {
		List<ResponseWorkflowInstanceDTO> instances = new ArrayList<>();
		Page<ResponseWorkflowInstanceDTO> page = new PageImpl<>(instances);

		when(workflowInstanceService.listWorkflowInstances(eq(1L), any(Pageable.class), any())).thenReturn(page);

		assertEquals(page, workflowController.listWorkflowInstances(1L, 0, 10, "null", "asc", instanceFilterDTO));
	}

	@Test
	void testListWorkflowInstancesThrowsWorkflowInstancesNotFoundException() {
		WorkflowInstanceFilterDTO instanceFilterDTO = new WorkflowInstanceFilterDTO();
		instanceFilterDTO.setEndDate(null);
		instanceFilterDTO.setStartDate(null);
		instanceFilterDTO.setStatus(null);
		instanceFilterDTO.setPriority(null);
		instanceFilterDTO.setDeliveryType(null);
		instanceFilterDTO.setDuration(null);
		instanceFilterDTO.setCompletedStart(null);
		instanceFilterDTO.setCompletedEnd(null);
		when(workflowInstanceService.listWorkflowInstances(eq(1L), any(Pageable.class), any()))
				.thenThrow(new WorkflowInstancesNotFoundException(AppConstants.WORKFLOW_INSTANCES_NOT_FOUND));

		assertThrows(WorkflowInstancesNotFoundException.class,
				() -> workflowController.listWorkflowInstances(1L, 0, 10, "null", "asc", instanceFilterDTO));
	}

	@Test
	void testCreateWorkflow() {
		Workflow createdWorkflow = new Workflow();
		createdWorkflow.setId(1L);
		createdWorkflow.setName("Mock Workflow");
		WorkflowDTO mockWorkflowDTO = new WorkflowDTO();

		when(workflowService.createWorkflow(mockWorkflowDTO)).thenReturn(createdWorkflow);

		ResponseEntity<Workflow> response = workflowController.createWorkflow(mockWorkflowDTO);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertEquals(createdWorkflow, response.getBody());
	}

	@Test
	void testUpdateWorkflow() {
		Long workflowId = 1L;
		WorkflowDTO updatedWorkflowDTO = new WorkflowDTO();
		updatedWorkflowDTO.setName("Updated Workflow");

		Workflow updatedWorkflow = new Workflow();
		updatedWorkflow.setId(workflowId);
		updatedWorkflow.setName("Updated Workflow");

		when(workflowService.updateWorkflow(workflowId, updatedWorkflowDTO)).thenReturn(updatedWorkflow);

		ResponseEntity<Workflow> response = workflowController.updateWorkflow(workflowId, updatedWorkflowDTO);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertEquals(updatedWorkflow, response.getBody());
	}

	@Test
	void testUpdateWorkflowThrowsNotFoundException() {
		Long workflowId = 1L;
		WorkflowDTO updatedWorkflowDTO = new WorkflowDTO();
		updatedWorkflowDTO.setName("Updated Workflow");

		when(workflowService.updateWorkflow(workflowId, updatedWorkflowDTO))
				.thenThrow(new WorkflowNotFoundException(AppConstants.WORKFLOW_ID_NOT_FOUND + workflowId));

		assertThrows(WorkflowNotFoundException.class,
				() -> workflowController.updateWorkflow(workflowId, updatedWorkflowDTO));
	}

	@Test
	void testDeleteWorkflow() {
		Long workflowId = 1L;

		ResponseEntity<String> expectedResponse = ResponseEntity
				.ok(String.format(AppConstants.DELETED_WORKFLOW_SUCCESS, workflowId));

		assertEquals(expectedResponse, workflowController.deleteWorkflow(workflowId));
	}

	@Test
	void testDeleteWorkflowThrowsNotFoundException() {
		Long workflowId = 10L;

		doThrow(new WorkflowNotFoundException(AppConstants.WORKFLOW_ID_NOT_FOUND + workflowId)).when(workflowService)
				.deleteWorkflow(workflowId);

		try {
			workflowController.deleteWorkflow(workflowId);
		} catch (WorkflowNotFoundException e) {
			assertEquals("Workflow not found with id 10", e.getMessage());
		}
	}

	@Test
	void testDeleteWorkflowThrowsDataIntegrityViolationException() {
		Long workflowId = 10L;
		doThrow(new DataIntegrityViolationException("Data integrity violation")).when(workflowService)
				.deleteWorkflow(workflowId);

		ResponseEntity<String> actualResponse = workflowController.deleteWorkflow(workflowId);

		assertEquals(HttpStatus.BAD_REQUEST, actualResponse.getStatusCode());
		assertEquals(AppConstants.DELETED_WORKFLOW_ERROR, actualResponse.getBody());
	}

	@Test
	void testListWorkflowInstances_withIdentifier() {

		Page<ResponseWorkflowInstanceDTO> result = workflowController.listWorkflowInstances(1L, 0, 10, "status", "asc",
				instanceFilterDTO);

		assertEquals(null, result);
	}

	@Test
	void testListWorkflowInstances_withoutIdentifier() {
		Page<ResponseWorkflowInstanceDTO> workflowInstances = new PageImpl<>(
				Collections.singletonList(new ResponseWorkflowInstanceDTO()));

		when(workflowInstanceService.listWorkflowInstances(eq(1L), any(PageRequest.class), any()))
				.thenReturn(workflowInstances);

		Page<ResponseWorkflowInstanceDTO> result = workflowController.listWorkflowInstances(1L, 0, 10, "status", "asc",
				instanceFilterDTO);

		assertEquals(workflowInstances, result);
	}
	@ParameterizedTest
	@MethodSource("sortingParameters")
	void testListWorkflowInstances(String sortBy, String direction, PageRequest expectedPageRequest) {
		Page<ResponseWorkflowInstanceDTO> workflowInstances = new PageImpl<>(
				Collections.singletonList(new ResponseWorkflowInstanceDTO()));

		when(workflowInstanceService.listWorkflowInstances(anyLong(), any(), any())).thenReturn(workflowInstances);
		Page<ResponseWorkflowInstanceDTO> result = workflowController.listWorkflowInstances(1L, 0, 10, sortBy,
				direction, instanceFilterDTO);
		assertEquals(workflowInstances, result);
	}

	private static Stream<Arguments> sortingParameters() {
		return Stream.of(Arguments.of("status", "desc", PageRequest.of(0, 10, Sort.by("status").descending())),
				Arguments.of("priority", "asc", PageRequest.of(0, 10, Sort.by("priority"))),
				Arguments.of("priority", "desc", PageRequest.of(0, 10, Sort.by("priority").descending())),
				Arguments.of("duration", "asc", PageRequest.of(0, 10, Sort.by("duration"))),
				Arguments.of("duration", "desc", PageRequest.of(0, 10, Sort.by("duration").descending())),
				Arguments.of("created", "asc", PageRequest.of(0, 10, Sort.by("created"))),
				Arguments.of("created", "desc", PageRequest.of(0, 10, Sort.by("created").descending())),
				Arguments.of("id", "desc", PageRequest.of(0, 10, Sort.by("id").descending())));
	}

	@Test
	void testCreateWorkFlowConfiguration_CreateNew() {
		Long workflowId = 1L;
		WorkflowConfigurationDTO workflowConfigurationDTO = new WorkflowConfigurationDTO();

		when(workflowService.getWorkflowConfigurations(workflowId)).thenReturn(Collections.emptyList());
		when(workflowService.createWorkFlowConfiguration(workflowId, workflowConfigurationDTO))
				.thenReturn(new WorkflowConfiguration());

		ResponseEntity<WorkflowConfiguration> response = workflowController.createWorkFlowConfiguration(workflowId,
				workflowConfigurationDTO);

		assertEquals(HttpStatus.CREATED, response.getStatusCode());
		assertNotNull(response.getBody());
	}

	@Test
	void testCreateWorkFlowConfiguration_UpdateExisting() {
		Long workflowId = 1L;
		WorkflowConfiguration existingConfig = new WorkflowConfiguration();
		existingConfig.setId(1L);
		WorkflowConfigurationDTO workflowConfigurationDTO = new WorkflowConfigurationDTO();
		List<WorkflowConfigurationDTO> listOfWorkflowConfigs = Collections.singletonList(workflowConfigurationDTO);

		when(workflowService.getWorkflowConfigurations(workflowId)).thenReturn(listOfWorkflowConfigs);
		when(workflowService.updateWorkFlowConfiguration(workflowId, workflowConfigurationDTO))
				.thenReturn(existingConfig);

		ResponseEntity<WorkflowConfiguration> response = workflowController.createWorkFlowConfiguration(workflowId,
				workflowConfigurationDTO);

		assertEquals(HttpStatus.OK, response.getStatusCode());
		assertNotNull(response.getBody());
		// Add more assertions based on properties of WorkflowConfiguration
	}

	@Test
	void testCreateWorkFlowConfiguration_WorkflowNotFound() {
		Long workflowId = 1L;

		WorkflowConfigurationDTO workflowConfigurationDTO = new WorkflowConfigurationDTO();

		when(workflowService.getWorkflowConfigurations(workflowId))
				.thenThrow(new WorkflowNotFoundException("Workflow not found"));

		Exception exception = assertThrows(WorkflowInstancesNotFoundException.class, () -> {
			workflowController.createWorkFlowConfiguration(workflowId, workflowConfigurationDTO);
		});

		assertEquals(AppConstants.WORKFLOW_ID_NOT_FOUND + workflowId, exception.getMessage());
	}

	@Test
	void testCreateWorkFlowConfiguration_ApplicationRuntimeException() {
		Long workflowId = 1L;

		WorkflowConfigurationDTO workflowConfigurationDTO = new WorkflowConfigurationDTO();

		when(workflowService.getWorkflowConfigurations(workflowId)).thenThrow(new RuntimeException("Unexpected error"));

		Exception exception = assertThrows(ApplicationRuntimeException.class, () -> {
			workflowController.createWorkFlowConfiguration(workflowId, workflowConfigurationDTO);
		});

		assertEquals(AppConstants.INTERNAL_SERVER_ERROR, exception.getMessage());
	}

	@Test
	void testGetWorkflowConfigurations() {
		Long workflowId = 1L;
		WorkflowConfigurationDTO configDTO = new WorkflowConfigurationDTO();

		when(workflowService.getWorkflowConfigurations(workflowId)).thenReturn(Collections.singletonList(configDTO));

		List<WorkflowConfigurationDTO> result = workflowController.getWorkflowConfigurations(workflowId);

		assertEquals(1, result.size());
		assertEquals(configDTO, result.get(0));
	}

	@Test
	void getWorkflowsStatistics() {
		StatisticsDTO statisticsDTO = new StatisticsDTO();
		when(workflowInstanceService.getWorkflowsStatistics(1L)).thenReturn(statisticsDTO);
		assertEquals(statisticsDTO, workflowController.getWorkflowsStatistics(1L));
	}
}
