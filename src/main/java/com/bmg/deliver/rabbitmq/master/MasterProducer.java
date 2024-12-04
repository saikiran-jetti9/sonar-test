package com.bmg.deliver.rabbitmq.master;

import com.bmg.deliver.config.MasterCondition;
import java.nio.charset.StandardCharsets;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.AmqpTemplate;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Conditional(MasterCondition.class)
public class MasterProducer {
	@Value("${spring.rabbitmq.workQueue.exchange}")
	private String exchange;

	@Value("${spring.rabbitmq.workQueue.routingKey}")
	private String routingKey;

	private final AmqpTemplate rabbitTemplate;

	public MasterProducer(AmqpTemplate rabbitTemplate) {
		this.rabbitTemplate = rabbitTemplate;
	}

	public void sendMessage(String message) {

		log.info("Sending RabbitMQ message from Master: {}", message);
		MessageProperties properties = new MessageProperties();
		Message messageToSend = new Message(message.getBytes(StandardCharsets.UTF_8), properties);
		rabbitTemplate.convertAndSend(exchange, routingKey, messageToSend);

	}
}
