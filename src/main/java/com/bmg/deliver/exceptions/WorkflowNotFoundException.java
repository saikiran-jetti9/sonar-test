package com.bmg.deliver.exceptions;

public class WorkflowNotFoundException extends RuntimeException {
	public WorkflowNotFoundException(String message) {
		super(message);
	}
}
