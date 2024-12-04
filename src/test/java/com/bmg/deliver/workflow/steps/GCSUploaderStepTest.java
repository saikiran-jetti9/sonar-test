package com.bmg.deliver.workflow.steps;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bmg.deliver.enums.WorkflowStepType;
import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.model.WorkflowStepConfiguration;
import com.bmg.deliver.workflow.execution.ExecutionContext;
import com.bmg.deliver.workflow.execution.Logger;
import com.bmg.deliver.workflow.step.StepParams;
import com.bmg.deliver.workflow.step.StepResult;
import com.google.cloud.WriteChannel;
import com.google.cloud.storage.Storage;

import java.io.IOException;
import java.io.InputStream;
import java.util.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class GCSUploaderStepTest {
	private GCSUploaderStep gcsUploaderStep;
	@Mock
	private ExecutionContext context;
	@Mock
	private Workflow workflow;
	@Mock
	private Logger logger;
	@Mock
	private Storage storage;
	@Mock
	private InputStream inputStream;
	@Mock
	private WriteChannel writeChannel;

	@BeforeEach
	void setUp() throws IOException {
		List<WorkflowStepConfiguration> stepConfigurations = new ArrayList<>();
		StepParams stepParams = new StepParams(1L, workflow, 1, "Test Step", WorkflowStepType.GCS_UPLOADER,
				stepConfigurations);
		gcsUploaderStep = new GCSUploaderStep(stepParams);
		TreeMap<String, String> filesToUpload = new TreeMap<>();
		filesToUpload.put("remote/path/file.txt", "local/path/file.txt");

		// when(context.getFilesToUpload()).thenReturn(filesToUpload);

		ReflectionTestUtils.setField(gcsUploaderStep, "logger", logger);
		ReflectionTestUtils.setField(gcsUploaderStep, "context", context);
	}

	@Test
	void testRunServiceAccountNotSet() throws IOException {
		gcsUploaderStep.setServiceAccount(null);
		StepResult result = gcsUploaderStep.run();
		verify(logger).error("Service account is not set");
		assertFalse(result.isSuccess());
		assertEquals("Service account is not set", result.getMessage());
	}

	@Test
	void testRunBucketNameNotSet() throws IOException {
		gcsUploaderStep.setServiceAccount("servie aaccount");
		gcsUploaderStep.setBucketName(null);
		StepResult result = gcsUploaderStep.run();
		verify(logger).error("Bucket name is not set");
		assertFalse(result.isSuccess());
		assertEquals("Bucket name is not set", result.getMessage());
	}

	// @Test
	// void testRunBucketNameNotSeet() throws IOException {
	// gcsUploaderStep.setServiceAccount("servie aaccount");
	// gcsUploaderStep.setBucketName("bucket");
	// StepResult result = gcsUploaderStep.run();
	//
	// assertFalse(result.isSuccess());
	// assertEquals("Exception while uploading file", result.getMessage());
	// }
}
