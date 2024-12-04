package com.bmg.deliver.utils;

import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.model.WorkflowInstance;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.velocity.app.VelocityEngine;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
class EmailContentBuilderTest {
	@Mock
	VelocityEngine velocityEngine;

	@InjectMocks
	EmailContentBuilder contentBuilder;

	@Test
	void testBuildEmailContent() {
		Workflow workflow = new Workflow();
		workflow.setId(1L);
		workflow.setName("Test Workflow");

		String recepientName = "John";
		WorkflowInstance instance = new WorkflowInstance();
		instance.setId(1L);

		JsonObject jsonObject;
		try (FileReader reader = new FileReader("src/test/resources/ddexstep/product.json")) {
			jsonObject = new Gson().fromJson(reader, JsonObject.class);
		} catch (FileNotFoundException e) {
			throw new RuntimeException(e);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}

		instance.setTriggerData(jsonObject.toString());
		instance.setWorkflow(workflow);

		String status = "Success";
		String emailContent = contentBuilder.buildEmailContent(recepientName, instance, status);
		assertNotNull(emailContent);
	}
}