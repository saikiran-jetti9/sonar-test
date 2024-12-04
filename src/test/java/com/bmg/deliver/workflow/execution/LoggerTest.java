package com.bmg.deliver.workflow.execution;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.bmg.deliver.model.WorkflowInstance;
import java.time.format.DateTimeFormatter;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoggerTest {

	@Mock
	private WorkflowInstance workflowInstance;
	private com.bmg.deliver.workflow.execution.Logger customLogger;
	private DateTimeFormatter dateTimeFormatter;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.initMocks(this);
		when(workflowInstance.getId()).thenReturn(1L);
		customLogger = new com.bmg.deliver.workflow.execution.Logger(workflowInstance);
		dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
	}

	@Test
	void testLoggerInitialization() {
		assertNotNull(customLogger.getDateTimeFormatter());
		assertNotNull(customLogger.getWorkflowInstance());
		assertEquals(1L, customLogger.getWorkflowInstance().getId());
		assertEquals(dateTimeFormatter.toString(), customLogger.getDateTimeFormatter().toString());
	}

	@Test
	void testInfoMessage() {
		String message = "This is an info message";
		customLogger.info(message);
		assertTrue(customLogger.getLogs().toString().contains(message));
	}

	@Test
	void testInfoMessageWithArgs() {
		String message = "This is an info message with args: %s";
		String arg = "arg1";
		customLogger.info(message, arg);
		assertTrue(customLogger.getLogs().toString().contains("This is an info message with args: arg1"));
	}

	@Test
	void testErrorMessage() {
		String message = "This is an error message";
		customLogger.error(message);
		assertTrue(customLogger.getLogs().toString().contains(message));
	}

	@Test
	void testErrorMessageWithArgs() {
		String message = "This is an error message with args: %s";
		String arg = "arg1";
		customLogger.error(message, arg);
		assertTrue(customLogger.getLogs().toString().contains("This is an error message with args: arg1"));
	}

	@Test
	void testErrorException() {
		Exception e = new RuntimeException("This is an exception");
		customLogger.error(e);
		assertTrue(customLogger.getLogs().toString().contains(ExceptionUtils.getStackTrace(e)));
	}

	@Test
	void testErrorMessageAndException() {
		String message = "This is an error message";
		Exception e = new RuntimeException("This is an exception");
		customLogger.error(message, e);
		assertTrue(customLogger.getLogs().toString().contains(message));
		assertTrue(customLogger.getLogs().toString().contains(ExceptionUtils.getStackTrace(e)));
	}
}
