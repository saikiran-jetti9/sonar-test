package com.bmg.deliver.rabbitmq.worker;

import com.bmg.deliver.config.WorkerCondition;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Conditional(WorkerCondition.class)
public class WorkerProducer {
	private final AmqpTemplate rabbitTemplate;

	@Value("${spring.rabbitmq.resultQueue.exchange}")
	private String exchange;

	@Value("${spring.rabbitmq.resultQueue.routingKey}")
	private String routingKey;

	public WorkerProducer(AmqpTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	public void sendMessage(String message) {
		try {
			log.info("Sending RabbitMQ message from Worker: {}", message);
			MessageProperties properties = new MessageProperties();
			Message messageToSend = new Message(message.getBytes(), properties);
			rabbitTemplate.convertAndSend(exchange, routingKey, messageToSend);
		} catch (Exception e) {
			log.error("Error sending RabbitMQ message from Worker: {}", ExceptionUtils.getStackTrace(e));
		}
	}
}
