package com.bmg.deliver.serviceimpl.worker;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

import com.bmg.deliver.dto.WorkflowInstanceMessageDTO;
import com.bmg.deliver.enums.Priority;
import com.bmg.deliver.enums.WorkflowInstanceStatus;
import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.model.WorkflowInstance;
import com.bmg.deliver.rabbitmq.worker.WorkerProducer;
import com.bmg.deliver.repository.WorkflowInstanceArtifactRepository;
import com.bmg.deliver.repository.WorkflowInstanceRepository;
import com.bmg.deliver.repository.WorkflowStepConfigurationRepository;
import com.bmg.deliver.repository.WorkflowStepRepository;
import com.bmg.deliver.service.WorkflowInstanceService;
import com.bmg.deliver.service.WorkflowService;
import com.bmg.deliver.workflow.execution.ExecutionContext;
import com.bmg.deliver.workflow.step.StepFactory;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;

class WorkerServiceTest {

	@Mock
	private WorkerProducer workerProducer;
	@Mock
	private WorkflowInstanceService workflowInstanceService;
	@Mock
	private WorkflowInstanceRepository workflowInstanceRepository;

	@Mock
	private WorkflowStepRepository workflowStepRepository;

	@Mock
	private WorkflowInstanceArtifactRepository workflowInstanceArtifactRepository;

	@Mock
	private WorkflowStepConfigurationRepository workflowStepConfigurationRepository;

	@Mock
	private StepFactory stepFactory;

	@Mock
	private WorkflowService workflowService;

	@Mock
	private SimpMessagingTemplate messagingTemplate;
	@Mock
	private JmsTemplate jmsTemplate;

	private WorkflowInstance workflowInstance;

	@Mock
	private WorkflowInstanceMessageDTO workflowInstanceMessageDTO;

	@InjectMocks
	private WorkerService workerService;

	@BeforeEach
	void setUp() {

		MockitoAnnotations.openMocks(this);

		workflowInstance = new WorkflowInstance();
		workflowInstance.setId(1L);
		Workflow workflow = new Workflow();
		workflow.setId(1L);
		workflowInstance.setWorkflow(workflow);
		workflowInstance.setPriority(Priority.HIGH);

		workflowInstanceMessageDTO = new WorkflowInstanceMessageDTO();
		workflowInstanceMessageDTO.setId(1L);
		workflowInstanceMessageDTO.setWorkflowId(1L);
		workflowInstanceMessageDTO.setPriority(Priority.HIGH);
	}

	@Test
	void processWorkflowInstanceResult_SendsMessageSuccessfully() throws Exception {
		// Arrange
		WorkflowInstance workflowInstance = new WorkflowInstance();
		workflowInstance.setId(1L);
		workflowInstance.setStatus(WorkflowInstanceStatus.COMPLETED);
		Workflow workflow = new Workflow();
		workflow.setId(1L);
		workflowInstance.setWorkflow(workflow);

		doNothing().when(workerProducer).sendMessage(anyString());

		// Act
		workerService.processWorkflowInstanceResult(workflowInstance);

		// Assert
		verify(workerProducer, times(1)).sendMessage(anyString());
	}

	@Test
	void processWorkflowInstanceResult_LogsErrorIfExceptionOccurs() throws Exception {
		// Arrange
		WorkflowInstance workflowInstance = new WorkflowInstance();
		workflowInstance.setId(1L);
		workflowInstance.setStatus(WorkflowInstanceStatus.COMPLETED);
		Workflow workflow = new Workflow();
		workflow.setId(1L);
		workflowInstance.setWorkflow(workflow);

		doThrow(new RuntimeException("Failed to send message")).when(workerProducer).sendMessage(anyString());

		// Act
		workerService.processWorkflowInstanceResult(workflowInstance);

		// Assert
		verify(workerProducer, times(1)).sendMessage(anyString());
	}

