package com.bmg.deliver.exceptions;

public class EmailNotFoundException extends RuntimeException {

	public EmailNotFoundException(String message) {
		super(message);
	}
}
