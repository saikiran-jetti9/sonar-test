package com.bmg.deliver.service;

import com.bmg.deliver.dto.WorkflowConfigurationDTO;
import com.bmg.deliver.dto.WorkflowDTO;
import com.bmg.deliver.dto.WorkflowInstanceDTO;
import com.bmg.deliver.dto.WorkflowStepDto;
import com.bmg.deliver.model.Workflow;
import com.bmg.deliver.model.WorkflowConfiguration;

import java.util.Date;
import java.util.List;

import com.bmg.deliver.model.WorkflowInstance;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface WorkflowService {
	Page<WorkflowDTO> getAllWorkflows(String search, Pageable pageable, Boolean enabled, Date startDate, Date endDate);

	Workflow getWorkflowById(Long id);

	List<WorkflowStepDto> getWorkflowSteps(Long id);

	Workflow createWorkflow(WorkflowDTO workflowDTO);

	Workflow updateWorkflow(Long id, WorkflowDTO workflowDTO);

	void deleteWorkflow(Long workflowId);

	Page<WorkflowDTO> getWorkflowsByWorkflowName(String identifier, Pageable pageable);

	WorkflowConfiguration createWorkFlowConfiguration(Long workflowId,
			WorkflowConfigurationDTO workflowConfigurationDTO);

	WorkflowConfiguration updateWorkFlowConfiguration(Long workflowId,
			WorkflowConfigurationDTO workflowConfigurationDTO);

	List<WorkflowConfigurationDTO> getWorkflowConfigurations(Long workflowId);

	Workflow getWorkflowByAlias(String alias);

	WorkflowInstance updateWorkflowInstance(Long id, WorkflowInstanceDTO workflowInstanceDTO);

}
