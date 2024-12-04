package com.bmg.deliver.serviceimpl.api;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

import com.bmg.deliver.dto.WorkflowDTO;
import com.bmg.deliver.dto.WorkflowInstanceDTO;
import com.bmg.deliver.dto.WorkflowStepConfigurationDTO;
import com.bmg.deliver.dto.WorkflowStepDto;
import com.bmg.deliver.enums.Priority;
import com.bmg.deliver.enums.WorkflowInstanceStatus;
import com.bmg.deliver.enums.WorkflowStepType;
import com.bmg.deliver.exceptions.WorkflowInstanceIdNotFoundException;
import com.bmg.deliver.exceptions.WorkflowNotFoundException;
import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.model.WorkflowInstance;
import com.bmg.deliver.model.WorkflowStep;
import com.bmg.deliver.repository.WorkflowInstanceRepository;
import com.bmg.deliver.repository.WorkflowRepository;
import com.bmg.deliver.repository.WorkflowStepRepository;
import com.bmg.deliver.service.WorkflowInstanceService;
import com.bmg.deliver.service.WorkflowStepConfigurationService;
import com.bmg.deliver.serviceimpl.master.MasterService;
import com.bmg.deliver.utils.AppConstants;
import com.nimbusds.jose.shaded.gson.JsonObject;

