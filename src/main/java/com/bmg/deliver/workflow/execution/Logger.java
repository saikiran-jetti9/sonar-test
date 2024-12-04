package com.bmg.deliver.workflow.execution;

import com.bmg.deliver.model.WorkflowInstance;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import com.bmg.deliver.utils.AppConstants;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.exception.ExceptionUtils;

@Slf4j
@Getter
@Setter
public class Logger {
	private final StringBuilder logs;
	private final DateTimeFormatter dateTimeFormatter;
	private final WorkflowInstance workflowInstance;
	private static final String LOG_MESSAGE_FORMAT = "[{}] {}";

	public Logger(WorkflowInstance workflowInstance) {
		this.workflowInstance = workflowInstance;
		logs = new StringBuilder();
		dateTimeFormatter = DateTimeFormatter.ofPattern(AppConstants.DATE_FORMAT_FULL_TIMESTAMP);
	}

	private void appendToLogs(String message) {
		String timestamp = LocalDateTime.now().format(dateTimeFormatter);
		String logEntry = String.format("[%s] %s%n", timestamp, message);
		logs.append(logEntry);
	}

	public void info(String message) {
		log.info(LOG_MESSAGE_FORMAT, workflowInstance.getId(), message);
		appendToLogs(message);
	}

	public void info(String var1, Object... var2) {
		String formattedMessage = String.format(var1, var2);
		log.info(LOG_MESSAGE_FORMAT, workflowInstance.getId(), formattedMessage);
		appendToLogs(formattedMessage);
	}

	public void error(String message) {
		log.error(LOG_MESSAGE_FORMAT, workflowInstance.getId(), message);
		appendToLogs(message);
	}

	public void error(String var1, Object... var2) {
		String formattedMessage = String.format(var1, var2);
		log.error(LOG_MESSAGE_FORMAT, workflowInstance.getId(), formattedMessage);
		appendToLogs(formattedMessage);
	}

	public void error(Exception e) {
		String stackTrace = ExceptionUtils.getStackTrace(e);
		log.error(LOG_MESSAGE_FORMAT, workflowInstance.getId(), stackTrace);
		appendToLogs(stackTrace);
	}

	public void error(String message, Exception e) {
		String stackTrace = ExceptionUtils.getStackTrace(e);
		log.error("[{}] {} {}", workflowInstance.getId(), message, stackTrace);
		appendToLogs(message + "\n" + stackTrace);
	}
}
