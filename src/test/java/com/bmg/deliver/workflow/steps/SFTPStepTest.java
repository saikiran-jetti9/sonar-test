package com.bmg.deliver.workflow.steps;

import static junit.framework.TestCase.assertEquals;
import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import com.bmg.deliver.enums.WorkflowStepType;
import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.model.WorkflowStepConfiguration;
import com.bmg.deliver.utils.AppConstants;
import com.bmg.deliver.workflow.execution.ExecutionContext;
import com.bmg.deliver.workflow.execution.Logger;
import com.bmg.deliver.workflow.step.StepParams;
import com.bmg.deliver.workflow.step.StepResult;
import com.jcraft.jsch.ChannelSftp;
import java.util.ArrayList;
import java.util.List;

import com.jcraft.jsch.JSchException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class SFTPStepTest {

	private SFTPStep sftpStep;

	@Mock
	private Workflow workflow;

	@Mock
	private Logger logger;

	@Mock
	private ExecutionContext context;

	@Mock
	private ChannelSftp channelSftp;

	private List<WorkflowStepConfiguration> stepConfigurations;

	@BeforeEach
	void setUp() {
		stepConfigurations = new ArrayList<>();

		WorkflowStepConfiguration config1 = Mockito.mock(WorkflowStepConfiguration.class);
		when(config1.getKey()).thenReturn(AppConstants.SFTP_USERNAME);
		when(config1.getValue()).thenReturn("testUsername");

		WorkflowStepConfiguration config2 = Mockito.mock(WorkflowStepConfiguration.class);
		when(config2.getKey()).thenReturn(AppConstants.SFTP_PASSWORD);
		when(config2.getValue()).thenReturn("testPassword");

		WorkflowStepConfiguration config3 = Mockito.mock(WorkflowStepConfiguration.class);
		when(config3.getKey()).thenReturn(AppConstants.SFTP_HOST);
		when(config3.getValue()).thenReturn("testHost");

		WorkflowStepConfiguration config4 = Mockito.mock(WorkflowStepConfiguration.class);
		when(config4.getKey()).thenReturn(AppConstants.SFTP_PORT);
		when(config4.getValue()).thenReturn("22");

		WorkflowStepConfiguration config5 = Mockito.mock(WorkflowStepConfiguration.class);
		when(config5.getKey()).thenReturn(AppConstants.SFTP_REMOTE_PATH);
		when(config5.getValue()).thenReturn("/test/remote/path");

		stepConfigurations.add(config1);
		stepConfigurations.add(config2);
		stepConfigurations.add(config3);
		stepConfigurations.add(config4);
		stepConfigurations.add(config5);
		StepParams params = new StepParams(1L, workflow, 1, "Test Step", WorkflowStepType.SFTP, stepConfigurations);
		sftpStep = new SFTPStep(params);
		ReflectionTestUtils.setField(sftpStep, "logger", logger);
		ReflectionTestUtils.setField(sftpStep, "context", context);
	}

	@Test
	void testRunMissingParameters() throws JSchException {
		sftpStep.setHost(null);
		sftpStep.setUsername(null);
		sftpStep.setPassword(null);
		sftpStep.setPort(0);
		StepResult result = sftpStep.run();
		assertFalse(result.isSuccess());
		assertEquals("SFTP connection parameters are missing.", result.getMessage());
	}

	@Test
	void testRunSuccessfulExecution() throws JSchException {
		sftpStep.setHost("testHost");
		sftpStep.setUsername("testUsername");
		sftpStep.setPassword("testPassword");
		sftpStep.setPort(22);
		sftpStep.setRemotePath("/remote/directory");
		StepResult result = sftpStep.run();
		assertTrue(result.isSuccess());
		assertEquals("SFTP successfully", result.getMessage());
	}
}
