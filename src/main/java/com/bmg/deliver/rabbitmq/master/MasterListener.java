package com.bmg.deliver.rabbitmq.master;

import com.bmg.deliver.config.MasterCondition;
import com.bmg.deliver.dto.WorkflowInstanceMessageDTO;
import com.bmg.deliver.serviceimpl.api.WorkflowServiceImpl;
import com.bmg.deliver.serviceimpl.master.MasterService;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;

import com.nimbusds.jose.shaded.gson.JsonObject;
import com.nimbusds.jose.shaded.gson.JsonParser;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.context.annotation.Conditional;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@Conditional(MasterCondition.class)
public class MasterListener {
	private final MasterService masterService;
	private final WorkflowServiceImpl workflowMasterService;
	private final ObjectMapper objectMapper = new ObjectMapper();

	public MasterListener(MasterService masterService, WorkflowServiceImpl workflowMasterService) {
		this.masterService = masterService;
		this.workflowMasterService = workflowMasterService;
	}

	@RabbitListener(queues = "${spring.rabbitmq.resultQueue.queue}", containerFactory = "prefetchOneRabbitListener", group = "master", concurrency = "${spring.rabbitmq.resultQueue.concurrency:'5-10'}")
	public void workflowInstanceResultConsumer(Message message) {
		String messageContent = null;
		try {
			byte[] body = message.getBody();
			messageContent = new String(body, StandardCharsets.UTF_8);
			WorkflowInstanceMessageDTO messageDTO = objectMapper.readValue(messageContent,
					WorkflowInstanceMessageDTO.class);
			log.info("Received RabbitMQ message in Master: {}", messageContent);

			masterService.processOnWorkResult(messageDTO);
		} catch (Exception e) {
			log.error("Error while parsing MQ message: {} {}", messageContent, ExceptionUtils.getStackTrace(e));
		}
	}

	@RabbitListener(queues = "${spring.rabbitmq.partnersQueue.queue}", containerFactory = "prefetchOneRabbitListener", group = "master", concurrency = "${spring.rabbitmq.partnersQueue.concurrency:'5-10'}")
	public void workflowInstanceFromPartnersConsumer(Message message) {
		String messageContent = null;
		try {
			byte[] body = message.getBody();
			messageContent = new String(body, StandardCharsets.UTF_8);
			JsonNode data = objectMapper.readTree(messageContent);
			String dataStr = objectMapper.writeValueAsString(data);
			JsonObject dataObj = JsonParser.parseString(dataStr).getAsJsonObject();

			Long workflowId = dataObj.has("workflowId") && !dataObj.get("workflowId").isJsonNull()
					? dataObj.get("workflowId").getAsLong()
					: null;
			JsonObject triggerData = dataObj.has("triggerData") && !dataObj.get("triggerData").isJsonNull()
					? dataObj.get("triggerData").getAsJsonObject()
					: null;

			workflowMasterService.createWorkFlowInstance(workflowId, triggerData);
		} catch (Exception e) {
			log.error("Error while parsing Partners message: {} {}", messageContent, ExceptionUtils.getStackTrace(e));
		}
	}
}
