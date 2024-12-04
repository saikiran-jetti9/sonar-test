package com.bmg.deliver.rabbitmq.worker;

import com.bmg.deliver.config.WorkerCondition;
import com.bmg.deliver.dto.WorkflowInstanceMessageDTO;
import com.bmg.deliver.workflow.execution.WorkflowInstanceProcessor;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Conditional(WorkerCondition.class)
public class WorkerListener {
	ObjectMapper objectMapper = new ObjectMapper();
	@Autowired
	WorkflowInstanceProcessor workflowInstanceProcessor;

	@RabbitListener(queues = "${spring.rabbitmq.workQueue.queue}", containerFactory = "prefetchOneRabbitListener", group = "worker", concurrency = "${spring.rabbitmq.workQueue.concurrency:'10-15'}")
	public void workflowInstanceConsumer(Message message) {
		String messageContent = null;
		try {
			byte[] body = message.getBody();
			messageContent = new String(body, StandardCharsets.UTF_8);
			log.info("Received RabbitMQ message WorkerConsumer: {}", messageContent);

			WorkflowInstanceMessageDTO workflowInstanceMessageDTO = objectMapper.readValue(messageContent,
					WorkflowInstanceMessageDTO.class);
			workflowInstanceProcessor.processor(workflowInstanceMessageDTO);
		} catch (Exception e) {
			log.error("Error while parsing MQ message {}: {}", messageContent, ExceptionUtils.getStackTrace(e));
		}
	}
}