import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class WorkflowServiceImplTest {

	@Mock
	private WorkflowInstanceService workflowInstanceService;

	@Mock
	private MasterService masterService;

	@Mock
	private WorkflowRepository workflowRepository;

	@Mock
	private WorkflowStepRepository workflowStepRepository;

	@Mock
	private WorkflowInstanceRepository workflowInstanceRepository;

	@Mock
	private WorkflowStepConfigurationService workflowStepConfigurationService;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@InjectMocks
	private WorkflowServiceImpl workflowService;

	private Workflow workflow;
	private WorkflowDTO workflowDTO;
	private WorkflowInstance workflowInstance;
	private WorkflowStepDto workflowStepDto;
	private WorkflowStep workflowStep;

	@BeforeEach
	void setUp() {
		workflow = new Workflow();
		workflow.setId(1L);
		workflow.setName("Test Workflow");
		workflow.setDescription("Test Description");
		workflow.setEnabled(true);
		workflow.setTaskChainIsValid(true);

		workflowDTO = new WorkflowDTO();
		workflowDTO.setId(1L);
		workflowDTO.setName("Test Workflow");
		workflowDTO.setDescription("Test Description");
		workflowDTO.setEnabled(true);
		workflowDTO.setIsTaskChainIsValid(true);

		workflowInstance = new WorkflowInstance();
		workflowInstance.setId(1L);
		workflowInstance.setCreated(new Date());

		workflowStepDto = new WorkflowStepDto();
		workflowStepDto.setId(1L);
		workflowStepDto.setName("Test Step");
		workflowStepDto.setExecutionOrder(1);
		workflowStepDto.setWorkflowStepConfigurations(new ArrayList<>());

		workflowStep = new WorkflowStep();
		workflowStep.setId(1L);
		workflowStep.setName("Old Name");
		workflowStep.setExecutionOrder(2);
		workflowStep.setWorkflow(new Workflow());
		workflowStep.setCreated(new Date());
		workflowStep.setModified(new Date());
	}

	@Test
  void testGetWorkflowById() {
    when(workflowRepository.findById(1L)).thenReturn(Optional.of(workflow));

    Workflow result = workflowService.getWorkflowById(1L);

    assertNotNull(result);
    assertEquals("Test Workflow", result.getName());
  }

	@Test
  void testGetWorkflowById_NotFound() {
    when(workflowRepository.findById(anyLong())).thenReturn(Optional.empty());

    assertThrows(WorkflowNotFoundException.class, () -> workflowService.getWorkflowById(1L));
  }

	// @Test
	// void testGetAllWorkflows() {
	// List<Workflow> workflows = new ArrayList<>();
	// workflows.add(workflow);
	// Page<Workflow> workflowPage = new PageImpl<>(workflows);
	//
	// when(workflowRepository.findAll(any(Pageable.class))).thenReturn(workflowPage);
	//
	// Page<WorkflowDTO> result = workflowService.getAllWorkflows("",
	// Pageable.unpaged(),true,null,null);
	//
	// assertNotNull(result);
	// assertEquals(1, result.getContent().size());
	// assertEquals("Test Workflow", result.getContent().get(0).getName());
	// }

	@Test
	void testGetWorkflowSteps() {
		Long workflowId = 1L;
		Workflow workflow = new Workflow();
		workflow.setId(workflowId);

		WorkflowStep step1 = new WorkflowStep();
		step1.setId(1L);
		step1.setWorkflow(workflow);
		step1.setExecutionOrder(1);
		step1.setType(WorkflowStepType.DDEX);
		step1.setName("DDEX");
		step1.setCreated(new Date());
		step1.setModified(new Date());

		WorkflowStep step2 = new WorkflowStep();
		step2.setId(2L);
		step2.setWorkflow(workflow);
		step2.setExecutionOrder(2);
		step2.setType(WorkflowStepType.GCS_UPLOADER);
		step2.setName("GCS");
		step2.setCreated(new Date());
		step2.setModified(new Date());

		List<WorkflowStep> workflowSteps = List.of(step1, step2);

		WorkflowStepConfigurationDTO configurationDto = new WorkflowStepConfigurationDTO();
		configurationDto.setKey("configKey");
		configurationDto.setValue("configValue");

		when(workflowStepRepository.findByWorkflowIdOrderByExecutionOrder(workflowId)).thenReturn(workflowSteps);
		when(workflowStepConfigurationService.getWorkflowStepConfigurationDtoByWorkflowStepId(step1.getId()))
				.thenReturn(List.of(configurationDto));
		when(workflowStepConfigurationService.getWorkflowStepConfigurationDtoByWorkflowStepId(step2.getId()))
				.thenReturn(List.of(configurationDto));

		// Act
		List<WorkflowStepDto> result = workflowService.getWorkflowSteps(workflowId);

		// Assert
		assertEquals(2, result.size());

		WorkflowStepDto resultStep1 = result.get(0);
		assertEquals(step1.getId(), resultStep1.getId());
		assertEquals(step1.getWorkflow().getId(), resultStep1.getWorkflowId());

		WorkflowStepDto resultStep2 = result.get(1);
		assertEquals(step2.getId(), resultStep2.getId());
		assertEquals(step2.getWorkflow().getId(), resultStep2.getWorkflowId());
		assertEquals(1, resultStep2.getWorkflowStepConfigurations().size());
	}

	@Test
  void testCreateWorkflow() {
    when(workflowRepository.save(any(Workflow.class))).thenReturn(workflow);

    Workflow result = workflowService.createWorkflow(workflowDTO);

    assertNotNull(result);
    assertEquals("Test Workflow", result.getName());
  }

	@Test
  void testUpdateWorkflow() {
    when(workflowRepository.findById(1L)).thenReturn(Optional.of(workflow));
    when(workflowRepository.save(any(Workflow.class))).thenReturn(workflow);

    WorkflowDTO updatedDTO = new WorkflowDTO();
    updatedDTO.setName("Updated Workflow");
	updatedDTO.setDescription("Updated Description");
	updatedDTO.setThrottleLimit(1);


    Workflow result = workflowService.updateWorkflow(1L, updatedDTO);

    assertNotNull(result);
    assertEquals("Updated Workflow", result.getName());
	assertEquals("Updated Description", result.getDescription());
	assertEquals(1, result.getThrottleLimit());

    when(workflowRepository.findById(1L)).thenReturn(Optional.empty());
    assertThrows(WorkflowNotFoundException.class, () -> workflowService.deleteWorkflow(1L));
  }

	@Test
  void testUpdateWorkflow_WorkflowNotFound() {
	when(workflowRepository.findById(anyLong())).thenReturn(Optional.empty());

	Exception exception = assertThrows(WorkflowNotFoundException.class, () -> {
	  workflowService.updateWorkflow(1L, workflowDTO);
	});

	String expectedMessage = "Workflow not found with id ";
	String actualMessage = exception.getMessage();

	assertTrue(actualMessage.contains(expectedMessage));
  }

	@Test
	void testUpdateWorkflowInstanceStatus() {
		Long id = 1L;
		WorkflowInstanceDTO workflowInstanceDTO = new WorkflowInstanceDTO();
		WorkflowInstanceStatus status = WorkflowInstanceStatus.TERMINATED;
		workflowInstanceDTO.setStatus(status);
		workflowInstanceDTO.setPriority(Priority.HIGH);
		WorkflowInstance workflowInstance = new WorkflowInstance();
		workflowInstance.setId(id);
		workflowInstance.setStatus(status);
		when(workflowInstanceRepository.findById(id)).thenReturn(Optional.of(workflowInstance));
		when(workflowInstanceRepository.save(workflowInstance)).thenReturn(workflowInstance);

		WorkflowInstance result = workflowService.updateWorkflowInstance(id, workflowInstanceDTO);

		assertEquals(workflowInstance, result);
		assertEquals(id, result.getId());
		assertEquals(status, result.getStatus());
		verify(workflowInstanceRepository, times(1)).findById(id);
		verify(workflowInstanceRepository, times(1)).save(workflowInstance);
	}

	@Test
	void testUpdateWorkflowInstanceStatus_ThrowsException() {
		Long id = 1L;
		WorkflowInstanceDTO workflowInstanceDTO = new WorkflowInstanceDTO();
		WorkflowInstanceStatus status = WorkflowInstanceStatus.TERMINATED;
		workflowInstanceDTO.setStatus(status);
		when(workflowInstanceRepository.findById(id)).thenReturn(Optional.empty());

		Exception exception = assertThrows(WorkflowInstanceIdNotFoundException.class,
				() -> workflowService.updateWorkflowInstance(id, workflowInstanceDTO));

		assertEquals(AppConstants.WORKFLOW_INSTANCE_ID_NOT_FOUND + id, exception.getMessage());
		verify(workflowInstanceRepository, times(1)).findById(id);
	}

	// @Test
	// void testDeleteWorkflow() {
	// when(workflowRepository.findById(1L)).thenReturn(Optional.of(workflow));
	//
	// workflowService.deleteWorkflow(1L);
	//
	// verify(workflowInstanceRepository, times(1)).deleteAllByWorkflowId(1L);
	// verify(workflowStepRepository, times(1)).deleteAllByWorkflowId(1L);
	// verify(workflowRepository, times(1)).deleteById(1L);
	//
	// when(workflowRepository.findById(1L)).thenReturn(Optional.empty());
	// assertThrows(WorkflowNotFoundException.class, () ->
	// workflowService.deleteWorkflow(1L));
	// }

	@Test
	void testCreateWorkFlowInstance() {
		JsonObject triggerData = new JsonObject();
		triggerData.addProperty("key", "value");

		when(workflowRepository.findById(1L)).thenReturn(Optional.of(workflow));
		when(workflowInstanceService.createWorkFlowInstance(any(JsonObject.class), any(Workflow.class)))
				.thenReturn(workflowInstance);

		WorkflowInstance createdInstance = workflowService.createWorkFlowInstance(1L, triggerData);
		verify(workflowInstanceService, times(1)).createWorkFlowInstance(triggerData, workflow);
		verify(masterService, times(1)).processOnApi(workflowInstance);

		assertEquals(workflowInstance.getId(), createdInstance.getId());
	}

	@Test
	void testCreateWorkFlowInstance_WorkflowNotFound() {
		JsonObject triggerData = new JsonObject();
		triggerData.addProperty("key", "value");

		when(workflowRepository.findById(anyLong())).thenReturn(Optional.empty());

		try {
			workflowService.createWorkFlowInstance(1L, triggerData);
		} catch (WorkflowNotFoundException e) {
			assertEquals("Workflow not found with id ", e.getMessage());
		}

		verify(workflowRepository, times(1)).findById(1L);
		verify(workflowInstanceService, never()).createWorkFlowInstance(any(JsonObject.class), any(Workflow.class));
		verify(masterService, never()).processOnApi(any(WorkflowInstance.class));
	}

	@Test
	void testGetWorkflowsByWorkflowName() {
		List<Workflow> workflows = new ArrayList<>();
		workflows.add(workflow);
		Page<Workflow> workflowPage = new PageImpl<>(workflows);

		when(workflowRepository.findByName("Test", Pageable.unpaged())).thenReturn(workflowPage);

		Page<WorkflowDTO> result = workflowService.getWorkflowsByWorkflowName("Test", Pageable.unpaged());

		assertNotNull(result);
		assertEquals(1, result.getContent().size());
		assertEquals("Test Workflow", result.getContent().get(0).getName());
	}

}
