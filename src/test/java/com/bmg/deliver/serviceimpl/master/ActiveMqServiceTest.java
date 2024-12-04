package com.bmg.deliver.serviceimpl.master;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.Logger;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.web.socket.TextMessage;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ActiveMqServiceTest {

	@Mock
	private JmsTemplate jmsTemplate;

	@Mock
	private Logger logger;

	@InjectMocks
	private ActiveMqService activeMqService;

	@Captor
	private ArgumentCaptor<TextMessage> messageCaptor;

	@Captor
	private ArgumentCaptor<String> stringCaptor;

	@BeforeEach
	public void setUp() {
		// activeMqService = new ActiveMqService(jmsTemplate);

	}

	// @Test
	// void testSendMessage_success() {
	// WorkflowInstance workflowInstance = new WorkflowInstance();
	// workflowInstance.setId(1L);
	//
	// Workflow workflow = new Workflow();
	// workflow.setId(100L);
	// workflowInstance.setWorkflow(workflow);
	//
	// workflowInstance.setStatus(WorkflowInstanceStatus.COMPLETED);
	// workflowInstance.setModified(new Date());
	// workflowInstance.setIdentifier(UUID.randomUUID().toString());
	//
	// activeMqService.sendMessage(workflowInstance);
	//
	// verify(jmsTemplate, times(1)).convertAndSend(stringCaptor.capture(),
	// messageCaptor.capture(), any());
	// String sentTopic = stringCaptor.getValue();
	// TextMessage sentMessage = messageCaptor.getValue();
	//
	// assertNotNull(sentMessage);
	// assertTrue(sentMessage.getPayload().contains("\"processInstanceId\":1"));
	// assertTrue(sentMessage.getPayload().contains("\"processId\":100"));
	// assertTrue(sentMessage.getPayload().contains("\"status\":\"COMPLETED\""));
	// }

	// @Test
	// void testSendMessage_exception() {
	// WorkflowInstance workflowInstance = new WorkflowInstance();
	// workflowInstance.setId(1L);
	// workflowInstance.setWorkflow(new Workflow());
	// workflowInstance.setStatus(WorkflowInstanceStatus.COMPLETED);
	// workflowInstance.setModified(new Date());
	// workflowInstance.setIdentifier(UUID.randomUUID().toString());
	//
	// doThrow(new RuntimeException("Test
	// Exception")).when(jmsTemplate).convertAndSend(eq("your-topic-name"),
	// any(TextMessage.class), any());
	//
	// activeMqService.sendMessage(workflowInstance);
	// assertTrue(true);
	// }

}
