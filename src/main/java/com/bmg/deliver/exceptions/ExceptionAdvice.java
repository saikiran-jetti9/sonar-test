package com.bmg.deliver.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class ExceptionAdvice {

	@ExceptionHandler(WorkflowStepNotFoundException.class)
	public ResponseEntity<String> handleWorkflowStepNotFoundException(WorkflowStepNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
	}

	@ExceptionHandler(WorkflowNotFoundException.class)
	public ResponseEntity<String> handleWorkflowNotFoundException(WorkflowNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
	}

	@ExceptionHandler(WorkflowInstancesNotFoundException.class)
	public ResponseEntity<String> handleWorkflowInstancesNotFoundExceptionException(
			WorkflowInstancesNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
	}

	@ExceptionHandler(WorkflowInstanceIdNotFoundException.class)
	public ResponseEntity<String> handleWorkflowInstanceIdNotFoundExceptionException(
			WorkflowInstanceIdNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
	}

	@ExceptionHandler(WorkflowStepTemplateNotFoundException.class)
	public ResponseEntity<String> handleWorkflowNotFoundException(WorkflowStepTemplateNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
	}

	@ExceptionHandler(TemplateIdNotFoundException.class)
	public ResponseEntity<String> handleWorkflowNotFoundException(TemplateIdNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
	}

	@ExceptionHandler(WorkflowStepConfigurationsNotFound.class)
	public ResponseEntity<String> handleWorkflowStepConfigurationsNotFound(WorkflowStepConfigurationsNotFound ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
	}

	@ExceptionHandler(ExecutionOrderExistException.class)
	public ResponseEntity<String> handleExecutionOrderExistException(ExecutionOrderExistException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
	}

	@ExceptionHandler(WorkflowAliasNotFoundException.class)
	public ResponseEntity<String> handleWorkflowAliasNotFoundException(WorkflowAliasNotFoundException ex) {
		return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ex.getMessage());
	}

	@ExceptionHandler(WorkflowAliasExistsException.class)
	public ResponseEntity<String> handleWorkflowAliasExistsException(WorkflowAliasExistsException ex) {
		return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ex.getMessage());
	}
}
