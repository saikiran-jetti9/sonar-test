package com.bmg.deliver.serviceimpl.master;

import com.bmg.deliver.config.MasterCondition;
import com.bmg.deliver.model.WorkflowInstance;
import com.google.gson.JsonObject;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

@Service
@Slf4j
@Conditional(MasterCondition.class)
public class ActiveMqService {
	@Value("${spring.activemq.topic}")
	private String topic;

	private final JmsTemplate jmsTemplate;

	public ActiveMqService(JmsTemplate jmsTemplate) {
		this.jmsTemplate = jmsTemplate;
	}

	public void sendMessage(WorkflowInstance workflowInstance) {
		try {
			JsonObject instanceObject = new JsonObject();
			instanceObject.addProperty("processInstanceId", workflowInstance.getId());
			instanceObject.addProperty("processId", workflowInstance.getWorkflow().getId());
			instanceObject.addProperty("status", workflowInstance.getStatus().toString());
			instanceObject.addProperty("completionDate", workflowInstance.getModified().toString());
			instanceObject.addProperty("identifier", workflowInstance.getIdentifier());

			String messageContent = instanceObject.toString();

			log.info("Sending AMQ message to {}: {}", topic, instanceObject);
			jmsTemplate.convertAndSend(topic, messageContent, message1 -> {
				message1.setStringProperty("SUPPLY_CHAIN_MESSAGE_TYPE", "PROCESS_INSTANCE_FINISHED");
				return message1;
			});
		} catch (Exception e) {
			log.error("Error sending message to ActiveMQ {}", ExceptionUtils.getStackTrace(e));
		}
	}
}
