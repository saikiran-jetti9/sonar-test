package com.bmg.deliver.serviceimpl;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bmg.deliver.dto.DeliveryTypeStatsDTO;
import com.bmg.deliver.dto.StatisticsDTO;
import com.bmg.deliver.dto.responsedto.ResponseWorkflowInstanceDTO;
import com.bmg.deliver.dto.responsedto.WorkflowInstanceFilterDTO;
import com.bmg.deliver.enums.DeliveryType;
import com.bmg.deliver.enums.Priority;
import com.bmg.deliver.enums.WorkflowInstanceStatus;
import com.bmg.deliver.exceptions.WorkflowInstanceIdNotFoundException;
import com.bmg.deliver.exceptions.WorkflowInstancesNotFoundException;
import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.model.WorkflowInstance;
import com.bmg.deliver.repository.WorkflowInstanceRepository;
import com.bmg.deliver.utils.AppConstants;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class WorkflowInstanceServiceImplTest {

	@Mock
	private WorkflowInstanceRepository workflowInstanceRepository;

	@InjectMocks
	private WorkflowInstanceServiceImpl workflowInstanceService;
	@Mock
	private ObjectMapper objectMapper;

	@Mock
	private SimpMessagingTemplate messagingTemplate;
	private Workflow mockWorkflow;
	private JsonObject mockTriggerData;
	private WorkflowInstance mockWorkflowInstance;

	private WorkflowInstanceFilterDTO instanceFilterDTO;

	@BeforeEach
	void setup() {
		MockitoAnnotations.openMocks(this);
		mockWorkflow = new Workflow();
		mockWorkflow.setId(1L);

		String triggerDataJson = "{" + "\"releaseProduct\": {" + "  \"productSummary\": { \"priority\": \"HIGH\" },"
				+ "  \"barcode\": \"1234567890\"" + "}}";
		mockTriggerData = JsonParser.parseString(triggerDataJson).getAsJsonObject();

		mockWorkflowInstance = new WorkflowInstance();
		mockWorkflowInstance.setWorkflow(mockWorkflow);
		mockWorkflowInstance.setPriority(Priority.HIGH);
		mockWorkflowInstance.setStatus(WorkflowInstanceStatus.CREATED);
		mockWorkflowInstance.setIdentifier("1234567890");
		mockWorkflowInstance.setTriggerData(mockTriggerData.toString());

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
  void testCreateWorkFlowInstance_takeDownDeliveryType()  {
    when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
        .thenReturn(mockWorkflowInstance);
	String triggerDataJson = "{\"releaseProduct\":{\"productSummary\":{\"priority\":\"MINOR\"},\"isDataOnlyTrigger\":false,\"deliveryType\":\"Takedown\"}}";

	JsonObject triggerData = JsonParser.parseString(triggerDataJson).getAsJsonObject();
	WorkflowInstance result =
        workflowInstanceService.createWorkFlowInstance(triggerData, mockWorkflow);

	assertNotNull(result);
    verify(workflowInstanceRepository, times(1)).save(any(WorkflowInstance.class));
  }

	@Test
	void testCreateWorkFlowInstance_dataOnlyTrigger()  {
		when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
				.thenReturn(mockWorkflowInstance);
		String triggerDataJson = "{\"releaseProduct\":{\"productSummary\":{\"priority\":\"MINOR\"},\"isDataOnlyTrigger\":false,\"deliveryType\":\"Data only trigger\"}}";

		JsonObject triggerData = JsonParser.parseString(triggerDataJson).getAsJsonObject();
		WorkflowInstance result =
				workflowInstanceService.createWorkFlowInstance(triggerData, mockWorkflow);

		assertNotNull(result);
		verify(workflowInstanceRepository, times(1)).save(any(WorkflowInstance.class));
	}

	@Test
	void testCreateWorkFlowInstance_packshot()  {
		when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
				.thenReturn(mockWorkflowInstance);
		String triggerDataJson = "{\"releaseProduct\":{\"productSummary\":{\"priority\":\"MINOR\"},\"isDataOnlyTrigger\":false,\"deliveryType\":\"Packshot\"}}";

		JsonObject triggerData = JsonParser.parseString(triggerDataJson).getAsJsonObject();
		WorkflowInstance result =
				workflowInstanceService.createWorkFlowInstance(triggerData, mockWorkflow);

		assertNotNull(result);
		verify(workflowInstanceRepository, times(1)).save(any(WorkflowInstance.class));
	}

	@Test
	void testCreateWorkFlowInstance_fulltrigger()  {
		when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
				.thenReturn(mockWorkflowInstance);
		String triggerDataJson = "{\"releaseProduct\":{\"productSummary\":{\"priority\":\"MINOR\"},\"isDataOnlyTrigger\":false,\"deliveryType\":\"Full trigger\"}}";

		JsonObject triggerData = JsonParser.parseString(triggerDataJson).getAsJsonObject();
		WorkflowInstance result =
				workflowInstanceService.createWorkFlowInstance(triggerData, mockWorkflow);

		assertNotNull(result);
		verify(workflowInstanceRepository, times(1)).save(any(WorkflowInstance.class));
	}

	@Test
	void testCreateWorkFlowInstance_screengrab()  {
		when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
				.thenReturn(mockWorkflowInstance);
		String triggerDataJson = "{\"releaseProduct\":{\"productSummary\":{\"priority\":\"MINOR\"},\"isDataOnlyTrigger\":false,\"deliveryType\":\"Screengrab\"}}";

		JsonObject triggerData = JsonParser.parseString(triggerDataJson).getAsJsonObject();
		WorkflowInstance result =
				workflowInstanceService.createWorkFlowInstance(triggerData, mockWorkflow);

		assertNotNull(result);
		verify(workflowInstanceRepository, times(1)).save(any(WorkflowInstance.class));
	}

	@Test
	void testCreateWorkFlowInstance_coverArt()  {
		when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
				.thenReturn(mockWorkflowInstance);
		String triggerDataJson = "{\"releaseProduct\":{\"productSummary\":{\"priority\":\"MINOR\"},\"isDataOnlyTrigger\":false,\"deliveryType\":\"Cover Art\"}}";

		JsonObject triggerData = JsonParser.parseString(triggerDataJson).getAsJsonObject();
		WorkflowInstance result =
				workflowInstanceService.createWorkFlowInstance(triggerData, mockWorkflow);

		assertNotNull(result);
		verify(workflowInstanceRepository, times(1)).save(any(WorkflowInstance.class));
	}

	@Test
	void testCreateWorkFlowInstance_insert()  {
		when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
				.thenReturn(mockWorkflowInstance);
		String triggerDataJson = "{\"releaseProduct\":{\"productSummary\":{\"priority\":\"MINOR\"},\"isDataOnlyTrigger\":false,\"deliveryType\":\"Insert\"}}";

		JsonObject triggerData = JsonParser.parseString(triggerDataJson).getAsJsonObject();
		WorkflowInstance result =
				workflowInstanceService.createWorkFlowInstance(triggerData, mockWorkflow);

		assertNotNull(result);
		verify(workflowInstanceRepository, times(1)).save(any(WorkflowInstance.class));
	}

	@Test
	void testCreateWorkFlowInstance_withoutDeliveryType()  {
		when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
				.thenReturn(mockWorkflowInstance);
		String triggerDataJson = "{\"releaseProduct\":{\"productSummary\":{\"priority\":\"MINOR\"},\"isDataOnlyTrigger\":false}}";

		JsonObject triggerData = JsonParser.parseString(triggerDataJson).getAsJsonObject();
		WorkflowInstance result =
				workflowInstanceService.createWorkFlowInstance(triggerData, mockWorkflow);

		assertNotNull(result);
		verify(workflowInstanceRepository, times(1)).save(any(WorkflowInstance.class));
	}

	@Test
	void testCreateWorkFlowInstance_withoutDeliveryTypeIsDataOnlyTrigger()  {
		when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
				.thenReturn(mockWorkflowInstance);
		String triggerDataJson = "{\"releaseProduct\":{\"productSummary\":{\"priority\":\"MINOR\"},\"isDataOnlyTrigger\":true}}";

		JsonObject triggerData = JsonParser.parseString(triggerDataJson).getAsJsonObject();
		WorkflowInstance result =
				workflowInstanceService.createWorkFlowInstance(triggerData, mockWorkflow);

		assertNotNull(result);
		verify(workflowInstanceRepository, times(1)).save(any(WorkflowInstance.class));
	}

	@Test
	 void testCreateWorkFlowInstance_InvalidPriority() {
		when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
				.thenReturn(mockWorkflowInstance);
		String triggerDataJson = "{\"releaseProduct\":{\"productSummary\":{\"priority\":\"Invalid\"},\"isDataOnlyTrigger\":true}}";

		JsonObject triggerData = JsonParser.parseString(triggerDataJson).getAsJsonObject();
		WorkflowInstance result =
				workflowInstanceService.createWorkFlowInstance(triggerData, mockWorkflow);
		assertNotNull(result);
	}

	@Test
  void testCreateWorkFlowInstance_throwsException() {
    when(workflowInstanceRepository.save(any(WorkflowInstance.class)))
        .thenThrow(new RuntimeException("Database Error"));

    Exception exception =
        assertThrows(
            RuntimeException.class,
            () -> workflowInstanceService.createWorkFlowInstance(mockTriggerData, mockWorkflow));

    assertEquals("Database Error", exception.getMessage());
    verify(workflowInstanceRepository, times(1)).save(any(WorkflowInstance.class));
  }

	@Test
	void testListWorkflowInstances() {
		Long workflowId = 1L;
		Pageable pageable = mock(Pageable.class);
		WorkflowInstance instance = new WorkflowInstance();
		instance.setId(1L);
		instance.setIdentifier("identifier");
		instance.setWorkflow(new Workflow());
		instance.setStatus(WorkflowInstanceStatus.CREATED);
		instance.setPriority(Priority.HIGH);
		instance.setCreated(new Date());
		instance.setModified(new Date());
		instance.setDeliveryType(DeliveryType.FULL_DELIVERY);

		Page<WorkflowInstance> page = new PageImpl<>(Collections.singletonList(instance));
		when(workflowInstanceRepository.findByWorkflowId(anyLong(), any())).thenReturn(page);
		Page<ResponseWorkflowInstanceDTO> result = workflowInstanceService.listWorkflowInstances(workflowId, pageable,
				instanceFilterDTO);

		assertEquals(1, result.getContent().size());
		ResponseWorkflowInstanceDTO dto = result.getContent().get(0);
		assertEquals(instance.getId(), dto.getId());
	}

	private void invokeServiceAndThrowException() {
		workflowInstanceService.listWorkflowInstances(1L, Pageable.unpaged(), any());
	}

	@Test
	void testGetWorkflowInstanceById_Success() {
		WorkflowInstance instance = new WorkflowInstance();
		when(workflowInstanceRepository.findById(1L)).thenReturn(Optional.of(instance));

		Optional<WorkflowInstance> result = workflowInstanceService.getWorkflowInstanceById(1L);

		assertTrue(result.isPresent());
		assertEquals(instance, result.get());
	}

	@Test
  void testGetWorkflowInstanceById_NotFound() {
    when(workflowInstanceRepository.findById(1L)).thenReturn(Optional.empty());

    assertThrows(
        WorkflowInstanceIdNotFoundException.class,
        () -> {
          workflowInstanceService.getWorkflowInstanceById(1L);
        });
  }

	@Test
	void testGetLogsOfWorkflowInstanceWhenInstanceExists() {
		// Arrange
		Long instanceId = 1L;
		WorkflowInstance workflowInstance = new WorkflowInstance();
		workflowInstance.setId(instanceId);
		workflowInstance.setLog("Sample info message");
		when(workflowInstanceRepository.findById(instanceId)).thenReturn(Optional.of(workflowInstance));

		String logs = workflowInstanceService.getLogsOfWorkflowInstance(instanceId);

		assertEquals("Sample info message", logs);
		verify(workflowInstanceRepository, times(1)).findById(instanceId);
	}

	@Test
	void testGetLogsOfWorkflowInstanceWhenInstanceDoesNotExist() {
		Long instanceId = 1L;
		when(workflowInstanceRepository.findById(instanceId)).thenReturn(Optional.empty());

		assertThrows(WorkflowInstancesNotFoundException.class,
				() -> workflowInstanceService.getLogsOfWorkflowInstance(instanceId));
		verify(workflowInstanceRepository, times(1)).findById(instanceId);
	}

	@Test
	void testGetWorkflowsByIdentifier() {
		String identifier = "test-identifier";
		Pageable pageable = mock(Pageable.class);
		WorkflowInstance instance = new WorkflowInstance();
		instance.setId(1L);
		instance.setIdentifier(identifier);
		instance.setWorkflow(new Workflow());
		instance.setStatus(WorkflowInstanceStatus.CREATED);
		instance.setPriority(Priority.HIGH);
		instance.setCreated(new Date());
		instance.setModified(new Date());
		instance.setDeliveryType(DeliveryType.FULL_DELIVERY);
		Page<WorkflowInstance> page = new PageImpl<>(Collections.singletonList(instance));
		when(workflowInstanceRepository.findByIdentifier(identifier, pageable)).thenReturn(page);

		Page<ResponseWorkflowInstanceDTO> result = workflowInstanceService.getWorkflowsByIdentifier(identifier,
				pageable);

		assertEquals(1, result.getContent().size());
		ResponseWorkflowInstanceDTO dto = result.getContent().get(0);
		assertEquals(instance.getId(), dto.getId());
	}

	// @Test
	// void testGetAllInstances_StatusPending() {
	// Pageable pageable = Pageable.unpaged();
	// List<WorkflowInstanceStatus> statuses =
	// Arrays.asList(WorkflowInstanceStatus.CREATED,
	// WorkflowInstanceStatus.QUEUED);
	// Page<WorkflowInstance> expectedPage = new PageImpl<>(Collections.emptyList(),
	// pageable, 0);
	// when(workflowInstanceRepository.findByStatusIn(pageable,
	// statuses)).thenReturn(expectedPage);
	//
	// WorkflowInstanceStatus status = WorkflowInstanceStatus.PENDING;
	// Page<WorkflowInstance> result =
	// workflowInstanceService.getAllInstances(pageable, status);
	//
	// assertNotNull(result);
	// assertEquals(expectedPage, result);
	// verify(workflowInstanceRepository, times(1)).findByStatusIn(pageable,
	// statuses);
	// }

	// @Test
	// void testGetAllInstances_StatusNotPending() {
	// Pageable pageable = Pageable.unpaged();
	// WorkflowInstanceStatus status = WorkflowInstanceStatus.COMPLETED;
	// List<WorkflowInstanceStatus> statuses = Arrays.asList(status);
	// Page<WorkflowInstance> expectedPage = new PageImpl<>(Collections.emptyList(),
	// pageable, 0);
	// when(workflowInstanceRepository.findByStatusIn(pageable,
	// statuses)).thenReturn(expectedPage);
	//
	// Page<WorkflowInstance> result =
	// workflowInstanceService.getAllInstances(pageable, status);
	//
	// assertNotNull(result);
	// assertEquals(expectedPage, result);
	// verify(workflowInstanceRepository, times(1)).findByStatusIn(pageable,
	// statuses);
	// }

	@Test
	void testDeleteWorkflowInstance() {
		Long id = 1L;
		WorkflowInstance workflowInstance = new WorkflowInstance();
		when(workflowInstanceRepository.findById(id)).thenReturn(Optional.of(workflowInstance));

		workflowInstanceService.deleteWorkflowInstance(id);

		verify(workflowInstanceRepository, times(1)).findById(id);
		verify(workflowInstanceRepository, times(1)).deleteById(id);
	}

	@Test
	void testDeleteWorkflowInstance_ThrowsException() {
		Long id = 1L;
		when(workflowInstanceRepository.findById(id)).thenReturn(Optional.empty());

		Exception exception = assertThrows(WorkflowInstanceIdNotFoundException.class,
				() -> workflowInstanceService.deleteWorkflowInstance(id));

		assertEquals(AppConstants.WORKFLOW_INSTANCE_ID_NOT_FOUND + id, exception.getMessage());
		verify(workflowInstanceRepository, times(1)).findById(id);
	}

	@Test
	void testGetWorkflowsStatistics() {
		Long workflowId = 1L;

		when(workflowInstanceRepository.countByWorkflowIdAndStatus(workflowId, WorkflowInstanceStatus.COMPLETED))
				.thenReturn(5L);
		when(workflowInstanceRepository.countByWorkflowIdAndStatus(workflowId, WorkflowInstanceStatus.FAILED))
				.thenReturn(2L);

		for (DeliveryType deliveryType : DeliveryType.values()) {
			when(workflowInstanceRepository.countByWorkflowIdAndDeliveryTypeAndStatus(workflowId, deliveryType,
					WorkflowInstanceStatus.COMPLETED)).thenReturn(3L);
			when(workflowInstanceRepository.countByWorkflowIdAndDeliveryTypeAndStatus(workflowId, deliveryType,
					WorkflowInstanceStatus.FAILED)).thenReturn(1L);
		}

		StatisticsDTO stats = workflowInstanceService.getWorkflowsStatistics(workflowId);

		// Assertions
		assertEquals(5L, stats.getTotalSuccessfulInstances());
		assertEquals(2L, stats.getTotalFailedInstances());

		Map<String, DeliveryTypeStatsDTO> deliveryStats = stats.getDeliveryTypeStats();
		assertEquals(DeliveryType.values().length, deliveryStats.size());

		for (DeliveryType deliveryType : DeliveryType.values()) {
			DeliveryTypeStatsDTO deliveryStatsDTO = deliveryStats.get(deliveryType.toString());
			assertEquals(3L, deliveryStatsDTO.getSuccessful());
			assertEquals(1L, deliveryStatsDTO.getFailures());
		}
	}
}
