package com.bmg.deliver.exceptions;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

class ExceptionAdviceTest {

	private final ExceptionAdvice exceptionAdvice = new ExceptionAdvice();

	@Test
	void testHandleWorkflowStepNotFoundException() {
		WorkflowStepNotFoundException ex = new WorkflowStepNotFoundException("Workflow step not found");
		ResponseEntity<String> response = exceptionAdvice.handleWorkflowStepNotFoundException(ex);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals("Workflow step not found", response.getBody());
	}

	@Test
	void testHandleWorkflowNotFoundException() {
		WorkflowNotFoundException ex = new WorkflowNotFoundException("Workflow not found");
		ResponseEntity<String> response = exceptionAdvice.handleWorkflowNotFoundException(ex);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals("Workflow not found", response.getBody());
	}

	@Test
	void testHandleWorkflowInstancesNotFoundExceptionException() {
		WorkflowInstancesNotFoundException ex = new WorkflowInstancesNotFoundException("Workflow instances not found");
		ResponseEntity<String> response = exceptionAdvice.handleWorkflowInstancesNotFoundExceptionException(ex);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals("Workflow instances not found", response.getBody());
	}

	@Test
	void testHandleWorkflowInstanceIdNotFoundExceptionException() {
		WorkflowInstanceIdNotFoundException ex = new WorkflowInstanceIdNotFoundException(
				"Workflow instance ID not found");
		ResponseEntity<String> response = exceptionAdvice.handleWorkflowInstanceIdNotFoundExceptionException(ex);
		assertEquals(HttpStatus.NOT_FOUND, response.getStatusCode());
		assertEquals("Workflow instance ID not found", response.getBody());
	}
}
