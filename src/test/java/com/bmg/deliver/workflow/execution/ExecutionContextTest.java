package com.bmg.deliver.workflow.execution;

import static org.mockito.Mockito.*;

import com.bmg.deliver.model.WorkflowInstance;
import com.google.gson.JsonObject;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

class ExecutionContextTest {
	@InjectMocks
	private ExecutionContext executionContext;

	@Mock
	private WorkflowInstance workflowInstance;

	@Mock
	private JsonObject triggerData;

	@BeforeEach
	void setUp() {
		MockitoAnnotations.openMocks(this);
		when(workflowInstance.getId()).thenReturn(1L);
		when(workflowInstance.getTriggerData()).thenReturn("{}");

		executionContext = new ExecutionContext(workflowInstance, executionContext.getAttachmentsDir());
	}
}
