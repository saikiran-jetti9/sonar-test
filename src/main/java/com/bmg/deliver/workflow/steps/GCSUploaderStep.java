package com.bmg.deliver.workflow.steps;

import com.bmg.deliver.utils.AppConstants;
import com.bmg.deliver.workflow.step.Step;
import com.bmg.deliver.workflow.step.StepField;
import com.bmg.deliver.workflow.step.StepParams;
import com.bmg.deliver.workflow.step.StepResult;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.BlobId;
import com.google.cloud.storage.BlobInfo;
import com.google.cloud.storage.Storage;
import com.google.cloud.storage.StorageOptions;

import java.io.*;
import java.nio.ByteBuffer;
import java.util.Map;

import lombok.Getter;
import lombok.Setter;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Getter
@Setter
public class GCSUploaderStep extends Step {
	@StepField(key = AppConstants.GCS_BUCKET_NAME)
	private String bucketName;

	@StepField(key = AppConstants.GCS_SERVICE_ACCOUNT)
	private String serviceAccount;

	public GCSUploaderStep(StepParams params) {
		super(params.getId(), params.getWorkflow(), params.getExecutionOrder(), params.getName(), params.getType(),
				params.getStepConfigurations());
	}

	@Override
	public StepResult run() throws IOException {
		if (serviceAccount == null) {
			logger.error("Service account is not set");
			return new StepResult(false, "Service account is not set");
		}
		if (bucketName == null) {
			logger.error("Bucket name is not set");
			return new StepResult(false, "Bucket name is not set");
		}
		GoogleCredentials credentials = GoogleCredentials
				.fromStream(new ByteArrayInputStream(serviceAccount.getBytes()));
		Storage storage = StorageOptions.newBuilder().setCredentials(credentials).build().getService();

		long totalSize = 0;
		for (Map.Entry<String, String> fileToUpload : context.getFilesToUpload().entrySet()) {
			String remoteFilePath = fileToUpload.getKey();
			String localFilePath = fileToUpload.getValue();
			totalSize = totalSize + uploadFile(storage, localFilePath, remoteFilePath);
		}
		logger.info("Uploaded all files and Total size of files uploaded: %s", readableByteCount(totalSize));
		return new StepResult(true, "GCS uploader step");
	}

	private long uploadFile(Storage storage, String localFilePath, String remoteFilePath) {
		context.getLogger().info("Uploading %s to %s with size %s", localFilePath, remoteFilePath,
				readableByteCount(new File(localFilePath).length()));
		BlobId blobId = BlobId.of(bucketName, remoteFilePath);
		BlobInfo blobInfo = BlobInfo.newBuilder(blobId).build();
		File file = new File(localFilePath);

		try (InputStream inputStream = new FileInputStream(localFilePath)) {
			try (WriteChannel writeChannel = storage.writer(blobInfo)) {
				byte[] buffer = new byte[8 * 1024];
				int limit;
				while ((limit = inputStream.read(buffer)) >= 0) {
					ByteBuffer byteBuffer = ByteBuffer.wrap(buffer, 0, limit);
					writeChannel.write(byteBuffer);
				}
			}
			logger.info("Successfully uploaded: %s", localFilePath);
			return file.length();
		} catch (Exception e) {
			logger.error("Exception while uploading file: %s", ExceptionUtils.getStackTrace(e));
		}
		return 0;
	}

	private String readableByteCount(long bytes) {
		long maxSize = 1024L;
		if (bytes < maxSize) {
			return bytes + " B";
		}
		int exp = (int) (Math.log(bytes) / Math.log(1024));
		String pre = String.valueOf("KMGTPE".charAt(exp - 1));
		return String.format("%.1f %sB", bytes / Math.pow(1024, exp), pre);
	}
}