	// @Test
	// void testGetStepsForWorkflowInstance() {
	// when(workflowInstanceRepository.findById(1L)).thenReturn(Optional.of(workflowInstance));
	// List<WorkflowStep> workflowSteps = Arrays.asList(new WorkflowStep(), new
	// WorkflowStep());
	// when(workflowStepRepository.findByWorkflowIdOrderByExecutionOrder(eq(1L)))
	// .thenReturn(workflowSteps);
	// when(workflowStepConfigurationRepository.findByWorkflowStepId(anyLong()))
	// .thenReturn(new ArrayList<>());
	// StepParams stepParams = new StepParams(1L, workflowInstance.getWorkflow(), 1,
	// "Test Step", null, new ArrayList<>());
	// when(stepFactory.createStep(stepParams))
	// .thenReturn(mock(Step.class));
	//
	// ExecutionContext context = new ExecutionContext(workflowInstance,
	// "attachmentsDir");
	// List<Step> steps = workerService.getStepsForWorkflowInstance(context);
	//
	// assertEquals(2, steps.size());
	// }

	// @Test
	// void testUpdateWorkflowInstanceStatus() {
	// WorkflowInstanceArtifact workflowInstanceArtifact = new
	// WorkflowInstanceArtifact();
	// Set<WorkflowInstanceArtifact> artifactsForStep = new HashSet<>();
	// artifactsForStep.add(workflowInstanceArtifact);
	//
	// List<WorkflowStep> workflowSteps = new ArrayList<>();
	// WorkflowStep workflowStep1 = new WorkflowStep();
	// workflowStep1.setId(1L);
	//
	// WorkflowStep workflowStep2 = new WorkflowStep();
	// workflowStep2.setId(1L);
	//
	// workflowSteps.add(workflowStep1);
	// workflowSteps.add(workflowStep2);
	//
	// Map<Long, Set<WorkflowInstanceArtifact>> artifactMap = new HashMap<>();
	// artifactMap.put(1L, artifactsForStep);
	// ExecutionContext context = new ExecutionContext(workflowInstance,
	// "attachmentsDir");
	// context.setArtifactMap(artifactMap);
	//
	// StepResult result = new StepResult(true, "abc");
	// workflowInstance.setStatus(WorkflowInstanceStatus.COMPLETED);
	//
	// when(workflowInstanceRepository.findById(1L)).thenReturn(Optional.of(workflowInstance));
	// when(workflowStepRepository.findByWorkflowIdOrderByExecutionOrder(eq(1L))).thenReturn(workflowSteps);
	//
	// workerService.updateWorkflowInstanceStatus(context, result, 1000L);
	//
	// assertEquals(WorkflowInstanceStatus.COMPLETED, workflowInstance.getStatus());
	// assertEquals(1000L, workflowInstance.getDuration());
	// assertNotNull(workflowInstance.getCompleted());
	//
	// verify(workflowInstanceArtifactRepository, times(1)).saveAll(anyList());
	// verify(workflowInstanceRepository, times(1)).save(workflowInstance);
	// verify(workerProducer, times(1)).sendMessage(anyString());
	// }

	// @Test
	// void testBuildExecutionContext() {
	// WorkflowInstanceMessageDTO dto = new WorkflowInstanceMessageDTO();
	// dto.setId(1L);
	//
	// WorkflowInstance workflowInstance = new WorkflowInstance();
	// workflowInstance.setId(1L);
	// workflowInstance.setStatus(WorkflowInstanceStatus.PENDING);
	//
	// when(workflowInstanceService.getWorkflowInstanceById(dto.getId())).thenReturn(Optional.of(workflowInstance));
	//
	// workerService.setAttachmentsDir("");
	// ExecutionContext context = workerService.buildExecutionContext(dto);
	//
	// assertNotNull(context);
	// assertEquals(WorkflowInstanceStatus.RUNNING, workflowInstance.getStatus());
	// verify(workflowInstanceRepository, times(1)).save(workflowInstance);
	// }

	@Test
	void testBuildExecutionContextWhenNotPresent() {
		WorkflowInstanceMessageDTO dto = new WorkflowInstanceMessageDTO();
		dto.setId(1L);

		when(workflowInstanceService.getWorkflowInstanceById(dto.getId())).thenReturn(Optional.empty());

		ExecutionContext context = workerService.buildExecutionContext(dto);

		assertNull(context);
	}
}
