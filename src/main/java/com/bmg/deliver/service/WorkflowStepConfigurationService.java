package com.bmg.deliver.service;

import com.bmg.deliver.dto.WorkflowStepConfigurationDTO;
import com.bmg.deliver.model.WorkflowStepConfiguration;

import java.util.List;

public interface WorkflowStepConfigurationService {

	WorkflowStepConfiguration createWorkFlowStepConfiguration(Long stepId,
			WorkflowStepConfigurationDTO workflowStepConfigurationDTO);

	WorkflowStepConfiguration updateWorkflowStepConfiguration(Long stepId,
			WorkflowStepConfigurationDTO workflowStepConfigurationDTO);
	List<WorkflowStepConfiguration> getWorkflowStepConfigurationById(Long id);

	void deleteWorkflowStepConfigurationById(Long workflowStepConfigurationId);

	WorkflowStepConfiguration getWorkflowStepConfigurationByKey(String key);

	List<WorkflowStepConfigurationDTO> getWorkflowStepConfigurationDtoByWorkflowStepId(Long id);
}
