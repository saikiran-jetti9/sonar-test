package com.bmg.deliver.exceptions;

public class ExecutionOrderExistException extends RuntimeException {
	public ExecutionOrderExistException(String message) {
		super(message);
	}
}