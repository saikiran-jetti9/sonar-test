package com.bmg.deliver.serviceimpl.master;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;
import static org.mockito.Mockito.times;

import com.bmg.deliver.dto.WorkflowDTO;
import com.bmg.deliver.dto.WorkflowInstanceMessageDTO;
import com.bmg.deliver.enums.DeliveryType;
import com.bmg.deliver.enums.Priority;
import com.bmg.deliver.enums.WorkflowInstanceStatus;
import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.model.WorkflowInstance;
import com.bmg.deliver.rabbitmq.master.MasterProducer;
import com.bmg.deliver.repository.SystemPropertiesRepository;
import com.bmg.deliver.repository.WorkflowConfigurationRepository;
import com.bmg.deliver.repository.WorkflowInstanceRepository;
import com.bmg.deliver.repository.WorkflowRepository;
import java.util.*;

import com.bmg.deliver.service.EmailService;
import com.bmg.deliver.service.WorkflowInstanceService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class MasterServiceTest {
	@Mock
	private WorkflowRepository mockWorkflowRepository;
	@Mock
	private MasterProducer masterProducer;

	@Mock
	private SystemPropertiesRepository systemPropertiesRepository;

	@Mock
	private WorkflowInstanceRepository workflowInstanceRepository;

	@Mock
	private WorkflowInstanceService workflowInstanceService;

	@Mock
	private ActiveMqService activeMqService;

	@Mock
	private EmailService emailService;

	@Mock
	private SimpMessagingTemplate messagingTemplate;

	@Mock
	private WorkflowConfigurationRepository workflowConfigurationRepository;

	@InjectMocks
	private MasterService masterService;

	private Workflow workflow;
	private WorkflowInstance workflowInstance;
	private WorkflowInstanceMessageDTO workflowInstanceMessageDTO;

	private WorkflowInstance mockWorkflowInstanceFullDelivery;
	private WorkflowInstance mockWorkflowInstanceDataOnly;

	private final List<Workflow> mockWorkflows = new ArrayList<>();
	private final List<WorkflowInstance> mockWorkflowInstances = new ArrayList<>();

	private List<WorkflowDTO> workflows;
	private Map<Long, List<WorkflowInstance>> queuedInstances;
	private Map<Long, List<WorkflowInstance>> runningInstances;
	private Map<Long, List<WorkflowInstanceMessageDTO>> timeIntervalInstances;

	@BeforeEach
	void setUp() {
		workflow = new Workflow();
		workflow.setId(1L);
		workflow.setThrottleLimit(2);
		workflow.setEnabled(true);
		workflow.setAssetIngestionTime("1m");
		workflow.setDataIngestionTime("2m");

		Workflow mockWorkflow = new Workflow();
		mockWorkflow.setId(1L);
		mockWorkflow.setThrottleLimit(2);
		mockWorkflow.setEnabled(true);
		mockWorkflows.add(mockWorkflow);

		workflowInstance = new WorkflowInstance();
		workflowInstance.setId(1L);
		workflowInstance.setWorkflow(mockWorkflow);
		workflowInstance.setPriority(Priority.HIGH);
		workflowInstance.setStatus(WorkflowInstanceStatus.CREATED);

		WorkflowInstance mockWorkflowInstance = new WorkflowInstance();
		mockWorkflowInstance.setId(1L);
		mockWorkflowInstance.setWorkflow(mockWorkflow);
		mockWorkflowInstance.setPriority(Priority.HIGH);
		mockWorkflowInstance.setStatus(WorkflowInstanceStatus.CREATED);

		WorkflowInstance mockWorkflowInstance2 = new WorkflowInstance();
		mockWorkflowInstance2.setId(2L);
		mockWorkflowInstance2.setWorkflow(mockWorkflow);
		mockWorkflowInstance2.setPriority(Priority.LOW);
		mockWorkflowInstance2.setStatus(WorkflowInstanceStatus.CREATED);

		WorkflowInstance mockWorkflowInstance3 = new WorkflowInstance();
		mockWorkflowInstance3.setId(3L);
		mockWorkflowInstance3.setWorkflow(mockWorkflow);
		mockWorkflowInstance3.setPriority(Priority.HIGH);
		mockWorkflowInstance3.setStatus(WorkflowInstanceStatus.CREATED);

		WorkflowInstance mockWorkflowInstance4 = new WorkflowInstance();
		mockWorkflowInstance4.setId(4L);
		mockWorkflowInstance4.setWorkflow(mockWorkflow);
		mockWorkflowInstance4.setPriority(Priority.LOW);
		mockWorkflowInstance4.setStatus(WorkflowInstanceStatus.CREATED);

		WorkflowInstance mockWorkflowInstance5 = new WorkflowInstance();
		mockWorkflowInstance5.setId(5L);
		mockWorkflowInstance5.setWorkflow(mockWorkflow);
		mockWorkflowInstance5.setPriority(Priority.HIGH);
		mockWorkflowInstance5.setStatus(WorkflowInstanceStatus.CREATED);

		WorkflowInstance workflowInstance6 = new WorkflowInstance();
		workflowInstance6.setId(6L);
		workflowInstance6.setWorkflow(workflow);
		workflowInstance6.getWorkflow().setId(1L);
		workflowInstance6.setDeliveryType(DeliveryType.DATA_ONLY);
		workflowInstance6.setPriority(Priority.HIGH);
		workflowInstance6.setCompleted(new Date());
		workflowInstance6.setIdentifier("test");

		WorkflowInstance workflowInstance7 = new WorkflowInstance();
		workflowInstance7.setId(7L);
		workflowInstance7.setWorkflow(workflow);
		workflowInstance7.getWorkflow().setId(1L);
		workflowInstance7.setDeliveryType(DeliveryType.DATA_ONLY);
		workflowInstance7.setPriority(Priority.HIGH);
		workflowInstance7.setIdentifier("test");

		WorkflowInstance workflowInstance8 = new WorkflowInstance();
		workflowInstance8.setId(8L);
		workflowInstance8.setWorkflow(workflow);
		workflowInstance8.getWorkflow().setId(1L);
		workflowInstance8.setDeliveryType(DeliveryType.FULL_DELIVERY);
		workflowInstance8.setPriority(Priority.HIGH);
		workflowInstance8.setCompleted(null);
		workflowInstance8.setIdentifier("test");

		WorkflowInstance workflowInstance9 = new WorkflowInstance();
		workflowInstance9.setId(9L);
		workflowInstance9.setWorkflow(workflow);
		workflowInstance9.getWorkflow().setId(1L);
		workflowInstance9.setDeliveryType(DeliveryType.FULL_DELIVERY);
		workflowInstance9.setPriority(Priority.HIGH);
		workflowInstance9.setIdentifier("test");

		mockWorkflowInstances.add(mockWorkflowInstance);
		mockWorkflowInstances.add(mockWorkflowInstance2);
		mockWorkflowInstances.add(mockWorkflowInstance3);
		mockWorkflowInstances.add(mockWorkflowInstance4);
		mockWorkflowInstances.add(mockWorkflowInstance5);
		mockWorkflowInstances.add(workflowInstance6);
		mockWorkflowInstances.add(workflowInstance7);
		mockWorkflowInstances.add(workflowInstance8);
		mockWorkflowInstances.add(workflowInstance9);

		queuedInstances = new HashMap<>();
		runningInstances = new HashMap<>();
		timeIntervalInstances = new HashMap<>();
		workflows = new ArrayList<>();
	}

	@Test
	void init() {
		Mockito.when(mockWorkflowRepository.findAll()).thenReturn(mockWorkflows);
		Mockito.when(workflowInstanceRepository.findAllByStatusOrderByPriorityAscIdAsc(Mockito.any(), Mockito.any()))
				.thenReturn(mockWorkflowInstances);

		masterService.init();
		// Mockito.doNothing().when(mockMasterProducer).sendMessage(Mockito.any());
		// Mockito.verify(mockMasterProducer,
		// Mockito.times(2)).sendMessage(Mockito.any());
	}

	// @Test
	// void processOnWorkResult() throws NoSuchFieldException,
	// IllegalAccessException {
	//
	// Field producerField = MasterService.class.getDeclaredField("masterProducer");
	// producerField.setAccessible(true);
	// producerField.set(masterService, masterProducer);
	//
	// WorkflowInstanceMessageDTO messageDTO = new WorkflowInstanceMessageDTO();
	// messageDTO.setId(1L);
	// messageDTO.setWorkflowId(1L);
	// messageDTO.setPriority(Priority.HIGH);
	//
	// WorkflowInstanceMessageDTO messageDTO1 = new WorkflowInstanceMessageDTO();
	// messageDTO1.setId(2L);
	// messageDTO1.setWorkflowId(1L);
	// messageDTO1.setPriority(Priority.HIGH);
	//
	// Field queuedInstancesField =
	// MasterService.class.getDeclaredField("queuedInstances");
	// queuedInstancesField.setAccessible(true);
	// Field runningInstancesField =
	// MasterService.class.getDeclaredField("runningInstances");
	// runningInstancesField.setAccessible(true);
	//
	// Field workflowsField = MasterService.class.getDeclaredField("workflows");
	// workflowsField.setAccessible(true);
	//
	// Map<Long, List<WorkflowInstanceMessageDTO>> queuedInstances = new
	// HashMap<>();
	// queuedInstances.put(1L, new ArrayList<>(List.of(messageDTO1)));
	//
	// Map<Long, List<Long>> runningInstances = new HashMap<>();
	// runningInstances.put(1L, new ArrayList<>(List.of(1L)));
	//
	// queuedInstancesField.set(masterService, queuedInstances);
	// runningInstancesField.set(masterService, runningInstances);
	//
	// MasterService.WorkflowDTO workflowDTO = new MasterService.WorkflowDTO();
	// workflowDTO.setId(1L);
	// workflowDTO.setThrottleLimit(2);
	// workflowDTO.setEnabled(true);
	//
	// Set<MasterService.WorkflowDTO> workflows = new HashSet<>();
	// workflows.add(workflowDTO);
	// workflowsField.set(masterService, workflows);
	//
	// when(workflowInstanceRepository.findById(anyLong())).thenReturn(Optional.ofNullable(workflowInstance));
	//
	// masterService.processOnWorkResult(messageDTO);
	// Map<Long, List<Long>> runningInstancesList = (Map<Long, List<Long>>)
	// runningInstancesField.get(masterService);
	// assertEquals(1, runningInstancesList.size());
	// }

	@Test
	void testUpdateWorkflow_existingWorkflow() {
		Workflow workflow = new Workflow();
		workflow.setId(1L);
		workflow.setEnabled(true);
		workflow.setThrottleLimit(10);

		WorkflowDTO workflowDTO = new WorkflowDTO();
		workflowDTO.setId(1L);
		workflowDTO.setEnabled(true);
		workflowDTO.setThrottleLimit(5);

		workflows.add(workflowDTO);

		masterService.updateWorkflow(workflow);

		assertEquals(true, workflowDTO.getEnabled());
		assertEquals(5, workflowDTO.getThrottleLimit());
	}

	@Test
	void testUpdateWorkflow_nonExistingWorkflow() {
		Workflow workflow = new Workflow();
		workflow.setId(2L);
		workflow.setEnabled(true);
		workflow.setThrottleLimit(10);
		masterService.updateWorkflow(workflow);

		assertEquals(0, workflows.size());
	}

	@Test
	void testAddWorkflow() {
		Workflow workflow = new Workflow();
		workflow.setId(1L);
		workflow.setEnabled(true);
		workflow.setThrottleLimit(10);

		WorkflowDTO workflowDTO = new WorkflowDTO();
		workflowDTO.setId(1L);
		workflowDTO.setEnabled(true);
		workflowDTO.setThrottleLimit(10);

		workflows.add(workflowDTO);
		masterService.addWorkflow(workflow);

		assertEquals(1, workflows.size());
		assertEquals(1L, workflowDTO.getId());
		assertEquals(true, workflowDTO.getEnabled());
		assertEquals(10, workflowDTO.getThrottleLimit());
	}

	@Test
	void testAddWorkflow_existingWorkflow() {
		Workflow workflow = new Workflow();
		workflow.setId(1L);
		workflow.setEnabled(true);
		workflow.setThrottleLimit(10);

		WorkflowDTO workflowDTO = new WorkflowDTO();
		workflowDTO.setId(1L);
		workflowDTO.setEnabled(false);
		workflowDTO.setThrottleLimit(5);

		workflows.add(workflowDTO);
		queuedInstances.put(1L, new ArrayList<>());
		runningInstances.put(1L, new ArrayList<>());
		masterService.addWorkflow(workflow);

		assertEquals(1, workflows.size());
		assertEquals(1, queuedInstances.size());
		assertEquals(1, runningInstances.size());
	}

	@Test
	void testPushToWorkQueue() {
		WorkflowInstanceMessageDTO messageDTO = new WorkflowInstanceMessageDTO();
		messageDTO.setId(1L);
		messageDTO.setWorkflowId(1L);
		messageDTO.setPriority(Priority.HIGH);

		WorkflowInstance workflowInstance = new WorkflowInstance();
		workflowInstance.setId(1L);
		workflowInstance.setStatus(WorkflowInstanceStatus.CREATED);
		workflowInstance.setStatus(WorkflowInstanceStatus.QUEUED);

		Mockito.when(workflowInstanceRepository.findById(1L)).thenReturn(Optional.of(workflowInstance));

		masterService.pushToWorkQueue(messageDTO);

		verify(masterProducer, times(1)).sendMessage(any());
		verify(workflowInstanceRepository, times(1)).save(workflowInstance);

	}

	// @Test
	// void testHandlePausedWorkflowInstancesWhenPaused() {
	// SystemProperties pausedProperty = new SystemProperties();
	// pausedProperty.setValue("true");
	//
	// when(systemPropertiesRepository.findByKey(AppConstants.PAUSED)).thenReturn(Optional.of(pausedProperty));
	//
	// WorkflowDTO workflow1 = new WorkflowDTO();
	// workflow1.setId(1L);
	// workflow1.setEnabled(true);
	//
	// WorkflowDTO workflow2 = new WorkflowDTO();
	// workflow2.setId(2L);
	// workflow2.setEnabled(true);
	//
	// Set<WorkflowDTO> workflows = new HashSet<>();
	// workflows.add(workflow1);
	// workflows.add(workflow2);
	//
	// List<WorkflowInstance> queuedInstances = Arrays.asList(new
	// WorkflowInstance(), new WorkflowInstance());
	// when(workflowInstanceRepository.findByStatusIn(anyList())).thenReturn(queuedInstances);
	// masterService.processOnStartup();
	// verify(workflowInstanceRepository)
	// .findByStatusIn(Arrays.asList(WorkflowInstanceStatus.CREATED,
	// WorkflowInstanceStatus.QUEUED));
	// }

	// @Test
	// void testHandlePausedWorkflowInstancesWhenFalse() {
	// SystemProperties pausedProperty = new SystemProperties();
	// pausedProperty.setValue("false");
	//
	// when(systemPropertiesRepository.findByKey(AppConstants.PAUSED)).thenReturn(Optional.of(pausedProperty));
	//
	// WorkflowDTO workflow1 = new WorkflowDTO();
	// workflow1.setId(1L);
	// workflow1.setEnabled(false);
	//
	// WorkflowDTO workflow2 = new WorkflowDTO();
	// workflow2.setId(2L);
	// workflow2.setEnabled(false);
	//
	// Set<WorkflowDTO> workflows = new HashSet<>();
	// workflows.add(workflow1);
	// workflows.add(workflow2);
	//
	// List<WorkflowInstance> queuedInstances = Arrays.asList(new
	// WorkflowInstance(), new WorkflowInstance());
	// when(workflowInstanceRepository.findByStatusIn(anyList())).thenReturn(queuedInstances);
	// masterService.processOnStartup();
	// verify(workflowInstanceRepository).findByStatusIn(Arrays.asList(WorkflowInstanceStatus.PAUSED));
	// }

	@Test
	void testProcessOnApi_WithQueuedInstance() {
		Workflow workflow = new Workflow();
		workflow.setId(1L);
		workflow.setEnabled(true);
		workflow.setThrottleLimit(2);

		WorkflowInstance workflowInstance = new WorkflowInstance();
		workflowInstance.setId(2L);
		workflowInstance.setWorkflow(workflow);
		workflowInstance.getWorkflow().setId(1L);
		workflowInstance.setDeliveryType(DeliveryType.NONE);
		workflowInstance.setPriority(Priority.HIGH);

		masterService.addWorkflow(workflowInstance.getWorkflow());

		masterService.processOnApi(workflowInstance);

		List<WorkflowInstanceMessageDTO> queuedInstances = masterService.getQueuedInstances().get(1L);
		assertNotNull(queuedInstances);

	}

	@Test
	void testProcessOnApi_WithQueuedInstanceAndNoThrottleLimit() {
		Workflow workflow = new Workflow();
		workflow.setId(1L);
		workflow.setEnabled(true);
		workflow.setThrottleLimit(0);

		WorkflowInstance workflowInstance = new WorkflowInstance();
		workflowInstance.setId(2L);
		workflowInstance.setWorkflow(workflow);
		workflowInstance.getWorkflow().setId(1L);
		workflowInstance.setDeliveryType(DeliveryType.NONE);
		workflowInstance.setPriority(Priority.HIGH);
		workflowInstance.setIdentifier("test");

		masterService.addWorkflow(workflowInstance.getWorkflow());

		masterService.processOnApi(workflowInstance);

		List<WorkflowInstanceMessageDTO> queuedInstances = masterService.getQueuedInstances().get(1L);
		assertNotNull(queuedInstances);

	}

	@Test
	void testProcessOnApi_WithQueuedInstanceAndFullDelivery() {
		masterService.addWorkflow(workflow);
		when(masterService.getLastInstanceByQuery(mockWorkflowInstances.get(8)))
				.thenReturn(Optional.of(mockWorkflowInstances.get(7)));

		masterService.processOnApi(mockWorkflowInstances.get(8));
		List<WorkflowInstanceMessageDTO> queuedInstances = masterService.getQueuedInstances().get(1L);
		assertNotNull(queuedInstances);
	}

	@Test
	void testProcessOnApi_WithQueuedInstanceAndDataOnly() {
		masterService.addWorkflow(workflow);
		when(masterService.getLastInstanceByQuery(mockWorkflowInstances.get(6)))
				.thenReturn(Optional.of(mockWorkflowInstances.get(5)));
		masterService.processOnApi(mockWorkflowInstances.get(6));
		List<WorkflowInstanceMessageDTO> queuedInstances = masterService.getQueuedInstances().get(1L);
		assertNotNull(queuedInstances);
	}

	@Test
	void testInstancesFromTimeIntervalToPushToWorkQueue_EmptyMap() {
		masterService.instancesFromTimeIntervalToPushToWorkQueue();
		verify(workflowInstanceRepository, never()).findById(anyLong());
	}

	@Test
	void testInstancesFromTimeIntervalToPushToWorkQueue_WithWorkflowInstance() {

		WorkflowInstanceMessageDTO messageDTO = new WorkflowInstanceMessageDTO();
		messageDTO.setId(1L);
		messageDTO.setWorkflowId(1L);
		messageDTO.setPriority(Priority.HIGH);
		when(workflowInstanceRepository.findById(1L)).thenReturn(Optional.of(workflowInstance));
		masterService.getTimeIntervalInstances().put(1L, new ArrayList<>(List.of(messageDTO)));
		masterService.getQueuedInstances().put(1L, new ArrayList<>());
		masterService.instancesFromTimeIntervalToPushToWorkQueue();

		assertNotNull(masterService.getTimeIntervalInstances().get(1L));
	}

}
