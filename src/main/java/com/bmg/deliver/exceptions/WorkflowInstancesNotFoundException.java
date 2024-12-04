package com.bmg.deliver.exceptions;

public class WorkflowInstancesNotFoundException extends RuntimeException {
	public WorkflowInstancesNotFoundException(String message) {
		super(message);
	}
}
