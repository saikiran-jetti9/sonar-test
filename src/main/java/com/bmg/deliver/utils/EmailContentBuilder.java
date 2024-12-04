package com.bmg.deliver.utils;

import com.bmg.deliver.model.WorkflowInstance;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.VelocityEngine;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.StringWriter;

@Component
public class EmailContentBuilder {
	private final VelocityEngine velocityEngine;

	@Value("${instance.url}")
	String instanceUrl;

	@Autowired
	public EmailContentBuilder(VelocityEngine velocityEngine) {
		this.velocityEngine = velocityEngine;
	}

	public String buildEmailContent(String recipientName, WorkflowInstance instance, String status) {
		VelocityContext context = new VelocityContext();
		JsonObject jsonData = new Gson().fromJson(instance.getTriggerData(), JsonObject.class);
		context.put(AppConstants.RECIPIENT_NAME, recipientName);
		context.put(AppConstants.BARCODE,
				jsonData.has(AppConstants.RELEASE_PRODUCT)
						? jsonData.get(AppConstants.RELEASE_PRODUCT).getAsJsonObject().get(AppConstants.BARCODE)
								.getAsString()
						: "");
		context.put(AppConstants.ARTIST,
				jsonData.has(AppConstants.RELEASE_PRODUCT)
						&& jsonData.get(AppConstants.RELEASE_PRODUCT).getAsJsonObject().has(AppConstants.ARTIST_NAME)
								? jsonData.get(AppConstants.RELEASE_PRODUCT).getAsJsonObject()
										.get(AppConstants.ARTIST_NAME).getAsString()
								: "");
		context.put(AppConstants.TITLE,
				jsonData.has(AppConstants.RELEASE_PRODUCT)
						? jsonData.get(AppConstants.RELEASE_PRODUCT).getAsJsonObject().get(AppConstants.TITLE)
								.getAsString()
						: "");
		context.put(AppConstants.FORMAT_NAME, jsonData.has(AppConstants.RELEASE_PRODUCT)
				&& jsonData.get(AppConstants.RELEASE_PRODUCT).getAsJsonObject().has(AppConstants.FORMAT)
				&& jsonData.get(AppConstants.RELEASE_PRODUCT).getAsJsonObject().get(AppConstants.FORMAT)
						.getAsJsonObject().has(AppConstants.NAME)
								? jsonData.get(AppConstants.RELEASE_PRODUCT).getAsJsonObject().get(AppConstants.FORMAT)
										.getAsJsonObject().get(AppConstants.NAME).getAsString()
								: "");
		context.put(AppConstants.RELEASE_DATE,
				jsonData.has(AppConstants.RELEASE_PRODUCT)
						&& jsonData.get(AppConstants.RELEASE_PRODUCT).getAsJsonObject().has(AppConstants.RELEASE_DATE)
								? jsonData.get(AppConstants.RELEASE_PRODUCT).getAsJsonObject()
										.get(AppConstants.RELEASE_DATE).getAsString()
								: "");
		context.put(AppConstants.STATUS, status.toLowerCase());
		context.put(AppConstants.PROCESS_NAME, instance.getWorkflow().getName());
		context.put(AppConstants.INSTANCE_ID, instance.getId());
		context.put(AppConstants.INSTANCE_URL,
				instanceUrl + "/workflows/" + instance.getWorkflow().getId() + "/workflowinstance/" + instance.getId());

		StringWriter writer = new StringWriter();
		velocityEngine.mergeTemplate("templates/email-template.vm", "UTF-8", context, writer);
		return writer.toString();
	}
}
