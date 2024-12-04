package com.bmg.deliver.workflow.execution;

import com.bmg.deliver.model.WorkflowInstance;
import com.bmg.deliver.model.WorkflowInstanceArtifact;
import com.bmg.deliver.model.interfaces.TriggerData;
import com.bmg.deliver.model.product.Product;
import com.bmg.deliver.utils.AppConstants;
import com.bmg.deliver.workflow.step.Step;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import java.io.*;
import java.nio.file.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Slf4j
@Setter
@Getter
public class ExecutionContext {
	private final String attachmentsDir;
	private final WorkflowInstance workflowInstance;
	private Long currentStepId;
	private String resourcePath;
	private TriggerData triggerData;
	private TreeMap<String, String> filesToUpload = new TreeMap<>();

	private List<Step> steps = new ArrayList<>();
	private Map<Long, Set<WorkflowInstanceArtifact>> artifactMap = new ConcurrentHashMap<>();

	private Logger logger;

	public ExecutionContext(WorkflowInstance workflowInstance, String attachmentsDir) {
		this.attachmentsDir = attachmentsDir;
		this.logger = new Logger(workflowInstance);
		this.workflowInstance = workflowInstance;

		this.triggerData = addTriggerData(workflowInstance.getTriggerData());
	}

	public TriggerData addTriggerData(String triggerData) {
		Gson gson = new Gson();
		JsonObject data = gson.fromJson(triggerData, JsonObject.class);
		if (data.has(AppConstants.RELEASE_PRODUCT) && data.has(AppConstants.CAPTURE_PRODUCT)) {
			return gson.fromJson(data, Product.class);
		}
		return null;
	}

	public void addArtifacts(Object inputData, String fileName) {
		addArtifacts(inputData, fileName, null);
	}

	/**
	 * This method adds the artifact file from a specified step while creating
	 * workflow instance. This method accepts any type of data as input and write it
	 * as string to file and added to attachments directory.
	 *
	 * @param inputData
	 * @param fileName
	 */
	public void addArtifacts(Object inputData, String fileName, String remotePath) {
		try {

			String fileExtension = FilenameUtils.getExtension(fileName);
			UUID uuid = UUID.randomUUID();
			String uniqueFileName = String.format(AppConstants.ATTACHMENT_DIR_PATH, attachmentsDir, uuid,
					fileExtension);
			Path attachmentFilePath = Paths.get(uniqueFileName);

			WorkflowInstanceArtifact instanceArtifact = new WorkflowInstanceArtifact();
			instanceArtifact.setFilename(fileName);
			instanceArtifact.setUniqueFilename(attachmentFilePath.getFileName().toString());
			instanceArtifact.setCreated(new Date());

			Set<WorkflowInstanceArtifact> artifacts = artifactMap.computeIfAbsent(this.getCurrentStepId(),
					k -> new HashSet<>());
			artifacts.add(instanceArtifact);
			artifactMap.put(this.getCurrentStepId(), artifacts);
			Files.writeString(attachmentFilePath, String.valueOf(inputData));
			if (remotePath != null) {
				filesToUpload.put(remotePath, attachmentFilePath.toString());
			}

		} catch (Exception e) {
			log.error("Exception while adding artifacts for stepId {}: {}", this.getCurrentStepId(),
					ExceptionUtils.getStackTrace(e));
		}
	}
}
