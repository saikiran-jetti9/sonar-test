package com.bmg.deliver.serviceimpl.master;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.bmg.deliver.dto.WorkflowConfigurationDTO;
import com.bmg.deliver.exceptions.WorkflowNotFoundException;
import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.model.WorkflowConfiguration;
import com.bmg.deliver.repository.WorkflowConfigurationRepository;
import com.bmg.deliver.repository.WorkflowRepository;
import com.bmg.deliver.service.WorkflowInstanceService;
import com.bmg.deliver.serviceimpl.api.WorkflowServiceImpl;
import com.bmg.deliver.utils.AppConstants;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import com.nimbusds.jose.shaded.gson.JsonSyntaxException;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class WorkflowServiceImplTest {
	@Mock
	private MasterService masterService;

	@Mock
	private WorkflowInstanceService workflowInstanceService;
	@Mock
	private WorkflowConfigurationRepository workflowConfigurationRepository;
	private WorkflowConfigurationDTO workflowConfigurationDTO;

	@Mock
	private WorkflowRepository workflowRepository;
	private JsonNode triggerData;
	@InjectMocks
	private WorkflowServiceImpl workflowMasterService;

	private final ObjectMapper objectMapper = new ObjectMapper();

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		workflowConfigurationDTO = new WorkflowConfigurationDTO();
		workflowConfigurationDTO.setKey("testKey");
		workflowConfigurationDTO.setValue("testValue");
	}

	// @Test
	// void createWorkflowInstance() {
	// Long workflowId = 1L;
	// Workflow workflow = new Workflow();
	// workflow.setId(workflowId);
	// WorkflowInstanceDTO workflowInstanceDTO = new WorkflowInstanceDTO();
	//
	// when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(workflow));
	// when(workflowInstanceService.createWorkFlowInstance(any(), any()))
	// .thenReturn(new WorkflowInstance());
	//
	// ResponseEntity<WorkflowInstanceDTO> responseEntity =
	// workflowMasterService.createWorkFlowInstance(workflowId,
	// workflowInstanceDTO);
	//
	// assertEquals(HttpStatus.CREATED, responseEntity.getStatusCode());
	// verify(masterService, times(1)).pushToWorkQueue(any());
	// }

	@Test
	void createWorkflowInstanceWorkflowNotFoundException() throws JsonProcessingException {
		if (triggerData == null) {
			triggerData = objectMapper.createObjectNode();
		}
		String jsonString = objectMapper.writeValueAsString(triggerData);
		JsonObject jsonObject;
		try {
			jsonObject = JsonParser.parseString(jsonString).getAsJsonObject();
		} catch (IllegalStateException | JsonSyntaxException e) {
			throw new IllegalArgumentException("Invalid JSON string: " + jsonString, e);
		}
		Long workflowId = 1L;
		when(workflowRepository.findById(workflowId)).thenReturn(Optional.empty());
		assertThrows(WorkflowNotFoundException.class,
				() -> workflowMasterService.createWorkFlowInstance(workflowId, jsonObject));
		verify(masterService, never()).pushToWorkQueue(any());
	}

	@Test
	void testGetWorkflowById() {
		Long workflowId = 1L;
		Workflow workflow = new Workflow();
		workflow.setId(workflowId);

		when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(workflow));

		Workflow result = workflowMasterService.getWorkflowById(workflowId);

		assertEquals(workflow, result);
	}

	@Test
	void testCreateWorkFlowConfiguration() {
		Long workflowId = 1L;
		Workflow mworkflow = new Workflow();
		mworkflow.setId(1L);
		WorkflowConfiguration workflowConfiguration = new WorkflowConfiguration();
		workflowConfiguration.setId(1L);
		workflowConfiguration.setKey(workflowConfigurationDTO.getKey());
		workflowConfiguration.setValue(workflowConfigurationDTO.getValue());
		workflowConfiguration.setWorkflow(mworkflow);

		Workflow mockWorkflow = new Workflow();
		mockWorkflow.setId(workflowId);

		when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(mockWorkflow));
		workflowMasterService.getWorkflowById(workflowId);

		when(workflowConfigurationRepository.save(any(WorkflowConfiguration.class))).thenReturn(workflowConfiguration);

		WorkflowConfiguration result = workflowMasterService.createWorkFlowConfiguration(workflowId,
				workflowConfigurationDTO);

		assertNotNull(result);
		assertEquals(workflowConfigurationDTO.getKey(), result.getKey());
		assertEquals(workflowConfigurationDTO.getValue(), result.getValue());
		verify(workflowConfigurationRepository).save(any(WorkflowConfiguration.class));
	}

	@Test
	void testUpdateWorkFlowConfiguration() {
		Long workflowId = 1L;
		WorkflowConfiguration existingConfig = new WorkflowConfiguration();
		existingConfig.setId(1L);
		existingConfig.setKey("testKey");
		existingConfig.setValue("oldValue");

		Workflow mockWorkflow = new Workflow();
		mockWorkflow.setId(workflowId);

		when(workflowRepository.findById(workflowId)).thenReturn(Optional.of(mockWorkflow));
		workflowMasterService.getWorkflowById(workflowId);

		when(workflowConfigurationRepository.findByWorkflowIdAndKey(workflowId, workflowConfigurationDTO.getKey()))
				.thenReturn(Optional.of(existingConfig));

		when(workflowConfigurationRepository.save(any(WorkflowConfiguration.class))).thenReturn(existingConfig);

		WorkflowConfiguration result = workflowMasterService.updateWorkFlowConfiguration(workflowId,
				workflowConfigurationDTO);

		assertNotNull(result);
		assertEquals(workflowConfigurationDTO.getKey(), result.getKey());
		assertEquals(workflowConfigurationDTO.getValue(), result.getValue());
		verify(workflowConfigurationRepository).save(existingConfig);
	}

	@Test
	void testUpdateWorkFlowConfiguration_NotFound() {
		Long workflowId = 1L;

		when(workflowConfigurationRepository.findByWorkflowIdAndKey(workflowId, workflowConfigurationDTO.getKey()))
				.thenReturn(Optional.empty());

		Exception exception = assertThrows(WorkflowNotFoundException.class, () -> {
			workflowMasterService.updateWorkFlowConfiguration(workflowId, workflowConfigurationDTO);
		});

		assertEquals(AppConstants.WORKFLOW_ID_NOT_FOUND + workflowId, exception.getMessage());
	}

	@Test
	void testGetWorkflowConfigurations() {
		Long workflowId = 1L;
		WorkflowConfiguration config = new WorkflowConfiguration();
		config.setId(1L);
		config.setKey("testKey");
		config.setValue("testValue");
		Workflow workflow = new Workflow();
		workflow.setId(workflowId);
		config.setWorkflow(workflow);

		when(workflowConfigurationRepository.findByWorkflowId(workflowId))
				.thenReturn(Collections.singletonList(config));

		List<WorkflowConfigurationDTO> result = workflowMasterService.getWorkflowConfigurations(workflowId);

		assertEquals(1, result.size());
		assertEquals(config.getId(), result.get(0).getId());
		assertEquals(config.getKey(), result.get(0).getKey());
		assertEquals(config.getValue(), result.get(0).getValue());
		assertEquals(workflowId, result.get(0).getWorkflowId());
	}
}
