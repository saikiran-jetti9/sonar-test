package com.bmg.deliver.exceptions;

public class WorkflowDisabledException extends RuntimeException {
	public WorkflowDisabledException(String message) {
		super(message);
	}
}
