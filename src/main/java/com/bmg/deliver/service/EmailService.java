package com.bmg.deliver.service;

import com.bmg.deliver.dto.WorkflowEmailDTO;
import com.bmg.deliver.model.WorkflowEmail;

import java.util.List;

public interface EmailService {
	void sendEmail(String to, String subject, String body);

	WorkflowEmail addEmail(Long id, WorkflowEmailDTO emailDTO);

	WorkflowEmail updateEmail(Long id, WorkflowEmailDTO emailDTO);

	WorkflowEmail getEmail(Long id);

	List<WorkflowEmailDTO> getEmailByWorkflowId(Long id);

	boolean deleteEmail(Long id);

	void emailNotifier(Long instanceId, Long workflowId);
}
