package com.bmg.deliver.exceptions;

public class WorkflowInstanceIdNotFoundException extends RuntimeException {
	public WorkflowInstanceIdNotFoundException(String message) {
		super(message);
	}
}
