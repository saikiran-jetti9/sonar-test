package com.bmg.deliver.controller;

import com.bmg.deliver.model.WorkflowInstance;
import org.springframework.data.domain.Page;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WebSocketController {

	@MessageMapping("/send-message")
	@SendTo("/topic/messages")
	public String sendMessage(String message) {
		return message;
	}

	@MessageMapping("/workflow-instances")
	@SendTo("/topic/workflow-instances")
	public Page<WorkflowInstance> sendWorkflowInstances(Page<WorkflowInstance> workflowInstances) {
		return workflowInstances;
	}
}
